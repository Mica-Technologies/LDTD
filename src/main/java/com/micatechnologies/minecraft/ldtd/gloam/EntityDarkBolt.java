package com.micatechnologies.minecraft.ldtd.gloam;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A darkness bolt, thrown by a Dimcaster. Flies straight (no gravity), hurts what it hits and
 * leaves it weakened for a few seconds. Passes harmlessly through other Gloam.
 */
public class EntityDarkBolt extends EntityThrowable {

    private float damage = 4.0F;

    public EntityDarkBolt(World world) {
        super(world);
        setSize(0.3F, 0.3F);
    }

    public EntityDarkBolt(World world, EntityLivingBase thrower, float damage) {
        super(world, thrower);
        setSize(0.3F, 0.3F);
        this.damage = damage;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        } else if (ticksExisted > 100) {
            setDead();
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote) {
            return;
        }
        if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (result.entityHit instanceof EntityGloam) {
                // Gloam don't hurt Gloam. Keep flying.
                return;
            }
            result.entityHit.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, getThrower()), damage);
            if (result.entityHit instanceof EntityLivingBase) {
                ((EntityLivingBase) result.entityHit).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 0));
            }
        }
        ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY, posZ, 16, 0.2D, 0.2D, 0.2D,
            0.05D);
        setDead();
    }
}
