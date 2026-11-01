package com.micatechnologies.minecraft.ldtd.sim.raid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How raids are built and how the Hearth's Flame behaves, loaded from {@code config/ldtd/raid.json}.
 * Minecraft-free. Fields are public and non-final so Gson can fill them.
 */
public final class RaidRules {

    /** Seconds between the dusk warning and the first Gloam arriving. */
    public int countdownSeconds = 30;

    /** How many waves a raid's units are split into. */
    public int waves = 3;

    /** Seconds between waves. */
    public int waveIntervalSeconds = 40;

    public Budget budget = new Budget();

    /** Extra budget (and, later, reward) for a raid called early at the Hearth. 0.25 = +25%. */
    public double callEarlyBonus = 0.25D;

    public Flame flame = new Flame();

    /** Duskglass paid into the stockpile for surviving a raid (Phase 7). */
    public Payout payout = new Payout();

    /** Late raids send tougher Gloam rather than ever more of them (Phase 10 balance). */
    public Toughness toughness = new Toughness();

    /** Per-unit raid rules, keyed by the unit id in {@code gloam.json}. */
    public Map<String, Unit> units = new LinkedHashMap<>();

    public static final class Budget {
        /** Budget points for the very first raid. */
        public int base = 6;
        /** Extra points for each raid the Hearth has already survived. */
        public int perNight = 3;
        /** Extra points per Hearth level above 1. */
        public int perLevel = 4;
        /** Hard ceiling, whatever the night. */
        public int max = 400;
    }

    public static final class Toughness {
        /** Extra health for each raid the Hearth has already survived, as a fraction (0.03 = +3%). */
        public double perNight = 0.03D;
        /** Ceiling on the health multiplier. */
        public double max = 2.5D;
    }

    /** The health multiplier for the Gloam of raid {@code night} (counting from 1). */
    public double healthMultiplier(int night) {
        return Math.min(toughness.max, 1.0D + toughness.perNight * Math.max(0, night - 1));
    }

    public static final class Flame {
        /** Full Flame. */
        public double max = 100.0D;
        /** Flame regained per minute while no raid is on. */
        public double regenPerMinute = 5.0D;
    }

    public static final class Payout {
        /** Paid for every raid survived. */
        public int base = 2;
        /** Extra per raid number (raid 5 pays base + 5 x perRaid). */
        public int perRaid = 1;
        /** Multiplier for a raid called early. */
        public double calledMultiplier = 1.25D;
    }

    /** Duskglass for surviving raid {@code night} (counting from 1). */
    public int payoutFor(int night, boolean calledEarly) {
        double p = payout.base + (double) payout.perRaid * night;
        return (int) Math.round(calledEarly ? p * payout.calledMultiplier : p);
    }

    public static final class Unit {
        /** The first raid (counting from 1) this unit can appear in. */
        public int fromNight = 1;
        /** Relative chance of being picked, among the units that fit the remaining budget. */
        public double weight = 1.0D;
        /** Flame drained per second while it stands at the Hearth. */
        public double drain = 1.0D;
        /** Most of the raid's budget this unit may take, 0-1. */
        public double maxShare = 1.0D;
    }

    /**
     * Checks the rules, including that every unit in {@code unitIds} has raid rules.
     *
     * @throws IllegalArgumentException with a human-readable reason
     */
    public RaidRules validate(Iterable<String> unitIds) {
        range("countdownSeconds", countdownSeconds, 0, 600);
        range("waves", waves, 1, 20);
        range("waveIntervalSeconds", waveIntervalSeconds, 1, 600);
        if (budget == null || flame == null || units == null) {
            throw new IllegalArgumentException("budget, flame and units are all required");
        }
        if (payout == null) {
            payout = new Payout();
        }
        if (toughness == null) {
            toughness = new Toughness();
        }
        range("toughness.perNight", toughness.perNight, 0.0D, 10.0D);
        range("toughness.max", toughness.max, 1.0D, 100.0D);
        range("payout.base", payout.base, 0, 100000);
        range("payout.perRaid", payout.perRaid, 0, 100000);
        range("payout.calledMultiplier", payout.calledMultiplier, 0.0D, 100.0D);
        range("budget.base", budget.base, 1, 100000);
        range("budget.perNight", budget.perNight, 0, 100000);
        range("budget.perLevel", budget.perLevel, 0, 100000);
        range("budget.max", budget.max, budget.base, 100000);
        range("callEarlyBonus", callEarlyBonus, 0.0D, 10.0D);
        range("flame.max", flame.max, 1.0D, 100000.0D);
        range("flame.regenPerMinute", flame.regenPerMinute, 0.0D, 100000.0D);
        boolean anyFirstNight = false;
        for (String id : unitIds) {
            Unit u = units.get(id);
            if (u == null) {
                throw new IllegalArgumentException("unit '" + id + "' has no raid rules");
            }
            range(id + ".fromNight", u.fromNight, 1, 100000);
            range(id + ".weight", u.weight, 0.0D, 100000.0D);
            range(id + ".drain", u.drain, 0.0D, 100000.0D);
            range(id + ".maxShare", u.maxShare, 0.0D, 1.0D);
            anyFirstNight |= u.fromNight == 1 && u.weight > 0.0D && u.maxShare > 0.0D;
        }
        if (!anyFirstNight) {
            throw new IllegalArgumentException("no unit can appear in the first raid (fromNight 1, weight > 0)");
        }
        units = Collections.unmodifiableMap(new LinkedHashMap<>(units));
        return this;
    }

    /** The budget for a raid. {@code night} counts from 1. */
    public int budgetFor(int night, int hearthLevel, boolean calledEarly) {
        long b = (long) budget.base + (long) budget.perNight * (night - 1) + (long) budget.perLevel * (hearthLevel - 1);
        if (calledEarly) {
            b = Math.round(b * (1.0D + callEarlyBonus));
        }
        return (int) Math.max(1, Math.min(b, budget.max));
    }

    private static void range(String field, double v, double min, double max) {
        if (Double.isNaN(v) || v < min || v > max) {
            throw new IllegalArgumentException(field + " is " + v + ", outside " + min + ".." + max);
        }
    }
}
