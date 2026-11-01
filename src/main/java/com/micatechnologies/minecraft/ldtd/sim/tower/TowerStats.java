package com.micatechnologies.minecraft.ldtd.sim.tower;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-tower numbers, loaded from {@code config/ldtd/towers.json}. Minecraft-free. Fields are public
 * and non-final so Gson can fill them. Not every field matters to every tower.
 */
public final class TowerStats {

    /** Targeting range in blocks. 0 for towers that don't shoot (the lantern). */
    public double range = 16.0D;

    /** Ticks between shots. */
    public int cooldownTicks = 20;

    /** Damage per hit, in half-hearts. */
    public double damage = 4.0D;

    /** Cannon: splash radius in blocks. */
    public double splashRadius = 0.0D;

    /** Frost: slowness level applied (0 = Slowness I). */
    public int slowAmplifier = 0;

    /** Frost: how long the slow lasts, in ticks. */
    public int slowTicks = 0;

    /** Guard post: how many Ember Guards it keeps. cooldownTicks is then the time to replace one. */
    public int units = 0;

    /**
     * @throws IllegalArgumentException with a human-readable reason
     */
    public static Map<String, TowerStats> validate(Map<String, TowerStats> raw, Iterable<String> required) {
        if (raw == null) {
            throw new IllegalArgumentException("the file has no towers");
        }
        Map<String, TowerStats> out = new LinkedHashMap<>();
        for (String id : required) {
            TowerStats s = raw.get(id);
            if (s == null) {
                throw new IllegalArgumentException("tower '" + id + "' is missing");
            }
            check(id, "range", s.range, 0.0D, 64.0D);
            check(id, "cooldownTicks", s.cooldownTicks, 1.0D, 1200.0D);
            check(id, "damage", s.damage, 0.0D, 1000.0D);
            check(id, "splashRadius", s.splashRadius, 0.0D, 8.0D);
            check(id, "slowAmplifier", s.slowAmplifier, 0.0D, 5.0D);
            check(id, "slowTicks", s.slowTicks, 0.0D, 1200.0D);
            check(id, "units", s.units, 0.0D, 16.0D);
            out.put(id, s);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void check(String id, String field, double v, double min, double max) {
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException("tower '" + id + "' has " + field + " " + v + ", outside " + min + ".." + max);
        }
    }
}
