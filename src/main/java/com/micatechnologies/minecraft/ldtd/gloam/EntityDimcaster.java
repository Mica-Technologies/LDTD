package com.micatechnologies.minecraft.ldtd.gloam;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.world.World;

/**
 * The Dimcaster: the Gloam's ranged unit (docs/GLOSSARY.md). It marches with the rest and throws
 * darkness bolts at anyone who comes within range. Defenders on walls are its natural prey, and
 * towers will be too once they exist (Phase 5).
 */
public class EntityDimcaster extends EntityGloam implements IRangedAttackMob {

    private static final float RANGE = 16.0F;

    public EntityDimcaster(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.DIMCASTER;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackRanged(this, 1.0D, 40, RANGE));
        march = new GloamMarch(this);
        tasks.addTask(3, march);
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new RangedPlayerTarget(this));
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        EntityDarkBolt bolt = new EntityDarkBolt(world, this, (float) stats().attack);
        double dx = target.posX - posX;
        double dy = target.posY + target.getEyeHeight() * 0.5D - bolt.posY;
        double dz = target.posZ - posZ;
        bolt.shoot(dx, dy, dz, 1.1F, 4.0F);
        swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        playSound(SoundEvents.ENTITY_ILLAGER_CAST_SPELL, 0.8F, 0.6F);
        world.spawnEntity(bolt);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
    }

    /** Like the melee units' targeting, but out to bolt range. */
    private static final class RangedPlayerTarget extends net.minecraft.entity.ai.EntityAINearestAttackableTarget<EntityPlayer> {

        RangedPlayerTarget(EntityDimcaster mob) {
            super(mob, EntityPlayer.class, true);
        }

        @Override
        protected double getTargetDistance() {
            return RANGE;
        }
    }
}
