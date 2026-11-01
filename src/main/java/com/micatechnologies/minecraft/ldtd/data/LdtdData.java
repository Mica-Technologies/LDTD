package com.micatechnologies.minecraft.ldtd.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.micatechnologies.minecraft.ldtd.Ldtd;
import com.micatechnologies.minecraft.ldtd.sim.HearthLevel;
import com.micatechnologies.minecraft.ldtd.sim.HearthLevelTable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * LDTD's data-driven content (design decision D11).
 *
 * <p>Content lives as JSON in {@code config/ldtd/}. The defaults ship in the jar under
 * {@code /ldtd/defaults/} and are copied out the first time, so a server owner always has a file to
 * edit. Each file is validated as it is read. A broken file is reported in the log and the last good
 * table (or, on first load, the jar's default) stays in force, so a typo can't take a server down.
 * {@code /ldtd reload} re-reads everything.</p>
 */
public final class LdtdData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULTS_ROOT = "/ldtd/defaults/";
    private static final String HEARTH_LEVELS = "hearth_levels.json";

    private static File dataDir;
    private static volatile HearthLevelTable hearthLevels;

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
        hearthLevels = readDefaultHearthLevels();
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
            hearthLevels = HearthLevelTable.validate(readHearthLevels(new File(dataDir, HEARTH_LEVELS)));
        } catch (IOException | JsonParseException | IllegalArgumentException e) {
            String msg = HEARTH_LEVELS + ": " + e.getMessage() + " (kept the previous table)";
            Ldtd.LOGGER.error("Invalid data file {}", msg);
            problems.add(msg);
        }
        return problems;
    }

    public static HearthLevelTable hearthLevels() {
        return hearthLevels;
    }

    private static List<HearthLevel> readHearthLevels(File file) throws IOException {
        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return parseHearthLevels(r);
        }
    }

    private static HearthLevelTable readDefaultHearthLevels() {
        try (InputStream in = LdtdData.class.getResourceAsStream(DEFAULTS_ROOT + HEARTH_LEVELS)) {
            if (in == null) {
                throw new IllegalStateException("default " + HEARTH_LEVELS + " is missing from the jar");
            }
            return HearthLevelTable.validate(parseHearthLevels(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("could not read the default " + HEARTH_LEVELS, e);
        }
    }

    private static List<HearthLevel> parseHearthLevels(Reader r) {
        Type type = new TypeToken<HearthLevelsFile>() { }.getType();
        HearthLevelsFile f = GSON.fromJson(r, type);
        if (f == null) {
            throw new JsonParseException("the file is empty");
        }
        return f.levels;
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
}
