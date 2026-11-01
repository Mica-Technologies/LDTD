package com.micatechnologies.minecraft.ldtd.gloam;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The Eclipse (Phase 9): the Gloam's great one, called out when a Hearth's region has torn down
 * enough Dusk Spires. It joins every raid on that Hearth until it is destroyed.
 *
 * <ul>
 *   <li>While it lives, the light weakness (design decision D15) doesn't touch any Gloam marching on
 *       its Hearth: it dims the whole territory.</li>
 *   <li>It is never weakened by light itself, and doesn't fade in daylight.</li>
 *   <li>Every 15 seconds it calls two Shades to its side, up to six at a time.</li>
 * </ul>
 * <p>Its hitbox fits the same 1x2 tunnels as every Gloam (it renders twice its size), so it can
 * follow a breached route like the rest.</p>
 */
public class EntityEclipse extends EntityGloam {

    /** Hearths with a living Eclipse marching on them, to the world time it last said so. */
    private static final Map<BlockPos, Long> DIMMED = new HashMap<>();

    private static final int SUMMON_EVERY = 300;
    private static final int SUMMON_CAP = 6;

    private final BossInfoServer bar = new BossInfoServer(new TextComponentTranslation("entity.ldtd.gloam_eclipse.name"),
        BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);

    public EntityEclipse(World world) {
        super(world);
        setSize(0.9F, 1.95F);
        isImmuneToFire = true;
        experienceValue = 100;
    }

    @Override
    public GloamKind kind() {
        return GloamKind.ECLIPSE;
    }

    /** Whether a living Eclipse dims the territory of the Hearth at {@code hearth}. */
    static boolean dims(World world, BlockPos hearth) {
        Long last = DIMMED.get(hearth);
        return last != null && world.getTotalWorldTime() - last < 60;
    }

    public static void clearAll() {
        DIMMED.clear();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    @Override
    public boolean isLit() {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DAYLIGHT) {
            return false;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            if (rand.nextInt(2) == 0) {
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX + (rand.nextDouble() - 0.5D) * 2.0D,
                    posY + rand.nextDouble() * 3.5D, posZ + (rand.nextDouble() - 0.5D) * 2.0D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        bar.setPercent(getHealth() / getMaxHealth());
        BlockPos target = marchTargetPos();
        if (target != null && ticksExisted % 20 == 0) {
            DIMMED.put(target, world.getTotalWorldTime());
        }
        if (target != null && ticksExisted % SUMMON_EVERY == 0) {
            summon(target);
        }
    }

    private void summon(BlockPos hearthPos) {
        HearthRecord hearth = marchHearth();
        if (hearth == null) {
            return;
        }
        int around = world.getEntitiesWithinAABB(EntityShade.class, getEntityBoundingBox().grow(16)).size();
        for (int i = 0; i < 2 && around + i < SUMMON_CAP; i++) {
            Entity e = EntityList.newEntity(EntityList.getClass(
                new ResourceLocation(LdtdConstants.MOD_NAMESPACE, GloamKind.SHADE.registryPath())), world);
            if (!(e instanceof EntityGloam)) {
                return;
            }
            e.setLocationAndAngles(posX + (rand.nextDouble() - 0.5D) * 3.0D, posY, posZ + (rand.nextDouble() - 0.5D) * 3.0D,
                rand.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(e);
            ((EntityGloam) e).marchOn(hearth);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, e.posX, e.posY + 1.0D, e.posZ, 15,
                0.3D, 0.6D, 0.3D, 0.01D);
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (world.isRemote) {
            return;
        }
        BlockPos target = marchTargetPos();
        if (target != null) {
            DIMMED.remove(target);
            if (world.isBlockLoaded(target)) {
                TileEntity te = world.getTileEntity(target);
                if (te instanceof TileEntityHearth) {
                    ((TileEntityHearth) te).setEclipseDefeated();
                }
            }
        }
        for (EntityPlayerMP p : world.getPlayers(EntityPlayerMP.class, p -> p.getDistanceSq(this) < 128 * 128)) {
            p.sendMessage(new TextComponentTranslation("ldtd.eclipse.defeated"));
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bar.removePlayer(player);
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }
}
