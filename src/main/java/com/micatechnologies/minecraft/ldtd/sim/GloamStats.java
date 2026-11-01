package com.micatechnologies.minecraft.ldtd.sim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-unit stats for the Gloam, loaded from {@code config/ldtd/gloam.json}. Minecraft-free.
 * Fields are public and non-final so Gson can fill them.
 */
public final class GloamStats {

    /** Max health, in half-hearts. */
    public double health = 20.0D;

    /** Movement speed attribute (vanilla zombie: 0.23). */
    public double speed = 0.25D;

    /** Melee damage, in half-hearts. */
    public double attack = 3.0D;

    /** Armor points. */
    public double armor = 0.0D;

    /**
     * How hard it hits blocks when sieging, as a multiple of a bare-handed player. 0 means it never
     * digs and waits for others to open the way.
     */
    public double digPower = 1.0D;

    /** Raid budget points this unit costs (used by the raid director, Phase 4). */
    public int budgetCost = 1;

    /** Chance (0-1) that it drops Duskglass when killed (not when it fades in daylight). */
    public double duskglassChance = 0.3D;

    /** How many Duskglass it drops when it does. */
    public int duskglassCount = 1;

    /**
     * Validates a whole {@code gloam.json} map. Every unit the mod defines must be present and sane.
     *
     * @throws IllegalArgumentException with a human-readable reason
     */
    public static Map<String, GloamStats> validate(Map<String, GloamStats> raw, Iterable<String> required) {
        if (raw == null) {
            throw new IllegalArgumentException("the file has no units");
        }
        Map<String, GloamStats> out = new LinkedHashMap<>();
        for (String id : required) {
            GloamStats s = raw.get(id);
            if (s == null) {
                throw new IllegalArgumentException("unit '" + id + "' is missing");
            }
            check(id, "health", s.health, 1.0D, 2048.0D);
            check(id, "speed", s.speed, 0.0D, 1.0D);
            check(id, "attack", s.attack, 0.0D, 1024.0D);
            check(id, "armor", s.armor, 0.0D, 30.0D);
            check(id, "digPower", s.digPower, 0.0D, 100.0D);
            check(id, "budgetCost", s.budgetCost, 1.0D, 1000.0D);
            check(id, "duskglassChance", s.duskglassChance, 0.0D, 1.0D);
            check(id, "duskglassCount", s.duskglassCount, 0.0D, 64.0D);
            out.put(id, s);
        }
        return Collections.unmodifiableMap(out);
    }

    private static void check(String id, String field, double v, double min, double max) {
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException("unit '" + id + "' has " + field + " " + v + ", outside " + min + ".." + max);
        }
    }
}
