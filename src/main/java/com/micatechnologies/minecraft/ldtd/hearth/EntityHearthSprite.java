package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.entity.FlightMoveHelper;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.raid.RaidDirector;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.tower.BlockTowerCore;
import com.micatechnologies.minecraft.ldtd.tower.TileEntityTower;
import com.micatechnologies.minecraft.ldtd.tower.TowerKind;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A Hearth-sprite (design decision D13): a small ember-spirit born from its Hearth's fire, which
 * builds and mends the Hearth's works (Phase 6).
 *
 * <p>It flies, so it needs no ground pathfinding. It can't be hurt (only {@code /kill} removes
 * one), and it glows. Between jobs it drifts around its Hearth. During a raid it huddles in the
 * Hearth's fire, and whenever Gloam come within {@value Work#DANGER_RANGE} blocks of it too. A
 * sprite whose Hearth is gone, or no longer counts it, fades away.</p>
 */
public class EntityHearthSprite extends EntityCreature {

    @Nullable
    private BlockPos home;

    public EntityHearthSprite(World world) {
        super(world);
        setSize(0.4F, 0.5F);
        setNoGravity(true);
        moveHelper = new FlightMoveHelper(this, 0.06D);
        isImmuneToFire = true;
    }

    public void setHome(BlockPos hearth) {
        home = hearth.toImmutable();
        enablePersistence();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.4D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new Work(this));
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        // Only /kill and the void.
        return source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBrightnessForRender() {
        // An ember glows by its own light.
        return 15728880;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote && rand.nextInt(6) == 0) {
            world.spawnParticle(EnumParticleTypes.FLAME, posX, posY + 0.15D, posZ, 0.0D, 0.0D, 0.0D);
        }
    }

    @Nullable
    private TileEntityHearth hearth() {
        if (home == null || !world.isBlockLoaded(home)) {
            return null;
        }
        TileEntity te = world.getTileEntity(home);
        return te instanceof TileEntityHearth ? (TileEntityHearth) te : null;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        if (home != null) {
            nbt.setLong("ldtdHome", home.toLong());
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("ldtdHome")) {
            home = BlockPos.fromLong(nbt.getLong("ldtdHome"));
        }
    }

    /** Spawns a new sprite at {@code hearth}. Server side. */
    static void spawnAt(World world, TileEntityHearth hearth) {
        EntityHearthSprite sprite = new EntityHearthSprite(world);
        BlockPos p = hearth.getPos();
        sprite.setLocationAndAngles(p.getX() + 0.5D, p.getY() + 1.2D, p.getZ() + 0.5D, 0.0F, 0.0F);
        sprite.setHome(p);
        world.spawnEntity(sprite);
        hearth.addSprite(sprite.getUniqueID());
        ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, sprite.posX, sprite.posY, sprite.posZ, 12,
            0.2D, 0.2D, 0.2D, 0.02D);
    }

    /**
     * The sprite's whole life: fetch work from the Hearth (up to {@value #CARRY} blocks a trip),
     * fly to each spot and place it, come back. During raids, huddle at the Hearth.
     */
    private static final class Work extends EntityAIBase {

        private static final int CARRY = 4;
        private static final double FETCH_RANGE_SQ = 36.0D;
        /** How close Gloam must be to the Hearth for its sprites to stop work and huddle. */
        private static final double DANGER_RANGE = 24.0D;
        /** A sprite stuck this long on one block just goes there: it is a spirit. */
        private static final int STUCK_TICKS = 200;

        private final EntityHearthSprite sprite;
        private final List<BuildQueue.Step> carrying = new ArrayList<>();
        private int fetchCooldown;
        private int stepTicks;
        private int idleTicks;
        private boolean danger;

        Work(EntityHearthSprite sprite) {
            this.sprite = sprite;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return !sprite.world.isRemote;
        }

        @Override
        public void updateTask() {
            TileEntityHearth hearth = sprite.hearth();
            if (hearth == null) {
                if (sprite.home == null || sprite.world.isBlockLoaded(sprite.home)) {
                    // The Hearth is gone: the sprite fades.
                    sprite.setDead();
                }
                return;
            }
            if (!hearth.sprites().contains(sprite.getUniqueID())) {
                sprite.setDead();
                return;
            }
            BlockPos h = hearth.getPos();
            if (sprite.ticksExisted % 20 == 0) {
                // Any Gloam about, raid or not: stay in the fire rather than hand a Smother fresh
                // torches to snuff.
                danger = !sprite.world.getEntitiesWithinAABB(EntityGloam.class,
                    new AxisAlignedBB(h).grow(DANGER_RANGE)).isEmpty();
            }
            if (hearth.isDormant() || danger || RaidDirector.isRaided(sprite.world, h)) {
                sprite.getMoveHelper().setMoveTo(h.getX() + 0.5D, h.getY() + 1.3D, h.getZ() + 0.5D, 1.2D);
                return;
            }
            if (carrying.isEmpty()) {
                fetch(hearth, h);
            } else {
                deliver(hearth);
            }
        }

        private void fetch(TileEntityHearth hearth, BlockPos h) {
            double dx = sprite.posX - (h.getX() + 0.5D);
            double dy = sprite.posY - (h.getY() + 1.5D);
            double dz = sprite.posZ - (h.getZ() + 0.5D);
            // Sprites draw from the Hearth's fire, not from a door: anywhere close will do. That
            // matters once the Hearth has grown a hall around itself (D20) and a sprite flying
            // straight at it would only bump into the walls.
            boolean atHearth = dx * dx + dy * dy + dz * dz < FETCH_RANGE_SQ;
            if (--fetchCooldown <= 0) {
                fetchCooldown = 20;
                if (!hearth.queue().isEmpty()) {
                    if (atHearth) {
                        carrying.addAll(hearth.queue().take(sprite.world, hearth.stockpile(), CARRY, false));
                        hearth.markDirty();
                        stepTicks = 0;
                        return;
                    }
                    sprite.getMoveHelper().setMoveTo(h.getX() + 0.5D, h.getY() + 1.5D, h.getZ() + 0.5D, 1.0D);
                    return;
                }
            }
            if (--idleTicks <= 0) {
                idleTicks = 40 + sprite.rand.nextInt(40);
                sprite.getMoveHelper().setMoveTo(h.getX() + 0.5D + (sprite.rand.nextDouble() - 0.5D) * 6.0D,
                    h.getY() + 1.5D + sprite.rand.nextDouble() * 2.0D,
                    h.getZ() + 0.5D + (sprite.rand.nextDouble() - 0.5D) * 6.0D, 0.6D);
            }
        }

        private void deliver(TileEntityHearth hearth) {
            BuildQueue.Step step = carrying.get(0);
            BlockPos p = step.pos;
            double tx = p.getX() + 0.5D;
            double ty = p.getY() + 1.4D;
            double tz = p.getZ() + 0.5D;
            double d2 = sprite.getDistanceSq(tx, ty, tz);
            if (d2 > 2.25D && ++stepTicks < STUCK_TICKS) {
                sprite.getMoveHelper().setMoveTo(tx, ty, tz, 1.0D);
                return;
            }
            if (stepTicks >= STUCK_TICKS) {
                sprite.setPosition(tx, ty, tz);
            }
            stepTicks = 0;
            carrying.remove(0);
            World world = sprite.world;
            if (!world.getBlockState(p).getBlock().isReplaceable(world, p)) {
                // Someone built here meanwhile. Give the block back to the stockpile.
                if (step.state != null) {
                    refund(hearth, step.state);
                }
                return;
            }
            if (step.towerBlueprint != null) {
                Blueprint bp = LdtdData.blueprints().get(step.towerBlueprint);
                TowerKind kind = bp == null || bp.tower == null ? TowerKind.ARCHER : TowerKind.byId(bp.tower);
                world.setBlockState(p, LdtdRegistry.BLOCK_TOWER_CORE.getDefaultState()
                    .withProperty(BlockTowerCore.KIND, kind == null ? TowerKind.ARCHER : kind));
                TileEntity te = world.getTileEntity(p);
                if (te instanceof TileEntityTower) {
                    ((TileEntityTower) te).configure(step.towerBlueprint, step.anchor, step.rotation);
                }
            } else if (step.state != null) {
                world.setBlockState(p, step.state);
            } else {
                world.setBlockToAir(p);
            }
            sprite.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, tx, p.getY() + 1.0D, tz, 6, 0.3D, 0.1D,
                0.3D, 0.01D);
            world.playSound(null, p, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.5F, 1.4F);
        }

        private void refund(TileEntityHearth hearth, IBlockState state) {
            String name = String.valueOf(net.minecraft.block.Block.REGISTRY.getNameForObject(state.getBlock()));
            ItemStack back = com.micatechnologies.minecraft.ldtd.tower.BlockSpec.costOfBlock(
                name + "@" + state.getBlock().getMetaFromState(state));
            if (!back.isEmpty()) {
                ItemStack left = hearth.stockpile().addItem(back);
                if (!left.isEmpty()) {
                    sprite.entityDropItem(left, 0.2F);
                }
            }
        }
    }
}
