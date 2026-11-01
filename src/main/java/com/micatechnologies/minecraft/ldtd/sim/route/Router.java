package com.micatechnologies.minecraft.ldtd.sim.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import javax.annotation.Nullable;

/**
 * Plans one shared route from a raid spawn point to a Hearth (design decision D10). Minecraft-free.
 *
 * <p>A* over marching states {@code (x, z, y)}. From a state, the march can step to each of the 8
 * neighbouring columns:</p>
 * <ul>
 *   <li><b>Walk</b> onto the neighbour's surface, if it is at most {@value #MAX_STEP_UP} block up
 *       and at most {@code maxDrop} blocks down. Costs 1 (or &radic;2 diagonally).</li>
 *   <li><b>Tunnel</b> straight into the neighbour at the current height, when its surface is too
 *       high to climb (a wall). Costs 1 plus the cost of breaking the two body-height blocks in the
 *       way. Orthogonal only, and only with a floor to stand on.</li>
 * </ul>
 * <p>Because breaking is priced in the same units as walking, the cheapest route is whichever is
 * cheaper: walking around the defenses, or cutting through the weakest wall. Placement of walls is
 * therefore never refused for blocking the path. The defenses just get sieged.</p>
 */
public final class Router {

    /** The highest step a marching mob takes without breaking anything. */
    public static final int MAX_STEP_UP = 1;

    private static final double DIAGONAL = Math.sqrt(2.0D);
    private static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONALS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final RouteTerrain terrain;
    private final int maxDrop;
    private final int nodeBudget;

    /**
     * @param maxDrop    the furthest a marching mob will drop down in one step
     * @param nodeBudget the most states to expand before giving up, so a hopeless search on a huge
     *                   area can't stall the server tick
     */
    public Router(RouteTerrain terrain, int maxDrop, int nodeBudget) {
        this.terrain = terrain;
        this.maxDrop = maxDrop;
        this.nodeBudget = nodeBudget;
    }

    /**
     * The cheapest route from {@code start} (a feet position) to any column within
     * {@code goalRadius} blocks of {@code (goalX, goalZ)}, or null if there is none within the
     * node budget.
     */
    @Nullable
    public Route find(RoutePoint start, int goalX, int goalZ, int goalRadius) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Long, Node> best = new HashMap<>();
        Node first = new Node(start.x, start.y, start.z, 0.0D, heuristic(start.x, start.z, goalX, goalZ, goalRadius),
            null, false);
        open.add(first);
        best.put(key(start.x, start.y, start.z), first);
        long goalRadiusSq = (long) goalRadius * goalRadius;
        int expanded = 0;

