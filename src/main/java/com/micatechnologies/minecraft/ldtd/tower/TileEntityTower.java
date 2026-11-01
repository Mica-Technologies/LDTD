package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerStats;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

/**
 * A tower's brain. Server side only: finds a Gloam it can see, then shoots it as its kind does.
 *
 * <ul>
 *   <li><b>Archer</b>: one arrow at one target. Arrows pass through anything that isn't Gloam.</li>
 *   <li><b>Cannon</b>: a lobbed shell that bursts on impact, hurting every Gloam nearby.</li>
 *   <li><b>Frost</b>: chills every Gloam it can see in range: a little damage and a strong slow.</li>
 *   <li><b>Lantern</b>: doesn't shoot. Its core's full light weakens the Gloam around it.</li>
 * </ul>
 *
 * <p>A tower whose structure is mostly gone (under 75% of its blueprint's blocks) stops working
 * until it is repaired. Retargeting is staggered by position, so many towers don't all search the
 * same tick.</p>
 */
public class TileEntityTower extends TileEntity implements ITickable {

    /** Damage dealt by frost and cannon bursts. */
    public static final DamageSource TOWER = new DamageSource("ldtd_tower");

    /** Below this share of its blueprint's blocks, a tower stops working. */
    private static final double INTACT_SHARE = 0.75D;

    public enum Priority { FIRST, STRONGEST, CLOSEST }

    private String blueprint = "";
    @Nullable
    private BlockPos anchor;
    private int rotation;
    private Priority priority = Priority.FIRST;

    private int cooldown;
    private boolean intact = true;

    /** Called once when a blueprint builds this tower. */
    public void configure(String blueprintId, BlockPos anchorPos, int rot) {
        blueprint = blueprintId;
        anchor = anchorPos.toImmutable();
        rotation = rot;
        markDirty();
    }

    public TowerKind kind() {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockTowerCore ? state.getValue(BlockTowerCore.KIND) : TowerKind.ARCHER;
    }

