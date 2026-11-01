package com.micatechnologies.minecraft.ldtd.sim.raid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RaidComposerTest {

    private static final List<String> IDS = Arrays.asList("shade", "smother", "moth");

    private static RaidRules rules() {
        RaidRules r = new RaidRules();
        r.units.put("shade", unit(1, 10.0D, 1.0D));
        r.units.put("smother", unit(3, 2.0D, 0.3D));
        r.units.put("moth", unit(4, 3.0D, 1.0D));
        return r.validate(IDS);
    }

    private static RaidRules.Unit unit(int fromNight, double weight, double maxShare) {
        RaidRules.Unit u = new RaidRules.Unit();
        u.fromNight = fromNight;
        u.weight = weight;
        u.maxShare = maxShare;
        return u;
    }

    private static Map<String, Integer> costs() {
        Map<String, Integer> c = new HashMap<>();
        c.put("shade", 1);
        c.put("smother", 5);
        c.put("moth", 2);
        return c;
    }

    private static int cost(List<List<String>> waves) {
        int total = 0;
        for (List<String> w : waves) {
            for (String id : w) {
                total += costs().get(id);
            }
        }
        return total;
    }

    @Test
    void firstNightOnlyHasFirstNightUnits() {
        List<List<String>> waves = RaidComposer.compose(rules(), costs(), 10, 1, new Random(1));
        for (List<String> w : waves) {
            for (String id : w) {
                assertEquals("shade", id);
            }
        }
        assertEquals(10, cost(waves), "a raid of only 1-point units spends its whole budget");
    }

    @Test
    void neverOverspends() {
        for (int seed = 0; seed < 200; seed++) {
            List<List<String>> waves = RaidComposer.compose(rules(), costs(), 37, 9, new Random(seed));
            assertTrue(cost(waves) <= 37);
        }
    }

    @Test
    void respectsShareCap() {
        for (int seed = 0; seed < 200; seed++) {
            int smotherCost = 0;
            for (List<String> w : RaidComposer.compose(rules(), costs(), 50, 9, new Random(seed))) {
                for (String id : w) {
                    if (id.equals("smother")) {
                        smotherCost += 5;
                    }
                }
            }
            assertTrue(smotherCost <= 15, "smothers took " + smotherCost + " of 50 with a 0.3 cap");
        }
    }

    @Test
    void sameSeedSameRaid() {
        assertEquals(RaidComposer.compose(rules(), costs(), 40, 6, new Random(7)),
            RaidComposer.compose(rules(), costs(), 40, 6, new Random(7)));
    }

    @Test
    void heavyUnitsComeInLaterWaves() {
        List<List<String>> waves = RaidComposer.compose(rules(), costs(), 60, 9, new Random(3));
        assertEquals(3, waves.size());
        int firstMax = waves.get(0).stream().mapToInt(id -> costs().get(id)).max().orElse(0);
        int lastMin = waves.get(2).stream().mapToInt(id -> costs().get(id)).min().orElse(0);
        assertTrue(firstMax <= lastMin, "wave 1 holds the cheapest units, the last wave the dearest");
    }

    @Test
    void tinyBudgetStillSendsOneUnit() {
        Map<String, Integer> c = costs();
        c.put("shade", 3);
        List<List<String>> waves = RaidComposer.compose(rules(), c, 1, 1, new Random(1));
        assertEquals(1, waves.size());
        assertEquals(Arrays.asList("shade"), waves.get(0));
    }

    @Test
    void budgetGrowsWithNightsAndLevelAndCalling() {
        RaidRules r = rules();
        assertEquals(6, r.budgetFor(1, 1, false));
        assertEquals(6 + 3 * 4 + 4 * 2, r.budgetFor(5, 3, false));
        assertEquals(Math.round(26 * 1.25D), r.budgetFor(5, 3, true));
        assertEquals(400, r.budgetFor(100000, 5, true));
    }

    @Test
    void validationRejectsMissingUnitsAndNoFirstNightUnit() {
        RaidRules r = new RaidRules();
        r.units.put("shade", unit(2, 1.0D, 1.0D));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> r.validate(Arrays.asList("shade")));
        assertTrue(e.getMessage().contains("first raid"));
        RaidRules missing = new RaidRules();
        missing.units.put("shade", unit(1, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> missing.validate(IDS));
        assertFalse(missing.units.isEmpty());
    }
}
