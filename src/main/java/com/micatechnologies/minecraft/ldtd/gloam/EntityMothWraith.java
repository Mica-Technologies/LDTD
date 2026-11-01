package com.micatechnologies.minecraft.ldtd.gloam;

import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * The Moth-wraith: the Gloam's flyer (docs/GLOSSARY.md). It ignores the ground route and the
 * walls, and flies by light: it steers towards whatever is brightest nearby, which is usually the
 * Hearth. Lanterns set out on purpose can pull it into a kill zone instead (design decision D15).
 * Anti-air towers are the intended answer.
 */
public class EntityMothWraith extends EntityGloam {

    /** Height above its target it cruises at before diving. */
    private static final double CRUISE_HEIGHT = 5.0D;

    public EntityMothWraith(World world) {
        super(world);
        setSize(0.8F, 0.8F);
        setNoGravity(true);
        moveHelper = new FlightControl(this);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.MOTH_WRAITH;
    }

    @Override
    protected void initEntityAI() {
        // Flyers don't march: no route, no navigator, no digging.
        tasks.addTask(1, new Charge(this));
        tasks.addTask(2, new FollowLight(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new NearbyPlayerTarget(this));
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        // Flies: never takes fall damage.
    }

    @Override
    protected void updateFallState(double y, boolean onGround, net.minecraft.block.state.IBlockState state,
                                   BlockPos pos) {
    }

    /** Flies straight at its move target, the way a flyer must: no navigator, no ground. */
    private static final class FlightControl extends EntityMoveHelper {

        FlightControl(EntityMothWraith mob) {
            super(mob);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (action != Action.MOVE_TO) {
                return;
            }
            double dx = posX - entity.posX;
            double dy = posY - entity.posY;
            double dz = posZ - entity.posZ;
            double dist = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.4D) {
                action = Action.WAIT;
                entity.motionX *= 0.5D;
                entity.motionY *= 0.5D;
                entity.motionZ *= 0.5D;
                return;
            }
            // The movement attribute, so the light slowdown (D15) applies to flight too.
            double accel = speed * entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
                * 0.05D;
            entity.motionX += dx / dist * accel;
            entity.motionY += dy / dist * accel;
            entity.motionZ += dz / dist * accel;
            entity.rotationYaw = -((float) MathHelper.atan2(entity.motionX, entity.motionZ)) * (180.0F / (float) Math.PI);
            entity.renderYawOffset = entity.rotationYaw;
        }
    }

    /** Dives at its attack target and strikes on contact. */
    private static final class Charge extends EntityAIBase {

        private final EntityMothWraith mob;
        private int cooldown;

        Charge(EntityMothWraith mob) {
            this.mob = mob;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = mob.getAttackTarget();
            return target != null && target.isEntityAlive();
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = mob.getAttackTarget();
            if (target == null) {
                return;
            }
            mob.getMoveHelper().setMoveTo(target.posX, target.posY + target.getEyeHeight() * 0.6D, target.posZ, 1.4D);
            mob.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (--cooldown <= 0 && mob.getEntityBoundingBox().grow(0.3D).intersects(target.getEntityBoundingBox())) {
                cooldown = 20;
                mob.attackEntityAsMob(target);
            }
        }
    }

    /**
     * Steers up the light gradient: every second it samples block light a few blocks away in each
     * direction and heads for the brightest sample. Without anything brighter in reach, it heads for
     * the Hearth it was sent against, cruising above it. Cheap (a dozen light reads a second), and it
     * gives moth-to-a-flame behaviour without scanning for light blocks.
     */
    private static final class FollowLight extends EntityAIBase {

        private static final int PROBE = 4;
        private static final int[][] DIRS = {
            {PROBE, 0, 0}, {-PROBE, 0, 0}, {0, 0, PROBE}, {0, 0, -PROBE}, {0, PROBE, 0}, {0, -PROBE, 0},
            {PROBE, 0, PROBE}, {PROBE, 0, -PROBE}, {-PROBE, 0, PROBE}, {-PROBE, 0, -PROBE}};

        private final EntityMothWraith mob;

        FollowLight(EntityMothWraith mob) {
            this.mob = mob;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return mob.getAttackTarget() == null;
        }

        @Override
        public void updateTask() {
            if (mob.ticksExisted % 20 != 0 && mob.getMoveHelper().isUpdating()) {
                return;
            }
            BlockPos here = mob.getPosition();
            int best = light(here);
            BlockPos bestPos = null;
            for (int[] d : DIRS) {
                BlockPos p = here.add(d[0], d[1], d[2]);
                int l = light(p);
                if (l > best + 1) {
                    best = l;
                    bestPos = p;
                }
            }
            if (bestPos != null) {
                mob.getMoveHelper().setMoveTo(bestPos.getX() + 0.5D, bestPos.getY() + 0.5D, bestPos.getZ() + 0.5D, 1.0D);
                return;
            }
            if (best >= 12) {
                // At the flame: flutter around it.
                mob.getMoveHelper().setMoveTo(mob.posX + (mob.rand.nextDouble() - 0.5D) * 3.0D,
                    mob.posY + (mob.rand.nextDouble() - 0.5D) * 2.0D, mob.posZ + (mob.rand.nextDouble() - 0.5D) * 3.0D,
                    0.8D);
                return;
            }
            HearthRecord hearth = mob.marchHearth();
            if (hearth != null) {
                double dx = hearth.pos.getX() + 0.5D - mob.posX;
                double dz = hearth.pos.getZ() + 0.5D - mob.posZ;
                double y = hearth.pos.getY() + (dx * dx + dz * dz > 64.0D ? CRUISE_HEIGHT : 1.5D);
                mob.getMoveHelper().setMoveTo(hearth.pos.getX() + 0.5D, y, hearth.pos.getZ() + 0.5D, 1.0D);
            }
        }

        private int light(BlockPos p) {
            if (!mob.world.isBlockLoaded(p)) {
                return 0;
            }
            return mob.world.getLightFor(EnumSkyBlock.BLOCK, p);
        }
    }
}
