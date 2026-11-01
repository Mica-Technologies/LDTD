package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * An archer tower's arrow. It flies through anything that isn't Gloam, so a tower never shoots its
 * own defenders, and it can't be picked up.
 */
public class EntityTowerArrow extends EntityTippedArrow {

    public EntityTowerArrow(World world) {
        super(world);
        pickupStatus = PickupStatus.DISALLOWED;
    }

    public EntityTowerArrow(World world, double x, double y, double z) {
        super(world, x, y, z);
        pickupStatus = PickupStatus.DISALLOWED;
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (hit.entityHit != null && !(hit.entityHit instanceof EntityGloam)) {
            return;
        }
        super.onHit(hit);
    }

    @Override
    protected void arrowHit(net.minecraft.entity.EntityLivingBase living) {
        super.arrowHit(living);
        // Tower arrows don't stick in the target.
        living.setArrowCountInEntity(Math.max(0, living.getArrowCountInEntity() - 1));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // Spent arrows don't litter the field.
        if (!world.isRemote && (inGround || ticksExisted > 100)) {
            setDead();
        }
    }
}
