package com.micatechnologies.minecraft.ldtd.sim.route;

/**
 * What the router needs to know about the world. Minecraft-free: the game implements it over a
 * real world, and tests implement it over a character map.
 *
 * <p>The model is "2.5D with tunnels": a marching enemy normally walks on the top surface of each
 * block column, and when a wall is in the way it can tunnel through the wall at its current height,
 * at the cost of breaking the blocks.</p>
 */
public interface RouteTerrain {

    /** Returned by {@link #surfaceY} for a column with nowhere to stand (void, deep liquid). */
    int NO_GROUND = Integer.MIN_VALUE;

    /**
     * Whether the router may consider column {@code (x, z)} at all. Bounds the search. The router
     * never asks the other methods about a column outside the bounds.
     */
    boolean inBounds(int x, int z);

    /**
     * The feet height of a mob standing on top of column {@code (x, z)}: the y of the first
     * air block above the highest solid one, with room for a 2-block-tall body.
     * {@link #NO_GROUND} if nothing can stand there.
     */
    int surfaceY(int x, int z);

    /** Whether block {@code (x, y, z)} is solid enough to stand on. */
    boolean isSolid(int x, int y, int z);

    /**
     * The cost of breaking block {@code (x, y, z)} out of the way, in the same units as walking
     * (one block walked costs 1). {@code 0} for a block that is already passable (air, grass,
     * flowers). {@link Double#POSITIVE_INFINITY} for a block that may not be broken: unbreakable
     * blocks, and every block outside a Hearth territory (design decision D4).
     */
    double breakCost(int x, int y, int z);
}
