package com.micatechnologies.minecraft.ldtd.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.micatechnologies.minecraft.ldtd.Ldtd;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.sim.GloamStats;
import com.micatechnologies.minecraft.ldtd.sim.HearthLevel;
import com.micatechnologies.minecraft.ldtd.sim.HearthLevelTable;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidRules;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.sim.tower.TowerStats;
import com.micatechnologies.minecraft.ldtd.tower.TowerKind;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LDTD's data-driven content (design decision D11).
 *
 * <p>Content lives as JSON in {@code config/ldtd/}. The defaults ship in the jar under
 * {@code /ldtd/defaults/} and are copied out the first time, so a server owner always has a file to
 * edit. Each file is validated as it is read. A broken file is reported in the log and the last good
 * values (or, on first load, the jar's defaults) stay in force, so a typo can't take a server down.
 * {@code /ldtd reload} re-reads everything.</p>
 *
 * <p>When a newer LDTD adds a unit that an older edited file doesn't have, the jar's default for
 * that unit is used and the gap is reported, rather than the whole file being rejected.</p>
 */
public final class LdtdData {

    private static final Gson GSON = new Gson();
    private static final String DEFAULTS_ROOT = "/ldtd/defaults/";
    private static final String HEARTH_LEVELS = "hearth_levels.json";
    private static final String GLOAM = "gloam.json";
    private static final String RAID = "raid.json";
    private static final String TOWERS = "towers.json";
    private static final String BLUEPRINTS = "blueprints.json";

    private static File dataDir;
    private static volatile HearthLevelTable hearthLevels;
    private static volatile Map<String, GloamStats> gloam;
    private static volatile RaidRules raid;
    private static volatile Map<String, TowerStats> towers;
    private static volatile Map<String, Blueprint> blueprints;

    private LdtdData() {
        throw new AssertionError("No instances.");
    }

    /** Sets the data folder up and loads everything. Called once in preInit. */
    public static void init(File modConfigDir) {
        dataDir = new File(modConfigDir, "ldtd");
        if (!dataDir.isDirectory() && !dataDir.mkdirs()) {
            Ldtd.LOGGER.error("Could not create {}; using built-in defaults only", dataDir);
        }
        copyDefaultIfMissing(HEARTH_LEVELS);
        copyDefaultIfMissing(GLOAM);
        copyDefaultIfMissing(RAID);
        copyDefaultIfMissing(TOWERS);
        copyDefaultIfMissing(BLUEPRINTS);
        hearthLevels = HearthLevelTable.validate(readDefault(HEARTH_LEVELS, HearthLevelsFile.class).levels);
        gloam = GloamStats.validate(readDefault(GLOAM, GloamFile.class).units, GloamKind.ids());
        raid = readDefault(RAID, RaidRules.class).validate(GloamKind.ids());
        towers = TowerStats.validate(readDefault(TOWERS, TowersFile.class).towers, TowerKind.ids());
        blueprints = validateBlueprints(readDefault(BLUEPRINTS, BlueprintsFile.class).blueprints);
        reload();
    }

    /**
     * Re-reads every data file.
     *
     * @return the problems found, one line each. Empty when everything loaded.
     */
    public static List<String> reload() {
        List<String> problems = new ArrayList<>();
        try {
            hearthLevels = HearthLevelTable.validate(readUser(HEARTH_LEVELS, HearthLevelsFile.class).levels);
        } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
            problems.add(report(HEARTH_LEVELS, e));
        }
        try {
            Map<String, GloamStats> units = new LinkedHashMap<>();
            Map<String, GloamStats> user = readUser(GLOAM, GloamFile.class).units;
            if (user != null) {
                units.putAll(user);
            }
            Map<String, GloamStats> defaults = readDefault(GLOAM, GloamFile.class).units;
            for (String id : GloamKind.ids()) {
                if (!units.containsKey(id)) {
                    units.put(id, defaults.get(id));
                    String msg = GLOAM + ": unit '" + id + "' is missing, so the built-in default is used";
                    Ldtd.LOGGER.warn(msg);
                    problems.add(msg);
                }
            }
            gloam = GloamStats.validate(units, GloamKind.ids());
        } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
            problems.add(report(GLOAM, e));
        }
        try {
            raid = readUser(RAID, RaidRules.class).validate(GloamKind.ids());
        } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
            problems.add(report(RAID, e));
        }
        try {
            towers = TowerStats.validate(readUser(TOWERS, TowersFile.class).towers, TowerKind.ids());
        } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
            problems.add(report(TOWERS, e));
        }
        try {
            blueprints = validateBlueprints(readUser(BLUEPRINTS, BlueprintsFile.class).blueprints);
        } catch (IOException | JsonParseException | IllegalArgumentException | IllegalStateException e) {
            problems.add(report(BLUEPRINTS, e));
        }
        return problems;
    }

    public static HearthLevelTable hearthLevels() {
        return hearthLevels;
    }

    /** Stats for one Gloam unit. Always present: validation guarantees every {@link GloamKind}. */
    public static GloamStats gloam(GloamKind kind) {
        return gloam.get(kind.id());
    }

    /** How raids are built, and the Hearth's Flame rules. */
    public static RaidRules raid() {
        return raid;
    }

    /** Stats for one tower kind. Always present: validation guarantees every {@link TowerKind}. */
    public static TowerStats tower(TowerKind kind) {
        return towers.get(kind.id());
    }

    /** Every blueprint, by id, in file order. */
    public static Map<String, Blueprint> blueprints() {
        return blueprints;
    }

    private static Map<String, Blueprint> validateBlueprints(Map<String, Blueprint> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("the file has no blueprints");
        }
        Map<String, Blueprint> out = new LinkedHashMap<>();
        for (Map.Entry<String, Blueprint> e : raw.entrySet()) {
            if (!e.getKey().matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("blueprint id '" + e.getKey() + "' must be lower-case letters, digits and _");
            }
            out.put(e.getKey(), e.getValue().validate(e.getKey(), TowerKind.ids()));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    private static String report(String file, Exception e) {
        String msg = file + ": " + e.getMessage() + " (kept the previous values)";
        Ldtd.LOGGER.error("Invalid data file {}", msg);
        return msg;
    }

    private static <T> T readUser(String name, Class<T> type) throws IOException {
        try (Reader r = Files.newBufferedReader(new File(dataDir, name).toPath(), StandardCharsets.UTF_8)) {
            T value = GSON.fromJson(r, type);
            if (value == null) {
                throw new JsonParseException("the file is empty");
            }
            return value;
        }
    }

    private static <T> T readDefault(String name, Class<T> type) {
        try (InputStream in = LdtdData.class.getResourceAsStream(DEFAULTS_ROOT + name)) {
            if (in == null) {
                throw new IllegalStateException("default " + name + " is missing from the jar");
            }
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new IllegalStateException("could not read the default " + name, e);
        }
    }

    private static void copyDefaultIfMissing(String name) {
        File target = new File(dataDir, name);
        if (target.exists()) {
            return;
        }
        try (InputStream in = LdtdData.class.getResourceAsStream(DEFAULTS_ROOT + name)) {
            if (in == null) {
                Ldtd.LOGGER.error("Default {} is missing from the jar", name);
                return;
            }
            Files.copy(in, target.toPath());
            Ldtd.LOGGER.info("Wrote default {}", target);
        } catch (IOException e) {
            Ldtd.LOGGER.error("Could not write default {}", target, e);
        }
    }

    /** The shape of {@code hearth_levels.json}. */
    private static final class HearthLevelsFile {
        List<HearthLevel> levels;
    }

    /** The shape of {@code towers.json}. */
    private static final class TowersFile {
        Map<String, TowerStats> towers;
    }

    /** The shape of {@code blueprints.json}. */
    private static final class BlueprintsFile {
        LinkedHashMap<String, Blueprint> blueprints;
    }

    /** The shape of {@code gloam.json}. */
    private static final class GloamFile {
        Map<String, GloamStats> units;
    }
}
