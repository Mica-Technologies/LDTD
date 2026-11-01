package com.micatechnologies.minecraft.ldtd.sim.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.micatechnologies.minecraft.ldtd.sim.GloamStats;
import com.micatechnologies.minecraft.ldtd.sim.balance.RaidSim.Tower;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidComposer;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidRules;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerStats;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerUpgrade;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The balance harness (Phase 10): composes real raids from the shipped defaults and runs them past
 * typical defenses. It prints a table (run with --info to see it) and checks the shape of the
 * difficulty curve, so a data change that breaks the curve fails the build.
 */
class BalanceTest {

    private static final double PATH = 48.0D;
    private static final int SAMPLES = 40;

    private static Map<String, GloamStats> gloam;
    private static Map<String, TowerStats> towers;
    private static TowerUpgrade upgrade;
    private static RaidRules rules;
    private static Map<String, Integer> costs;

    private static JsonObject read(String name) throws Exception {
        try (Reader r = new InputStreamReader(BalanceTest.class.getResourceAsStream("/ldtd/defaults/" + name),
            StandardCharsets.UTF_8)) {
            return new Gson().fromJson(r, JsonObject.class);
        }
    }

    @BeforeAll
    static void load() throws Exception {
        Gson gson = new Gson();
        gloam = gson.fromJson(read("gloam.json").get("units"), new TypeToken<Map<String, GloamStats>>() { }.getType());
        JsonObject t = read("towers.json");
        towers = gson.fromJson(t.get("towers"), new TypeToken<Map<String, TowerStats>>() { }.getType());
        upgrade = gson.fromJson(t.get("upgrade"), TowerUpgrade.class);
        rules = gson.fromJson(read("raid.json"), RaidRules.class);
        costs = new HashMap<>();
        gloam.forEach((k, v) -> costs.put(k, v.budgetCost));
    }

    /** The defense a player might reasonably have at each Hearth level. */
    static List<Tower> defenseFor(int level) {
        switch (level) {
            case 1:
                return Arrays.asList(new Tower("archer", 1, 30));
            case 2:
                return Arrays.asList(new Tower("archer", 1, 24), new Tower("archer", 1, 36));
            case 3:
                return Arrays.asList(new Tower("archer", 2, 20), new Tower("archer", 1, 36),
                    new Tower("frost", 1, 28), new Tower("lantern", 1, 40));
            case 4:
                return Arrays.asList(new Tower("archer", 2, 16), new Tower("archer", 2, 36),
                    new Tower("cannon", 1, 26), new Tower("frost", 2, 30), new Tower("lantern", 1, 40));
            default:
                return Arrays.asList(new Tower("archer", 3, 14), new Tower("archer", 3, 34),
                    new Tower("cannon", 2, 24), new Tower("cannon", 1, 40), new Tower("frost", 3, 30),
                    new Tower("lantern", 1, 22), new Tower("lantern", 1, 42));
        }
    }

    /** Average leak rate of raid {@code night} on a Hearth of {@code level} defended as {@code defense}. */
    static double leak(int night, int level, List<Tower> defense) {
        RaidSim sim = new RaidSim(gloam, towers, upgrade);
        sim.healthMultiplier = rules.healthMultiplier(night);
        Random random = new Random(night * 31L + level);
        int units = 0;
        int leaked = 0;
        for (int s = 0; s < SAMPLES; s++) {
            List<List<String>> waves = RaidComposer.compose(rules, costs, rules.budgetFor(night, level, false), night,
                random);
            RaidSim.Result r = sim.runRaid(waves, defense, PATH);
            units += r.units;
            leaked += r.leaked;
        }
        return (double) leaked / units;
    }

    @Test
    void groundSpeedMatchesVanilla() {
        // A zombie (0.23) walks about 2.3 blocks a second.
        assertEquals(2.33D, RaidSim.blocksPerTick(0.23D) * 20.0D, 0.05D);
    }

    @Test
    void armorMatchesVanilla() {
        // 8 damage into 10 armor: max(2, 10 - 4) = 6 points cut, 24% off.
        assertEquals(6.08D, RaidSim.afterArmor(8.0D, 10.0D), 0.01D);
    }

    @Test
    void flareTowerDownsFliersOnly() {
        RaidSim sim = new RaidSim(gloam, towers, upgrade);
        List<String> moths = Arrays.asList("moth_wraith", "moth_wraith", "moth_wraith", "moth_wraith");
        List<String> shades = Arrays.asList("shade", "shade", "shade", "shade");
        List<Tower> flare = Arrays.asList(new Tower("flare", 1, 24));
        // Against fliers it holds the line; against walkers it never fires.
        assertTrue(sim.run(moths, flare, PATH).leakRate() < sim.run(moths, new ArrayList<>(), PATH).leakRate(),
            "a flare tower should stop some Moth-wraiths");
        assertEquals(1.0D, sim.run(shades, flare, PATH).leakRate(), 1e-9, "a flare tower must ignore walkers");
    }

    @Test
    void lanternTiersWidenTheLight() {
        RaidSim sim = new RaidSim(gloam, towers, upgrade);
        assertEquals(sim.lanternReach, sim.lanternReachAt(1), 1e-9, "tier I is just the core's light");
        assertTrue(sim.lanternReachAt(2) > sim.lanternReachAt(1), "tier II's aura reaches further");
        assertTrue(sim.lanternReachAt(3) > sim.lanternReachAt(2), "tier III's aura reaches further still");
    }

    @Test
    void difficultyCurve() {
        StringBuilder table = new StringBuilder("night lvl budget | leak% own-level | leak% one-level-behind\n");
        List<Double> onPace = new ArrayList<>();
        List<Double> behind = new ArrayList<>();
        for (int night = 1; night <= 30; night++) {
            int level = Math.min(5, 1 + (night - 1) / 3);
            double own = leak(night, level, defenseFor(level));
            double lag = level > 1 ? leak(night, level, defenseFor(level - 1)) : own;
            onPace.add(own);
            behind.add(lag);
            table.append(String.format("%5d %3d %6d | %5.1f        | %5.1f%n", night, level,
                rules.budgetFor(night, level, false), own * 100, lag * 100));
        }
        System.out.println(table);
        // An undefended Hearth loses everything, even on night 1: raids have teeth.
        assertEquals(1.0D, leak(1, 1, new ArrayList<>()), 1e-9);
        // The first nights need the player's own blade beside a lone archer, but never hopelessly.
        for (int i = 0; i < 3; i++) {
            assertTrue(onPace.get(i) <= 0.5D, "night " + (i + 1) + " leaks " + onPace.get(i) + " on pace");
        }
        // From then on a player keeping pace holds the line on towers alone.
        for (int i = 3; i < onPace.size(); i++) {
            assertTrue(onPace.get(i) <= 0.25D, "night " + (i + 1) + " leaks " + onPace.get(i) + " on pace");
        }
        // Falling a level behind costs you. Right after a level-up the older defense may still
        // cope for a raid or two, but it never does better, and over the mid and late game it
        // clearly leaks more.
        double gap = 0.0D;
        for (int i = 6; i < behind.size(); i++) {
            assertTrue(behind.get(i) >= onPace.get(i), "night " + (i + 1) + ": lagging should never help");
            gap += behind.get(i) - onPace.get(i);
        }
        assertTrue(gap / (behind.size() - 6) > 0.1D, "lagging a level behind should hurt on average");
        // And there is no perfect fortress: by night 60 even a full level-5 defense starts to give.
        assertTrue(leak(60, 5, defenseFor(5)) > 0.05D, "the endgame should still press a maxed defense");
    }
}
