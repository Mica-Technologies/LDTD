package com.micatechnologies.minecraft.ldtd.route;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.hearth.BlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.sim.route.RouteTerrain;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * {@link RouteTerrain} over a real world, for one route search.
 *
 * <p>Bounded to a rectangle and to loaded chunks: the router must never cause a chunk load. Surface
 * heights are cached for the life of the search, since A* asks about the same columns many times.</p>
 */
final class WorldRouteTerrain implements RouteTerrain {

    private final World world;
    private final HearthRegistry hearths;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final Map<Long, Integer> surface = new HashMap<>();
    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    WorldRouteTerrain(World world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world;
        this.hearths = HearthRegistry.get(world);
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    @Override
    public boolean inBounds(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && world.isBlockLoaded(scratch.setPos(x, 64, z));
    }

    @Override
    public int surfaceY(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        Integer cached = surface.get(key);
        if (cached != null) {
            return cached;
        }
        int y = computeSurface(x, z);
        surface.put(key, y);
        return y;
    }

    private int computeSurface(int x, int z) {
        // Top block that blocks movement, ignoring leaves and foliage: where a mob walking over
        // the land would put its feet.
        BlockPos top = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
        if (top.getY() <= 0) {
            return NO_GROUND;
        }
        if (world.getBlockState(top.down()).getMaterial().isLiquid()) {
            return NO_GROUND;
        }
        return top.getY();
    }

    @Override
    public boolean isSolid(int x, int y, int z) {
        return world.getBlockState(scratch.setPos(x, y, z)).getMaterial().blocksMovement();
    }

    @Override
    public double breakCost(int x, int y, int z) {
        scratch.setPos(x, y, z);
        IBlockState state = world.getBlockState(scratch);
        if (!state.getMaterial().blocksMovement()) {
            return 0.0D;
        }
        if (!SiegeRules.mayBreak(world, hearths, scratch, state)) {
            return Double.POSITIVE_INFINITY;
        }
        return 1.0D + state.getBlockHardness(world, scratch) * LdtdConfig.breachCostPerHardness;
    }

    /** The rules for which blocks the Gloam may break. Shared by routing and the damage tracker. */
    static final class SiegeRules {

        private SiegeRules() {
        }

        static boolean mayBreak(World world, HearthRegistry hearths, BlockPos pos, IBlockState state) {
            if (!LdtdConfig.gloamDamageBlocks) {
                return false;
            }
            if (state.getBlock() instanceof BlockHearth) {
                return false;
            }
            if (state.getBlockHardness(world, pos) < 0.0F) {
                return false;
            }
            // Design decision D4: only ever inside a Hearth's territory.
            return hearths.territoryAt(pos.getX(), pos.getZ()) != null;
        }
    }
}
