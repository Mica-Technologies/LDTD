package com.micatechnologies.minecraft.ldtd;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/**
 * Forge {@link Configuration}-backed settings ({@code config/ldtd.cfg}), loaded once in preInit.
 *
 * <p>Values are read into static fields at load time, never queried per tick. Gameplay content
 * (levels, towers, enemies, waves) is not here. It lives in JSON under {@code config/ldtd/}, see
 * {@link com.micatechnologies.minecraft.ldtd.data.LdtdData}. This file holds switches and limits a
 * server owner sets once.</p>
 *
 * <p><b>Server authority.</b> Only the {@code client} category is read on the client for its own
 * use. Everything else is the server's truth.</p>
 */
public final class LdtdConfig {

    public static final String CATEGORY_RAIDS = "raids";
    public static final String CATEGORY_GRIEFING = "griefing";
    public static final String CATEGORY_HEARTH = "hearth";
    public static final String CATEGORY_CLIENT = "client";

    /** Master switch for raids. Off turns LDTD into a building-only mod. */
    public static boolean raidsEnabled = true;

    /** Whether raids start on their own at nightfall. */
    public static boolean nightRaids = true;

    /** Whether players may ring the Hearth to call a raid early. */
    public static boolean callEarly = true;

    /** Most Gloam alive at once per Hearth during a raid. The rest trickle in as others die. */
    public static int maxAlivePerHearth = 120;

    /**
     * Whether the Gloam may damage blocks at all. Even when on, they only ever damage blocks
     * inside a Hearth's territory (design decision D4). Off makes walls indestructible to them.
     */
    public static boolean gloamDamageBlocks = true;

    /** Whether only the owner (and operators) may break a Hearth. */
    public static boolean hearthOwnerOnlyBreak = true;

    /** Extra gap, in blocks, required between the edges of two territories. */
    public static int territorySpacing = 4;

    /** When to draw territory borders: {@code holding} (holding a Hearth), {@code always}, {@code never}. */
    public static String showTerritoryBorders = "holding";

    private LdtdConfig() {
        throw new AssertionError("No instances.");
    }

    public static void init(File file) {
        Configuration c = new Configuration(file);
        c.load();

        raidsEnabled = c.getBoolean("enabled", CATEGORY_RAIDS, raidsEnabled,
            "Master switch for raids. False makes LDTD building-only.");
        nightRaids = c.getBoolean("nightRaids", CATEGORY_RAIDS, nightRaids,
            "Whether raids start on their own at nightfall.");
        callEarly = c.getBoolean("callEarly", CATEGORY_RAIDS, callEarly,
            "Whether players may ring the Hearth to call a raid early, for a bonus.");
        maxAlivePerHearth = c.getInt("maxAlivePerHearth", CATEGORY_RAIDS, maxAlivePerHearth, 10, 500,
            "Most Gloam alive at once per Hearth during a raid. The rest trickle in as others die.");

        gloamDamageBlocks = c.getBoolean("gloamDamageBlocks", CATEGORY_GRIEFING, gloamDamageBlocks,
            "Whether the Gloam may damage blocks. Even when true, they only damage blocks inside a"
                + " Hearth's territory, never outside one.");

        hearthOwnerOnlyBreak = c.getBoolean("ownerOnlyBreak", CATEGORY_HEARTH, hearthOwnerOnlyBreak,
            "Whether only a Hearth's owner (and operators) may break it.");
        territorySpacing = c.getInt("territorySpacing", CATEGORY_HEARTH, territorySpacing, 0, 256,
            "Extra gap, in blocks, required between the edges of two territories.");

        showTerritoryBorders = c.getString("showTerritoryBorders", CATEGORY_CLIENT, showTerritoryBorders,
            "When to draw territory borders: holding (while holding a Hearth), always, or never.",
            new String[] {"holding", "always", "never"});

        if (c.hasChanged()) {
            c.save();
        }
    }
}
