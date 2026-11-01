package com.micatechnologies.minecraft.ldtd.spire;

import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Raises a Dusk Spire (Phase 9): a 5x5 plinth of dusk stone, a 3x3 shaft rising six blocks with
 * the core sealed inside at its foot, and a needle on top that grows with the spire's tier.
 * Footprint is x and z within -2..+2 of the centre, so a spire placed at a chunk's middle never
 * spills into a neighbouring chunk during world generation.
 */
public final class SpireBuilder {

    private SpireBuilder() {
        throw new AssertionError("No instances.");
    }

    /**
     * Builds a spire standing on {@code ground} (the first air block above the surface).
     *
     * @return the core's position
     */
    public static BlockPos build(World world, BlockPos ground, int tier) {
        IBlockState stone = LdtdRegistry.BLOCK_DUSK_STONE.getDefaultState();
        // Plinth: a full 5x5 slab, sunk one block into the ground so it sits flush.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(ground.add(dx, -1, dz), stone, 2);
                world.setBlockState(ground.add(dx, 0, dz), stone, 2);
            }
        }
        // Shaft: 3x3, six high, solid. The core is sealed at its foot.
        for (int dy = 1; dy <= 6; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.setBlockState(ground.add(dx, dy, dz), stone, 2);
                }
            }
        }
        BlockPos core = ground.up();
        world.setBlockState(core, LdtdRegistry.BLOCK_SPIRE_CORE.getDefaultState(), 2);
        // Needle: taller for stronger spires.
        int needle = 2 + 3 * tier;
        for (int dy = 7; dy < 7 + needle; dy++) {
            world.setBlockState(ground.up(dy), stone, 2);
        }
        SpireRegistry.get(world).add(core, tier);
        return core;
    }

    /** Tier from distance to the world spawn: further out, stronger spires. */
    public static int tierAt(World world, BlockPos pos) {
        double d = Math.sqrt(world.getSpawnPoint().distanceSq(pos.getX(), world.getSpawnPoint().getY(), pos.getZ()));
        return d < 600.0D ? 1 : d < 1200.0D ? 2 : 3;
    }
}
