package com.micatechnologies.minecraft.ldtd.route;

import com.micatechnologies.minecraft.ldtd.Ldtd;
import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.sim.route.Route;
import com.micatechnologies.minecraft.ldtd.sim.route.RoutePoint;
import com.micatechnologies.minecraft.ldtd.sim.route.Router;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Plans and caches raid routes (design decision D10).
 *
 * <p>Gloam spawned together ask for routes from nearly the same place, so routes are cached by the
 * target Hearth and the start position rounded to a 4-block cell. An entry lives for
 * {@value #TTL_TICKS} ticks, which bounds how stale a route can get as walls go up or come down.
 * A mob that gets stuck asks for a fresh route from where it stands.</p>
 *
 * <p>Server thread only.</p>
 */
public final class RouteService {

    /** How long a cached route is reused. */
    public static final int TTL_TICKS = 200;

    /** How close to the Hearth a route ends. */
    public static final int GOAL_RADIUS = 2;

    /** Extra room around the start and the territory that the search may use. */
    private static final int MARGIN = 16;

    /** The furthest a mob drops in one step without breaking anything. */
    private static final int MAX_DROP = 3;

    private static final Map<Integer, RouteService> BY_DIMENSION = new HashMap<>();

    private final Map<Key, Entry> cache = new HashMap<>();

    public static RouteService get(World world) {
        return BY_DIMENSION.computeIfAbsent(world.provider.getDimension(), d -> new RouteService());
    }

    /** Forget every cached route, everywhere. Called when the server stops. */
    public static void clearAll() {
        BY_DIMENSION.clear();
    }

    /**
     * A route from {@code start} (feet position) to {@code hearth}, from the cache when a fresh one
     * exists. Null if there is no way there, not even through the walls.
     */
    @Nullable
    public Route route(World world, BlockPos start, HearthRecord hearth, boolean fresh) {
        long now = world.getTotalWorldTime();
        Key key = new Key(hearth.pos, start.getX() >> 2, start.getY() >> 2, start.getZ() >> 2);
        if (!fresh) {
            Entry e = cache.get(key);
            if (e != null && now - e.created < TTL_TICKS) {
                return e.route;
            }
        }
        Route route = plan(world, start, hearth);
        cache.put(key, new Entry(route, now));
        prune(now);
        return route;
    }

    private Route plan(World world, BlockPos start, HearthRecord hearth) {
        int reach = hearth.radius() + MARGIN;
        int minX = Math.min(start.getX() - MARGIN, hearth.pos.getX() - reach);
        int maxX = Math.max(start.getX() + MARGIN, hearth.pos.getX() + reach);
        int minZ = Math.min(start.getZ() - MARGIN, hearth.pos.getZ() - reach);
        int maxZ = Math.max(start.getZ() + MARGIN, hearth.pos.getZ() + reach);
        WorldRouteTerrain terrain = new WorldRouteTerrain(world, minX, minZ, maxX, maxZ);
        long t0 = System.nanoTime();
        Route route = new Router(terrain, MAX_DROP, LdtdConfig.routeNodeBudget)
            .find(new RoutePoint(start.getX(), start.getY(), start.getZ()), hearth.pos.getX(), hearth.pos.getZ(),
                GOAL_RADIUS);
        long micros = (System.nanoTime() - t0) / 1000L;
        if (route == null) {
            Ldtd.LOGGER.debug("No route from {} to the Hearth at {} ({} us)", start, hearth.pos, micros);
        } else {
            Ldtd.LOGGER.debug("Route from {} to the Hearth at {}: {} steps, {} breach blocks, cost {} ({} us)",
                start, hearth.pos, route.points.size(), route.breaches.size(), route.cost, micros);
        }
        return route;
    }

    private void prune(long now) {
        Iterator<Entry> it = cache.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().created >= TTL_TICKS) {
                it.remove();
            }
        }
    }

    private static final class Entry {
        final Route route;
        final long created;

        Entry(Route route, long created) {
            this.route = route;
            this.created = created;
        }
    }

    private static final class Key {
        final BlockPos hearth;
        final int cx;
        final int cy;
        final int cz;

        Key(BlockPos hearth, int cx, int cy, int cz) {
            this.hearth = hearth;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) {
                return false;
            }
            Key k = (Key) o;
            return cx == k.cx && cy == k.cy && cz == k.cz && hearth.equals(k.hearth);
        }

        @Override
        public int hashCode() {
            return ((hearth.hashCode() * 31 + cx) * 31 + cy) * 31 + cz;
        }
    }
}
