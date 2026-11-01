package com.micatechnologies.minecraft.ldtd.sim.balance;

import com.micatechnologies.minecraft.ldtd.sim.GloamStats;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerStats;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerUpgrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An offline raid simulator for balancing (Phase 10). Minecraft-free.
 *
 * <p>The route is a line {@code pathLength} blocks long, ending at the Hearth. Gloam enter one by
 * one, {@code spacingTicks} apart, and walk it at their real ground speed. Towers stand beside the
 * route ({@code offset} blocks off it) at fixed distances along it and shoot the way the real ones
 * do: an archer or cannon fires at the unit furthest along within range, the cannon also hits
 * everything within its splash, the frost tower hits and slows everything in range, and the lantern
 * lights a stretch of the route. A lit Gloam takes more damage and walks slower (design decision
 * D15). Armor reduces each hit the way vanilla does.</p>
 *
 * <p>It leaves out players, Ember Guards, walls and the siege, so it measures what towers alone hold
 * back. That is the right baseline: a defense the player also fights beside should do better.</p>
 */
public final class RaidSim {

    /** A tower beside the route. */
    public static final class Tower {
        final String kind;
        final int tier;
        final double at;

        /**
         * @param kind tower id in towers.json (archer, cannon, frost, lantern)
         * @param tier 1-3
         * @param at   distance along the route, in blocks from the entrance
         */
        public Tower(String kind, int tier, double at) {
            this.kind = kind;
            this.tier = tier;
            this.at = at;
        }
    }

    /** The result of one wave or raid. */
    public static final class Result {
        public int units;
        public int leaked;
        public double hpTotal;
        public double damageDealt;

        public double leakRate() {
            return units == 0 ? 0.0D : (double) leaked / units;
        }
    }

    /** How far the towers stand from the route. */
    public double offset = 4.0D;
    /** Ticks between one Gloam entering and the next. */
    public int spacingTicks = 10;
    /** Light reach of a lantern along the route, in blocks either side (light 15 falls to 10 in 5). */
    public double lanternReach = 5.0D;
    public double litDamageMultiplier = 1.5D;
    /** Raid toughness: every unit's health is multiplied by this. */
    public double healthMultiplier = 1.0D;
    public double litSlowdown = 0.15D;

    private final Map<String, GloamStats> gloam;
    private final Map<String, TowerStats> towers;
    private final TowerUpgrade upgrade;

    public RaidSim(Map<String, GloamStats> gloam, Map<String, TowerStats> towers, TowerUpgrade upgrade) {
        this.gloam = gloam;
        this.towers = towers;
        this.upgrade = upgrade;
    }

    /** Ground speed in blocks per tick for a movement attribute (vanilla friction 0.546 on grass). */
    public static double blocksPerTick(double attribute) {
        return attribute * attribute / (1.0D - 0.546D);
    }

    /** A hit after vanilla armor (no toughness). */
    public static double afterArmor(double damage, double armor) {
        double cut = Math.min(20.0D, Math.max(armor / 5.0D, armor - damage / 2.0D));
        return damage * (1.0D - cut / 25.0D);
    }

    /** Runs one wave of units down a route of {@code pathLength} blocks past the given towers. */
    public Result run(List<String> units, List<Tower> defense, double pathLength) {
        int n = units.size();
        double[] pos = new double[n];
        double[] hp = new double[n];
        double[] speed = new double[n];
        double[] armor = new double[n];
        int[] slowUntil = new int[n];
        int[] slowLevel = new int[n];
        boolean[] done = new boolean[n];
        Result r = new Result();
        r.units = n;
        for (int i = 0; i < n; i++) {
            GloamStats s = gloam.get(units.get(i));
            pos[i] = -i * spacingTicks * blocksPerTick(s.speed);
            hp[i] = s.health * healthMultiplier;
            speed[i] = s.speed;
            armor[i] = s.armor;
            r.hpTotal += hp[i];
        }
        int[] cooldown = new int[defense.size()];
        int alive = n;
        for (int tick = 0; alive > 0 && tick < 20 * 60 * 10; tick++) {
            // Move.
            for (int i = 0; i < n; i++) {
                if (done[i]) {
                    continue;
                }
                // Light and frost don't stack: only the stronger slow applies (as in game).
                double slow = lit(pos[i], defense) ? litSlowdown : 0.0D;
                if (tick < slowUntil[i]) {
                    slow = Math.max(slow, 0.15D * (slowLevel[i] + 1));
                }
                double attr = speed[i] * Math.max(0.0D, 1.0D - slow);
                pos[i] += blocksPerTick(attr);
                if (pos[i] >= pathLength) {
                    done[i] = true;
                    r.leaked++;
                    alive--;
                }
            }
            // Shoot.
            for (int t = 0; t < defense.size(); t++) {
                Tower tw = defense.get(t);
                TowerStats base = towers.get(tw.kind);
                if (base == null || base.range <= 0.0D || base.units > 0) {
                    continue;
                }
                if (cooldown[t] > 0) {
                    cooldown[t]--;
                    continue;
                }
                double range = base.range * upgrade.range(tw.tier);
                double reach = range > offset ? Math.sqrt(range * range - offset * offset) : 0.0D;
                double damage = base.damage * upgrade.damage(tw.tier);
                List<Integer> inRange = new ArrayList<>();
                int target = -1;
                for (int i = 0; i < n; i++) {
                    if (!done[i] && pos[i] >= 0.0D && Math.abs(pos[i] - tw.at) <= reach) {
                        inRange.add(i);
                        if (target < 0 || pos[i] > pos[target]) {
                            target = i;
                        }
                    }
                }
                if (target < 0) {
                    continue;
                }
                cooldown[t] = (int) Math.round(base.cooldownTicks * upgrade.cooldown(tw.tier));
                List<Integer> hit = new ArrayList<>();
                if (base.slowTicks > 0) {
                    hit.addAll(inRange);
                    for (int i : inRange) {
                        slowUntil[i] = tick + base.slowTicks;
                        slowLevel[i] = base.slowAmplifier;
                    }
                } else if (base.splashRadius > 0.0D) {
                    for (int i : inRange) {
                        if (Math.abs(pos[i] - pos[target]) <= base.splashRadius) {
                            hit.add(i);
                        }
                    }
                } else {
                    hit.add(target);
                }
                for (int i : hit) {
                    double d = damage * (lit(pos[i], defense) ? litDamageMultiplier : 1.0D);
                    d = afterArmor(d, armor[i]);
                    double dealt = Math.min(d, hp[i]);
                    hp[i] -= dealt;
                    r.damageDealt += dealt;
                    if (hp[i] <= 0.0D && !done[i]) {
                        done[i] = true;
                        alive--;
                    }
                }
            }
        }
        return r;
    }

    /** Runs every wave of a raid in turn and adds the results up. */
    public Result runRaid(List<List<String>> waves, List<Tower> defense, double pathLength) {
        Result total = new Result();
        for (List<String> wave : waves) {
            Result w = run(wave, defense, pathLength);
            total.units += w.units;
            total.leaked += w.leaked;
            total.hpTotal += w.hpTotal;
            total.damageDealt += w.damageDealt;
        }
        return total;
    }

    private boolean lit(double p, List<Tower> defense) {
        for (Tower t : defense) {
            if ("lantern".equals(t.kind) && Math.abs(p - t.at) <= lanternReach) {
                return true;
            }
        }
        return false;
    }
}
