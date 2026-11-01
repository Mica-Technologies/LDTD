package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A cannon tower's shell. Lobbed in an arc, it bursts on the first block or Gloam it touches,
 * hurting every Gloam within the splash radius (less towards the edge). It breaks no blocks and
 * hurts nothing that isn't Gloam.
 */
public class EntityCannonShell extends EntityThrowable {

    private float damage = 8.0F;
    private float splash = 2.5F;

    public EntityCannonShell(World world) {
        super(world);
        setSize(0.35F, 0.35F);
    }

    public EntityCannonShell(World world, double x, double y, double z, float damage, float splash) {
        super(world, x, y, z);
        setSize(0.35F, 0.35F);
        this.damage = damage;
        this.splash = splash;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.05F;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        } else if (ticksExisted > 120) {
            setDead();
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote) {
            return;
        }
        if (result.typeOfHit == RayTraceResult.Type.ENTITY && !(result.entityHit instanceof EntityGloam)) {
            // Passes over defenders and anything else that isn't Gloam.
            return;
        }
        for (EntityGloam g : world.getEntitiesWithinAABB(EntityGloam.class, getEntityBoundingBox().grow(splash))) {
            double d = g.getDistance(this);
            if (d <= splash && g.isEntityAlive()) {
                g.attackEntityFrom(TileEntityTower.TOWER, (float) (damage * (1.0D - 0.5D * d / splash)));
            }
        }
        ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY, posZ, 1, 0.0D, 0.0D,
            0.0D, 0.0D);
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.8F, 1.2F);
        setDead();
    }
}
