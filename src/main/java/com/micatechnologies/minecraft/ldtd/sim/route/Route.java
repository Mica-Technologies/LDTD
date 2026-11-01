package com.micatechnologies.minecraft.ldtd.sim.route;

import java.util.Collections;
import java.util.List;

/**
 * A planned march from a spawn point to a Hearth.
 *
 * <p>{@link #points} are the feet positions to walk through, in order, starting with the spawn.
 * {@link #breaches} are the blocks that have to be broken on the way, in the order they are met.
 * An empty breach list means the defenses can be walked around.</p>
 */
public final class Route {

    public final List<RoutePoint> points;
    public final List<RoutePoint> breaches;
    public final double cost;

    Route(List<RoutePoint> points, List<RoutePoint> breaches, double cost) {
        this.points = Collections.unmodifiableList(points);
        this.breaches = Collections.unmodifiableList(breaches);
        this.cost = cost;
    }

    public boolean needsBreach() {
        return !breaches.isEmpty();
    }

    public RoutePoint start() {
        return points.get(0);
    }

    public RoutePoint end() {
        return points.get(points.size() - 1);
    }
}
