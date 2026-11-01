package com.micatechnologies.minecraft.ldtd.sim.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RouterTest {

    /**
     * A flat test world drawn as characters, one per column, with x across and z down.
     * <ul>
     *   <li>{@code .} open ground (feet at y=1)</li>
     *   <li>{@code #} a weak 3-high wall (each block costs 2 to break)</li>
     *   <li>{@code W} a strong 3-high wall (each block costs 40)</li>
     *   <li>{@code X} an unbreakable 3-high wall</li>
     *   <li>{@code ^} a 1-high step (feet at y=2)</li>
     *   <li>space: void, nowhere to stand</li>
     *   <li>{@code S} start, {@code H} the Hearth (both open ground)</li>
     * </ul>
     */
    private static final class Grid implements RouteTerrain {
        final String[] rows;
        int sx;
        int sz;
        int hx;
        int hz;

        Grid(String... rows) {
            this.rows = rows;
            for (int z = 0; z < rows.length; z++) {
                for (int x = 0; x < rows[z].length(); x++) {
                    if (rows[z].charAt(x) == 'S') {
                        sx = x;
                        sz = z;
                    } else if (rows[z].charAt(x) == 'H') {
                        hx = x;
                        hz = z;
                    }
                }
            }
        }

        char at(int x, int z) {
            return rows[z].charAt(x);
        }

        boolean wall(char c) {
            return c == '#' || c == 'W' || c == 'X';
        }

        @Override
        public boolean inBounds(int x, int z) {
            return z >= 0 && z < rows.length && x >= 0 && x < rows[z].length();
        }

        @Override
        public int surfaceY(int x, int z) {
            char c = at(x, z);
            if (c == ' ') {
                return NO_GROUND;
            }
            if (wall(c)) {
                return 4;
            }
            return c == '^' ? 2 : 1;
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            char c = at(x, z);
            if (c == ' ') {
                return false;
            }
            if (y == 0) {
                return true;
            }
            return wall(c) && y >= 1 && y <= 3 || c == '^' && y == 1;
        }

        @Override
        public double breakCost(int x, int y, int z) {
            if (!isSolid(x, y, z)) {
                return 0.0D;
            }
            if (y <= 0) {
                return Double.POSITIVE_INFINITY;
            }
            switch (at(x, z)) {
                case '#':
                    return 2.0D;
                case 'W':
                    return 40.0D;
                case '^':
                    return 1.0D;
                default:
                    return Double.POSITIVE_INFINITY;
            }
        }

        Route route() {
            return new Router(this, 3, 100_000).find(new RoutePoint(sx, 1, sz), hx, hz, 1);
        }
    }

    @Test
    void openFieldNeedsNoBreach() {
        Grid g = new Grid(
            "S.........",
            "..........",
            ".........H");
        Route r = g.route();
        assertNotNull(r);
        assertFalse(r.needsBreach());
        assertEquals(new RoutePoint(0, 1, 0), r.start());
        // 9 across and 2 down: 2 diagonal steps plus 6 straight ones, stopping 1 short of the Hearth.
        assertTrue(r.cost < 9.0D, "cost " + r.cost);
    }

    @Test
    void walksThroughAGapRatherThanBreaking() {
        Grid g = new Grid(
            "S....#....",
            ".....#....",
            "..........",
            ".....#....",
            ".....#...H");
        Route r = g.route();
        assertNotNull(r);
        assertFalse(r.needsBreach());
    }

    @Test
    void sealedBaseIsBreachedAtTheWeakestWall() {
        Grid g = new Grid(
            "S..........",
            "...WWWWW...",
            "...W...W...",
            "...W.H.#...",
            "...W...W...",
            "...WWWWW...",
            "...........");
        Route r = g.route();
        assertNotNull(r);
        assertTrue(r.needsBreach());
        for (RoutePoint p : r.breaches) {
            assertEquals('#', g.at(p.x, p.z), "breached a strong wall at " + p);
        }
        // Two body-height blocks of one weak wall column.
        assertEquals(2, r.breaches.size());
    }

    @Test
    void unbreakableRingHasNoRoute() {
        Grid g = new Grid(
            "S......",
            ".XXXXX.",
            ".X...X.",
            ".X.H.X.",
            ".X...X.",
            ".XXXXX.",
            ".......");
        assertNull(g.route());
    }

    @Test
    void cheapWallIsBreachedInsteadOfALongDetour() {
        // Going round the weak gate means a detour of about 31 blocks: down the left edge, along
        // the bottom corridor and up the right edge. Straight through is 21 steps plus 4 to break
        // the gate (2 blocks at 2 each), so the march goes through it.
        Grid g = new Grid(
            "S.........#..........H",
            ".XXXXXXXXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXXXXXXXX.",
            "......................");
        Route r = g.route();
        assertNotNull(r);
        assertTrue(r.needsBreach());
        assertEquals(2, r.breaches.size());
    }

    @Test
    void longDetourBeatsAStrongWall() {
        Grid g = new Grid(
            "S.W...H",
            "..W....",
            "..W....",
            "..W....",
            ".......");
        Route r = g.route();
        assertNotNull(r);
        assertFalse(r.needsBreach(), "should walk round a strong wall, not through it");
    }

    @Test
    void doesNotSlipBetweenDiagonalWallBlocks() {
        Grid g = new Grid(
            "XXXXX",
            "XS.XX",
            "XX.XX",
            "XXX.X",
            "XXXHX",
            "XXXXX");
        // (2,2) to (3,3) is a diagonal squeeze between two X blocks: not allowed, and nothing else
        // leads there, so there is no route.
        assertNull(g.route());
    }

    @Test
    void stepsUpOneBlockButNotThree() {
        Grid g = new Grid("S.^^.H");
        Route r = g.route();
        assertNotNull(r);
        assertFalse(r.needsBreach());
        assertEquals(2, r.points.get(2).y);
    }

    @Test
    void voidIsNotWalkable() {
        Grid g = new Grid(
            "S.  .H",
            "..  ..");
        assertNull(g.route());
    }

    @Test
    void giveUpWhenOverBudget() {
        Grid g = new Grid(
            "S.........................................................H");
        assertNull(new Router(g, 3, 5).find(new RoutePoint(g.sx, 1, g.sz), g.hx, g.hz, 1));
    }
}