    public boolean isIntact() {
        return intact;
    }

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }
        long t = world.getTotalWorldTime();
        int phase = (int) ((pos.toLong() & 0x7FFFFFFF) % 100);
        if ((t + phase) % 100 == 0) {
            intact = checkIntact();
        }
        if (--cooldown > 0 || !intact) {
            return;
        }
        TowerKind kind = kind();
        if (kind == TowerKind.LANTERN) {
            return;
        }
        // Look for targets every 5 ticks at most, staggered by position.
        if ((t + phase) % 5 != 0) {
            return;
        }
        TowerStats stats = LdtdData.tower(kind);
        List<EntityGloam> targets = targets(stats.range);
        if (targets.isEmpty()) {
            return;
        }
        cooldown = stats.cooldownTicks;
        switch (kind) {
            case ARCHER:
                shootArrow(targets.get(0), stats);
                break;
            case CANNON:
                shootShell(targets.get(0), stats);
                break;
            case FROST:
                chill(targets, stats);
                break;
            default:
                break;
        }
    }

    /** Where shots leave from: just above the core. */
    private Vec3d muzzle() {
        return new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D);
    }

    /** Every living Gloam in range that the tower can see, best target first. */
    private List<EntityGloam> targets(double range) {
        Vec3d from = muzzle();
        List<EntityGloam> out = new ArrayList<>();
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(range);
        for (EntityGloam g : world.getEntitiesWithinAABB(EntityGloam.class, box)) {
            if (!g.isEntityAlive() || g.getDistanceSq(from.x, from.y, from.z) > range * range) {
                continue;
            }
            Vec3d to = new Vec3d(g.posX, g.posY + g.getEyeHeight() * 0.7D, g.posZ);
            if (world.rayTraceBlocks(from, to, false, true, false) == null) {
                out.add(g);
            }
        }
        Comparator<EntityGloam> order;
        switch (priority) {
            case STRONGEST:
                order = Comparator.comparingDouble(g -> -g.getHealth());
                break;
            case CLOSEST:
                order = Comparator.comparingDouble(g -> g.getDistanceSq(pos));
                break;
            default:
                // Furthest along: nearest to the Hearth it is marching on.
                order = Comparator.comparingDouble(g -> {
                    BlockPos h = g.marchTargetPos();
                    return h == null ? Double.MAX_VALUE : g.getDistanceSq(h);
                });
        }
        out.sort(order);
        return out;
    }

    private void shootArrow(EntityGloam target, TowerStats stats) {
        Vec3d from = muzzle();
        EntityTowerArrow arrow = new EntityTowerArrow(world, from.x, from.y, from.z);
        double dist = target.getDistance(from.x, from.y, from.z);
        // Lead the target a little: arrows fly about 2.5 blocks a tick.
        double lead = dist / 2.5D;
        double dx = target.posX + target.motionX * lead - from.x;
        double dz = target.posZ + target.motionZ * lead - from.z;
        double dy = target.posY + target.height * 0.5D - from.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.12D, dz, 2.5F, 1.0F);
        // Vanilla arrow damage is speed x base damage, rounded up.
        arrow.setDamage(stats.damage / 2.5D);
        world.spawnEntity(arrow);
        world.playSound(null, pos, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 0.8F, 1.1F);
    }

    private void shootShell(EntityGloam target, TowerStats stats) {
        Vec3d from = muzzle();
        EntityCannonShell shell = new EntityCannonShell(world, from.x, from.y, from.z, (float) stats.damage,
            (float) stats.splashRadius);
        double dx = target.posX - from.x;
        double dz = target.posZ - from.z;
        double dy = target.posY - from.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        shell.shoot(dx, dy + horizontal * 0.35D, dz, 1.0F, 2.0F);
        world.spawnEntity(shell);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.4F, 1.6F);
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, from.x, from.y, from.z, 6, 0.1D, 0.1D,
            0.1D, 0.02D);
    }

    private void chill(List<EntityGloam> targets, TowerStats stats) {
        Vec3d from = muzzle();
        WorldServer ws = (WorldServer) world;
        for (EntityGloam g : targets) {
            g.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, stats.slowTicks, stats.slowAmplifier));
            if (stats.damage > 0.0D) {
                g.attackEntityFrom(TOWER, (float) stats.damage);
            }
            Vec3d to = new Vec3d(g.posX, g.posY + g.height * 0.5D, g.posZ);
            for (int i = 1; i <= 6; i++) {
                double f = i / 6.0D;
                ws.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, from.x + (to.x - from.x) * f,
                    from.y + (to.y - from.y) * f, from.z + (to.z - from.z) * f, 2, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.4F, 1.8F);
    }

    /** Whether enough of the tower's blueprint still stands for it to work. */
    private boolean checkIntact() {
        Blueprint bp = LdtdData.blueprints().get(blueprint);
        if (bp == null || anchor == null) {
            return true;
        }
        int expected = 0;
        int present = 0;
        for (Blueprint.Cell c : bp.cells(rotation)) {
            if (c.key == Blueprint.AIR || c.key == Blueprint.CORE) {
                continue;
            }
            IBlockState want = BlockSpec.state(bp.palette.get(String.valueOf(c.key)));
            if (want == null) {
                continue;
            }
            expected++;
            if (world.getBlockState(anchor.add(c.dx, c.dy, c.dz)).getBlock() == want.getBlock()) {
                present++;
            }
        }
        return expected == 0 || present >= expected * INTACT_SHARE;
    }

    void interact(EntityPlayer player) {
        if (player.isSneaking()) {
            priority = Priority.values()[(priority.ordinal() + 1) % Priority.values().length];
            markDirty();
            player.sendStatusMessage(new TextComponentTranslation("ldtd.tower.priority",
                new TextComponentTranslation("ldtd.tower.priority." + priority.name().toLowerCase())), true);
            return;
        }
        TowerKind kind = kind();
        TowerStats s = LdtdData.tower(kind);
        player.sendStatusMessage(new TextComponentTranslation(intact ? "ldtd.tower.info" : "ldtd.tower.info_broken",
            new TextComponentTranslation("ldtd.tower." + kind.id()), (int) s.range,
            new TextComponentTranslation("ldtd.tower.priority." + priority.name().toLowerCase())), true);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("blueprint", blueprint);
        if (anchor != null) {
            nbt.setLong("anchor", anchor.toLong());
        }
        nbt.setInteger("rotation", rotation);
        nbt.setString("priority", priority.name());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        blueprint = nbt.getString("blueprint");
        anchor = nbt.hasKey("anchor") ? BlockPos.fromLong(nbt.getLong("anchor")) : null;
        rotation = nbt.getInteger("rotation");
        try {
            priority = Priority.valueOf(nbt.getString("priority"));
        } catch (IllegalArgumentException e) {
            priority = Priority.FIRST;
        }
    }
}
