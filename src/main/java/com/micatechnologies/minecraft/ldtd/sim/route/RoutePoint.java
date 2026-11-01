package com.micatechnologies.minecraft.ldtd.sim.route;

/** An immutable block position. Minecraft-free stand-in for {@code BlockPos}. */
public final class RoutePoint {

    public final int x;
    public final int y;
    public final int z;

    public RoutePoint(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoutePoint)) {
            return false;
        }
        RoutePoint p = (RoutePoint) o;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
