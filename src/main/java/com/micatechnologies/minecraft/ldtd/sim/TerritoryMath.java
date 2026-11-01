package com.micatechnologies.minecraft.ldtd.sim;

/**
 * Geometry of Hearth territories. Minecraft-free on purpose, so it is unit-testable on a bare JVM.
 *
 * <p>A territory is a vertical column with a circular footprint: every block whose centre lies
 * within {@code radius} blocks (horizontally) of the Hearth's centre, at any height. A column
 * rather than a sphere keeps the rule easy for players to reason about: "inside the circle" means
 * inside, whether you are digging down or building up.</p>
 */
public final class TerritoryMath {

    private TerritoryMath() {
        throw new AssertionError("No instances.");
    }

    /**
     * Whether block {@code (x, z)} is inside the territory of a Hearth at {@code (cx, cz)}.
     * Distances are measured between block centres, so a Hearth's own block is always inside.
     */
    public static boolean contains(int cx, int cz, int radius, int x, int z) {
        if (radius < 0) {
            return false;
        }
        long dx = (long) x - cx;
        long dz = (long) z - cz;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    /** Whether two territories share at least one block column. */
    public static boolean overlaps(int ax, int az, int aRadius, int bx, int bz, int bRadius) {
        if (aRadius < 0 || bRadius < 0) {
            return false;
        }
        long dx = (long) ax - bx;
        long dz = (long) az - bz;
        long reach = (long) aRadius + bRadius;
        return dx * dx + dz * dz <= reach * reach;
    }

    /** Squared horizontal distance between two block columns, for "nearest Hearth" lookups. */
    public static long distanceSq(int ax, int az, int bx, int bz) {
        long dx = (long) ax - bx;
        long dz = (long) az - bz;
        return dx * dx + dz * dz;
    }
}
