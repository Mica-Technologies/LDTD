package com.micatechnologies.minecraft.ldtd.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The validated, immutable Hearth level table. Minecraft-free.
 *
 * <p>Built through {@link #validate(List)}, which rejects a table a server owner has broken by
 * hand (gaps in the levels, a radius out of range, a territory that shrinks as the Hearth levels
 * up) with a message saying exactly what is wrong. The loader then keeps the last good table
 * rather than running with a broken one.</p>
 */
public final class HearthLevelTable {

    /** Smallest allowed territory radius. Below this, a territory is too small to defend. */
    public static final int MIN_RADIUS = 8;

    /** Largest allowed territory radius. Keeps routing grids and chunk tickets bounded. */
    public static final int MAX_RADIUS = 128;

    private final List<HearthLevel> levels;

    private HearthLevelTable(List<HearthLevel> levels) {
        this.levels = Collections.unmodifiableList(levels);
    }

    /**
     * Checks {@code raw} and builds a table from it.
     *
     * @throws IllegalArgumentException with a human-readable reason if the table is invalid
     */
    public static HearthLevelTable validate(List<HearthLevel> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("the table has no levels");
        }
        List<HearthLevel> sorted = new ArrayList<>();
        for (HearthLevel l : raw) {
            if (l == null) {
                throw new IllegalArgumentException("the table has an empty entry");
            }
            sorted.add(new HearthLevel(l.level, l.radius, l.maxSprites));
        }
        sorted.sort(Comparator.comparingInt(l -> l.level));
        int previousRadius = 0;
        for (int i = 0; i < sorted.size(); i++) {
            HearthLevel l = sorted.get(i);
            if (l.level != i + 1) {
                throw new IllegalArgumentException(
                    "levels must run 1, 2, 3, ... with no gaps or repeats, but found level " + l.level
                        + " where level " + (i + 1) + " was expected");
            }
            if (l.radius < MIN_RADIUS || l.radius > MAX_RADIUS) {
                throw new IllegalArgumentException("level " + l.level + " has radius " + l.radius
                    + ", outside " + MIN_RADIUS + ".." + MAX_RADIUS);
            }
            if (l.radius < previousRadius) {
                throw new IllegalArgumentException("level " + l.level + " has a smaller radius ("
                    + l.radius + ") than the level before it (" + previousRadius + ")");
            }
            if (l.maxSprites < 0) {
                throw new IllegalArgumentException("level " + l.level + " has a negative maxSprites");
            }
            previousRadius = l.radius;
        }
        return new HearthLevelTable(sorted);
    }

    /** The highest level in the table. */
    public int maxLevel() {
        return levels.size();
    }

    /** The row for {@code level}, clamped into the table, so a stale saved level never crashes. */
    public HearthLevel get(int level) {
        int clamped = Math.max(1, Math.min(level, levels.size()));
        return levels.get(clamped - 1);
    }

    /** The largest radius any level reaches. */
    public int maxRadius() {
        return levels.get(levels.size() - 1).radius;
    }

    public List<HearthLevel> levels() {
        return levels;
    }
}
