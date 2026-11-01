package com.micatechnologies.minecraft.ldtd.gloam;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.route.SiegeDamage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * The Smother: the Gloam's siege unit. Slow, heavily built and armoured, and it hits walls about six
 * times as hard as a Shade (docs/GLOSSARY.md). As it passes, it snuffs out the small lights nearby,
 * such as torches and lanterns, so the next wave walks in darker ground (design decision D15).
 */
public class EntitySmother extends EntityGloam {

    /** How far a Smother reaches to snuff a light. */
    private static final int SNUFF_RANGE = 3;

    /** Only lights this fragile are snuffed outright. Sturdier ones must be sieged like walls. */
    private static final float SNUFF_MAX_HARDNESS = 0.5F;

    public EntitySmother(World world) {
        super(world);
        // The hitbox must fit the 1x2 tunnels the router plans, so it is smaller than the model,
        // which renders at 1.3x and looms over the gap it squeezes through.
        setSize(0.9F, 1.95F);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.SMOTHER;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!world.isRemote && ticksExisted % 40 == 0) {
            snuffNearbyLight();
        }
    }

    /** Puts out one fragile light source within reach, if there is one. */
    private void snuffNearbyLight() {
        BlockPos centre = getPosition();
        for (BlockPos pos : BlockPos.getAllInBoxMutable(centre.add(-SNUFF_RANGE, -1, -SNUFF_RANGE),
            centre.add(SNUFF_RANGE, 3, SNUFF_RANGE))) {
            IBlockState state = world.getBlockState(pos);
            if (state.getLightValue(world, pos) < 8 || state.getBlockHardness(world, pos) > SNUFF_MAX_HARDNESS) {
                continue;
            }
            BlockPos lamp = pos.toImmutable();
            // Snuffing is block damage like any other, so the territory rule (D4) applies.
            if (!SiegeDamage.maySnuff(world, lamp)) {
                continue;
            }
            swingArm(EnumHand.MAIN_HAND);
            getLookHelper().setLookPosition(lamp.getX() + 0.5D, lamp.getY() + 0.5D, lamp.getZ() + 0.5D, 30.0F, 30.0F);
            com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth.recordSiegeLoss(world, lamp, state);
            world.destroyBlock(lamp, LdtdConfig.siegeDropsBlocks);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, lamp.getX() + 0.5D,
                lamp.getY() + 0.6D, lamp.getZ() + 0.5D, 10, 0.15D, 0.15D, 0.15D, 0.01D);
            world.playSound(null, lamp, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 0.6F, 0.7F);
            return;
        }
    }
}
