package com.micatechnologies.minecraft.ldtd.gloam;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The Flicker: the Gloam's runner. Fast and fragile, and every few seconds it blinks several steps
 * ahead along its route, though never past a wall that still stands (docs/GLOSSARY.md). Slowing
 * towers and bright light are the answer: a lit Flicker doesn't blink.
 */
public class EntityFlicker extends EntityGloam {

    private static final int BLINK_STEPS = 6;
    private int blinkCooldown = 60;

    public EntityFlicker(World world) {
        super(world);
        setSize(0.6F, 1.6F);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.FLICKER;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || --blinkCooldown > 0) {
            return;
        }
        blinkCooldown = 80 + rand.nextInt(60);
        if (isLit()) {
            return;
        }
        BlockPos from = march.blink(BLINK_STEPS);
        if (from != null) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, from.getX() + 0.5D, from.getY() + 1.0D,
                from.getZ() + 0.5D, 12, 0.25D, 0.5D, 0.25D, 0.01D);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + 1.0D, posZ, 12, 0.25D, 0.5D, 0.25D, 0.01D);
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE,
                0.6F, 0.5F);
        }
    }
}