        while (!open.isEmpty()) {
            Node n = open.poll();
            if (n.closed) {
                continue;
            }
            n.closed = true;
            long dx = (long) n.x - goalX;
            long dz = (long) n.z - goalZ;
            if (dx * dx + dz * dz <= goalRadiusSq) {
                return build(n);
            }
            if (++expanded > nodeBudget) {
                return null;
            }

            for (int[] d : ORTHOGONAL) {
                expand(n, n.x + d[0], n.z + d[1], 1.0D, true, open, best, goalX, goalZ, goalRadius);
            }
            for (int[] d : DIAGONALS) {
                // No cutting corners: both orthogonal columns beside the diagonal must be walkable,
                // or the march would slip through the gap between two wall blocks.
                if (walkableY(n.y, n.x + d[0], n.z) != RouteTerrain.NO_GROUND
                    && walkableY(n.y, n.x, n.z + d[1]) != RouteTerrain.NO_GROUND) {
                    expand(n, n.x + d[0], n.z + d[1], DIAGONAL, false, open, best, goalX, goalZ, goalRadius);
                }
            }
        }
        return null;
    }

    private void expand(Node from, int nx, int nz, double stepCost, boolean mayTunnel,
                        PriorityQueue<Node> open, Map<Long, Node> best, int goalX, int goalZ, int goalRadius) {
        if (!terrain.inBounds(nx, nz)) {
            return;
        }
        int ny = walkableY(from.y, nx, nz);
        double cost;
        boolean breach = false;
        if (ny != RouteTerrain.NO_GROUND) {
            cost = stepCost;
        } else if (mayTunnel && tooHighToClimb(from.y, nx, nz) && terrain.isSolid(nx, from.y - 1, nz)) {
            double dig = terrain.breakCost(nx, from.y, nz) + terrain.breakCost(nx, from.y + 1, nz);
            if (Double.isInfinite(dig) || Double.isNaN(dig)) {
                return;
            }
            ny = from.y;
            cost = stepCost + dig;
            breach = dig > 0.0D;
        } else {
            return;
        }

        double g = from.g + cost;
        long k = key(nx, ny, nz);
        Node existing = best.get(k);
        if (existing != null && existing.g <= g) {
            return;
        }
        if (existing != null) {
            // Lazy deletion: the stale entry is skipped when it is polled.
            existing.closed = true;
        }
        Node next = new Node(nx, ny, nz, g, g + heuristic(nx, nz, goalX, goalZ, goalRadius), from, breach);
        best.put(k, next);
        open.add(next);
    }

    /**
     * The feet height after walking from height {@code y} onto column {@code (x, z)}, or
     * {@link RouteTerrain#NO_GROUND} if it can't be walked onto.
     */
    private int walkableY(int y, int x, int z) {
        // The terrain is only asked about columns inside its bounds.
        if (!terrain.inBounds(x, z)) {
            return RouteTerrain.NO_GROUND;
        }
        int s = terrain.surfaceY(x, z);
        if (s == RouteTerrain.NO_GROUND) {
            return RouteTerrain.NO_GROUND;
        }
        if (s - y > MAX_STEP_UP || y - s > maxDrop) {
            return RouteTerrain.NO_GROUND;
        }
        return s;
    }

    private boolean tooHighToClimb(int y, int x, int z) {
        int s = terrain.surfaceY(x, z);
        return s != RouteTerrain.NO_GROUND && s - y > MAX_STEP_UP;
    }

    /**
     * Straight-line distance to the edge of the goal circle. Never more than the true remaining
     * cost (every step costs at least its length), so A* stays optimal.
     */
    private static double heuristic(int x, int z, int goalX, int goalZ, int goalRadius) {
        double dx = x - goalX;
        double dz = z - goalZ;
        return Math.max(0.0D, Math.sqrt(dx * dx + dz * dz) - goalRadius);
    }

    private Route build(Node end) {
        List<RoutePoint> points = new ArrayList<>();
        List<RoutePoint> breaches = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent) {
            points.add(new RoutePoint(n.x, n.y, n.z));
            if (n.viaBreach) {
                // Added head-then-feet here, so after the final reversal each tunnel step lists its
                // feet block, then its head block.
                if (terrain.breakCost(n.x, n.y + 1, n.z) > 0.0D) {
                    breaches.add(new RoutePoint(n.x, n.y + 1, n.z));
                }
                if (terrain.breakCost(n.x, n.y, n.z) > 0.0D) {
                    breaches.add(new RoutePoint(n.x, n.y, n.z));
                }
            }
        }
        Collections.reverse(points);
        Collections.reverse(breaches);
        return new Route(points, breaches, end.g);
    }

    /** Packs a position into a map key. x and z keep 26 bits each, enough for the whole world. */
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static final class Node implements Comparable<Node> {
        final int x;
        final int y;
        final int z;
        final double g;
        final double f;
        final Node parent;
        final boolean viaBreach;
        boolean closed;

        Node(int x, int y, int z, double g, double f, Node parent, boolean viaBreach) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.g = g;
            this.f = f;
            this.parent = parent;
            this.viaBreach = viaBreach;
        }

        @Override
        public int compareTo(Node o) {
            int c = Double.compare(f, o.f);
            // On a tie, prefer the node further along (higher g): it reaches the goal sooner.
            return c != 0 ? c : Double.compare(o.g, g);
        }
    }
}
