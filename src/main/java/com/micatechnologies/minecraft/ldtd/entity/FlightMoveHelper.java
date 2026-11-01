package com.micatechnologies.minecraft.ldtd.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.MathHelper;

/**
 * Flies an entity straight at its move target: no navigator, no ground. For the mod's flyers
 * (Moth-wraiths, Hearth-sprites), which have {@code setNoGravity(true)}.
 *
 * <p>Acceleration scales with the movement-speed attribute, so modifiers such as the Gloam's light
 * slowdown (design decision D15) apply to flight too.</p>
 */
public class FlightMoveHelper extends EntityMoveHelper {

    private final double accelPerSpeed;

    /** @param accelPerSpeed acceleration per tick, per point of the movement-speed attribute */
    public FlightMoveHelper(EntityLiving entity, double accelPerSpeed) {
        super(entity);
        this.accelPerSpeed = accelPerSpeed;
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
        double accel = speed * entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
            * accelPerSpeed;
        entity.motionX += dx / dist * accel;
        entity.motionY += dy / dist * accel;
        entity.motionZ += dz / dist * accel;
        entity.rotationYaw = -((float) MathHelper.atan2(entity.motionX, entity.motionZ)) * (180.0F / (float) Math.PI);
        entity.renderYawOffset = entity.rotationYaw;
    }
}
