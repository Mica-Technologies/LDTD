package com.micatechnologies.minecraft.ldtd.sim.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Turns a raid budget into the units of a raid, split into waves. Minecraft-free.
 *
 * <p>Budget-based generation (research doc section 2.3): units are bought one at a time, each picked
 * by weight among the units that are allowed tonight, still fit the remaining budget, and haven't
 * hit their share cap. That scales to any night without hand-written wave lists, and the gating
 * ({@code fromNight}) and caps ({@code maxShare}) keep early raids gentle and late raids varied.</p>
 *
 * <p>Waves get more expensive towards the end: units are dealt cheapest-first into waves of equal
 * count, so the heavy hitters arrive last, once the defenses are already busy.</p>
 */
public final class RaidComposer {

    private RaidComposer() {
        throw new AssertionError("No instances.");
    }

    /**
     * @param costs  budget cost of every unit, by id
     * @param budget the raid's budget, from {@link RaidRules#budgetFor}
     * @param night  which raid this is for the Hearth, counting from 1
     * @return the waves, each a list of unit ids. Never empty: a raid always has at least one unit.
     */
    public static List<List<String>> compose(RaidRules rules, Map<String, Integer> costs, int budget, int night,
                                             Random random) {
        List<String> picked = new ArrayList<>();
        Map<String, Integer> spent = new HashMap<>();
        int remaining = budget;
        while (true) {
            List<String> options = new ArrayList<>();
            double totalWeight = 0.0D;
            for (Map.Entry<String, RaidRules.Unit> e : rules.units.entrySet()) {
                String id = e.getKey();
                RaidRules.Unit u = e.getValue();
                Integer cost = costs.get(id);
                if (cost == null || u.weight <= 0.0D || night < u.fromNight || cost > remaining) {
                    continue;
                }
                if (spent.getOrDefault(id, 0) + cost > u.maxShare * budget) {
                    continue;
                }
                options.add(id);
                totalWeight += u.weight;
            }
            if (options.isEmpty()) {
                break;
            }
            double roll = random.nextDouble() * totalWeight;
            String choice = options.get(options.size() - 1);
            for (String id : options) {
                roll -= rules.units.get(id).weight;
                if (roll < 0.0D) {
                    choice = id;
                    break;
                }
            }
            int cost = costs.get(choice);
            picked.add(choice);
            spent.merge(choice, cost, Integer::sum);
            remaining -= cost;
        }
        if (picked.isEmpty()) {
            picked.add(cheapestFirstNightUnit(rules, costs));
        }
        return split(picked, costs, rules.waves);
    }

    private static String cheapestFirstNightUnit(RaidRules rules, Map<String, Integer> costs) {
        String best = null;
        for (Map.Entry<String, RaidRules.Unit> e : rules.units.entrySet()) {
            if (e.getValue().fromNight == 1 && e.getValue().weight > 0.0D && costs.containsKey(e.getKey())
                && (best == null || costs.get(e.getKey()) < costs.get(best))) {
                best = e.getKey();
            }
        }
        if (best == null) {
            throw new IllegalStateException("validated rules always have a first-night unit");
        }
        return best;
    }

    private static List<List<String>> split(List<String> units, Map<String, Integer> costs, int waveCount) {
        units.sort((a, b) -> Integer.compare(costs.get(a), costs.get(b)));
        int waves = Math.max(1, Math.min(waveCount, units.size()));
        List<List<String>> out = new ArrayList<>();
        for (int w = 0; w < waves; w++) {
            out.add(new ArrayList<>());
        }
        for (int i = 0; i < units.size(); i++) {
            out.get(i * waves / units.size()).add(units.get(i));
        }
        return out;
    }
}
