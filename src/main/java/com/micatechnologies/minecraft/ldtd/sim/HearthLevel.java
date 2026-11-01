package com.micatechnologies.minecraft.ldtd.sim;

/**
 * One row of the Hearth level table: what a Hearth of this level gives you. Loaded from
 * {@code config/ldtd/hearth_levels.json}. Fields are public and non-final so Gson can fill them.
 */
public final class HearthLevel {

    /** The level number. Levels run 1, 2, 3, ... with no gaps. */
    public int level;

    /** Territory radius in blocks. */
    public int radius;

    /** How many Hearth-sprites this Hearth may keep. */
    public int maxSprites;

    /** Duskglass it takes to reach this level (Phase 7). 0 for level 1. */
    public int duskglass;

    /** Stockpile materials it takes to reach this level, {@code modid:name[@meta]} to count. */
    public java.util.Map<String, Integer> materials = new java.util.LinkedHashMap<>();

    public HearthLevel() {
    }

    public HearthLevel(int level, int radius, int maxSprites) {
        this.level = level;
        this.radius = radius;
        this.maxSprites = maxSprites;
    }
}
