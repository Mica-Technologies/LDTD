package com.micatechnologies.minecraft.ldtd.sim.tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A structure Hearth-sprites (or, until Phase 6, the blueprint itself) can build: towers, walls.
 * Loaded from {@code config/ldtd/blueprints.json}. Minecraft-free.
 *
 * <p>The shape is a stack of layers, bottom first. Each layer is a list of rows (z, front to back),
 * each row a string of cells (x, left to right). A cell is a palette character, {@code '.'} for
 * "must be air", or a space for "leave whatever is there". {@code 'C'} is the tower core, the block
 * that does the tower's work. Exactly one is required when {@link #tower} is set.</p>
 *
 * <p>Placement anchors the structure's bottom layer on the clicked spot, centred. Rotation turns it
 * in 90 degree steps about that anchor, so the preview and the build always agree.</p>
 */
public final class Blueprint {

    public static final char AIR = '.';
    public static final char ANY = ' ';
    public static final char CORE = 'C';

    /** The tower kind this builds (a key in towers.json), or null for a plain structure like a wall. */
    public String tower;

    /** Palette character to block, as {@code modid:name} or {@code modid:name@meta}. */
    public Map<String, String> palette = new LinkedHashMap<>();

    /** Bottom layer first. */
    public List<List<String>> layers = new ArrayList<>();

    /** Extra items the build costs on top of its blocks, as {@code modid:name[@meta]} to count. */
    public Map<String, Integer> extraCost = new LinkedHashMap<>();

    private transient int width;
    private transient int depth;

    /** One cell of a placed structure, relative to the anchor. */
    public static final class Cell {
        public final int dx;
        public final int dy;
        public final int dz;
        public final char key;

        Cell(int dx, int dy, int dz, char key) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.key = key;
        }

        @Override
        public String toString() {
            return key + "@" + dx + "," + dy + "," + dz;
        }
    }

    /**
     * Checks the shape and palette.
     *
     * @param towerIds the known tower kinds
     * @throws IllegalArgumentException with a human-readable reason
     */
    public Blueprint validate(String id, Iterable<String> towerIds) {
        if (layers == null || layers.isEmpty()) {
            throw new IllegalArgumentException(id + ": no layers");
        }
        if (layers.size() > 16) {
            throw new IllegalArgumentException(id + ": more than 16 layers");
        }
        depth = layers.get(0).size();
        width = depth == 0 ? 0 : layers.get(0).get(0).length();
        if (width < 1 || depth < 1 || width > 9 || depth > 9) {
            throw new IllegalArgumentException(id + ": footprint must be 1-9 by 1-9");
        }
        int cores = 0;
        for (int y = 0; y < layers.size(); y++) {
            List<String> layer = layers.get(y);
            if (layer.size() != depth) {
                throw new IllegalArgumentException(id + ": layer " + y + " has " + layer.size() + " rows, expected " + depth);
            }
            for (String row : layer) {
                if (row.length() != width) {
                    throw new IllegalArgumentException(id + ": layer " + y + " has a row of " + row.length()
                        + " cells, expected " + width);
                }
                for (char c : row.toCharArray()) {
                    if (c == CORE) {
                        cores++;
                    } else if (c != AIR && c != ANY && (palette == null || !palette.containsKey(String.valueOf(c)))) {
                        throw new IllegalArgumentException(id + ": cell '" + c + "' is not in the palette");
                    }
                }
            }
        }
        if (tower != null) {
            boolean known = false;
            for (String t : towerIds) {
                known |= t.equals(tower);
            }
            if (!known) {
                throw new IllegalArgumentException(id + ": unknown tower '" + tower + "'");
            }
            if (cores != 1) {
                throw new IllegalArgumentException(id + ": a tower needs exactly one 'C' core cell, found " + cores);
            }
        } else if (cores != 0) {
            throw new IllegalArgumentException(id + ": only towers may have a 'C' core cell");
        }
        if (palette == null) {
            palette = new LinkedHashMap<>();
        }
        if (extraCost == null) {
            extraCost = new LinkedHashMap<>();
        }
        return this;
    }

    public int width() {
        return width;
    }

    public int depth() {
        return depth;
    }

    public int height() {
        return layers.size();
    }

    /**
     * Every cell that matters (not {@link #ANY}), relative to the anchor, after turning the
     * structure {@code rotation} quarter-turns clockwise (seen from above). 0 = as written.
     */
    public List<Cell> cells(int rotation) {
        List<Cell> out = new ArrayList<>();
        int r = ((rotation % 4) + 4) % 4;
        for (int y = 0; y < layers.size(); y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < depth; z++) {
                String row = layer.get(z);
                for (int x = 0; x < width; x++) {
                    char c = row.charAt(x);
                    if (c == ANY) {
                        continue;
                    }
                    // Centre on the anchor, then rotate.
                    int cx = x - width / 2;
                    int cz = z - depth / 2;
                    int rx;
                    int rz;
                    switch (r) {
                        case 1:
                            rx = -cz;
                            rz = cx;
                            break;
                        case 2:
                            rx = -cx;
                            rz = -cz;
                            break;
                        case 3:
                            rx = cz;
                            rz = -cx;
                            break;
                        default:
                            rx = cx;
                            rz = cz;
                    }
                    out.add(new Cell(rx, y, rz, c));
                }
            }
        }
        return out;
    }

    /** How many of each palette block the structure uses (its cost before {@link #extraCost}). */
    public Map<String, Integer> blockCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Cell c : cells(0)) {
            if (c.key != AIR && c.key != CORE) {
                counts.merge(palette.get(String.valueOf(c.key)), 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(counts);
    }
}
