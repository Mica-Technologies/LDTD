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
    public static final String CATEGORY_GLOAM = "gloam";
    public static final String CATEGORY_CLIENT = "client";
    public static final String CATEGORY_RANDOM_SPAWN = "randomSpawn";
    public static final String CATEGORY_PROTECTION = "protection";
    public static final String CATEGORY_SPIRES = "spires";

    /** Master switch for raids. Off turns LDTD into a building-only mod. */
    public static boolean raidsEnabled = true;

    /** Whether raids start on their own at nightfall. */
    public static boolean nightRaids = true;

    /** Whether players may ring the Hearth to call a raid early. */
    public static boolean callEarly = true;

    /** Most Gloam alive at once per Hearth during a raid. The rest trickle in as others die. */
    public static int maxAlivePerHearth = 120;

    /** How close the owner must be for a night raid to start (design decision D18). */
    public static int defenderRange = 96;

    /** Share of each stockpile stack lost when a Hearth falls (design decision D17). */
    public static double fallStockpileLoss = 0.25D;

    /** Item that relights a fallen Hearth, by registry name. */
    public static String relightFuel = "minecraft:coal";

    /** How many of {@link #relightFuel} it takes. */
    public static int relightFuelCount = 8;

    /**
     * Whether the Gloam may damage blocks at all. Even when on, they only ever damage blocks
     * inside a Hearth's territory (design decision D4). Off makes walls indestructible to them.
     */
    public static boolean gloamDamageBlocks = true;

    /** Whether blocks the Gloam break drop as items (so walls can be rebuilt from what's left). */
    public static boolean siegeDropsBlocks = true;

    /** Route cost of breaking a block, per point of block hardness, in walking blocks. */
    public static double breachCostPerHardness = 4.0D;

    /** Most states the router expands for one route before giving up. */
    public static int routeNodeBudget = 60000;

    /** Block light level at or above which the Gloam are weakened (design decision D15). */
    public static int gloamLightThreshold = 10;

    /** Damage multiplier the Gloam take while weakened by light. */
    public static double gloamLitDamageMultiplier = 1.5D;

    /** Fraction of speed the Gloam lose while weakened by light. */
    public static double gloamLitSlowdown = 0.15D;

    /** Whether the Gloam smoulder and take damage in daylight under the open sky. */
    public static boolean gloamBurnInDaylight = true;

    /** Whether only the owner (and operators) may break a Hearth. */
    public static boolean hearthOwnerOnlyBreak = true;

    /** Extra gap, in blocks, required between the edges of two territories. */
    public static int territorySpacing = 4;

    /** Whether villages can get a Hearth Square and Watch Posts (Phase 9, D21/D22). */
    public static boolean villageBuildings = true;

    /** Whether each player gets the Hearthkeeper's Guide the first time they join (Phase 10). */
    public static boolean giveGuide = true;

    /** Whether wounded Gloam show a health bar (client, Phase 10). */
    public static boolean healthBars = true;

    /** Whether Dusk Spires generate in new chunks (Phase 9). */
    public static boolean spiresEnabled = true;
    /** One chunk in this many gets a spire (before spacing and distance rules). */
    public static int spireRarity = 300;
    /** No spire within this many blocks of the world spawn. */
    public static int spireMinSpawnDistance = 256;
    /** No two spires closer than this. */
    public static int spireSpacing = 384;
    /** Spires within this many blocks of a Hearth feed its raids. */
    public static int spireInfluence = 384;
    /** Extra raid budget per spire tier within reach. */
    public static int spireBudgetPerTier = 3;
    /** Spires a Hearth's region must lose before the Eclipse comes for it. */
    public static int eclipseAfterSpires = 3;

    /** Master switch for territory protection (Phase 4b). The Gloam's siege is never affected. */
    public static boolean protectionEnabled = true;

    /** Untrusted players can't break or place blocks (or empty buckets, or light fires) in a territory. */
    public static boolean protectBlocks = true;

    /** Untrusted players can't open a Hearth's stockpile. */
    public static boolean protectStockpile = true;

    /** Explosions break no blocks inside a territory. */
    public static boolean protectExplosions = true;

    /** Operators ignore territory protection. */
    public static boolean protectionOpsBypass = true;

    /**
     * Whether blueprints build at once in survival, paid up front, instead of being built block by
     * block by Hearth-sprites. Creative always builds at once.
     */
    public static boolean instantBuild = false;

    /** Whether {@code /randomspawn} ({@code /rs}) is available. */
    public static boolean randomSpawnEnabled = true;

    /** Permission level {@code /randomspawn} needs: 0 = every player, 2 = operators. */
    public static int randomSpawnPermissionLevel = 0;

    /** Seconds a player waits between uses. Operators skip it. */
    public static int randomSpawnCooldownSeconds = 300;

    /**
     * Below this generated radius (blocks from spawn), {@code /randomspawn} may pick ungenerated
     * ground within it. At or above it, only already-generated chunks.
     */
    public static int randomSpawnFallbackRadius = 1000;

    /** Ember sparks along a territory's border when you are near it. */
    public static boolean edgeParticles = true;

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
        defenderRange = c.getInt("defenderRange", CATEGORY_RAIDS, defenderRange, 16, 1024,
            "How close (blocks) a Hearth's owner must be, online, for a night raid to start.");

        fallStockpileLoss = c.getFloat("fallStockpileLoss", CATEGORY_HEARTH, (float) fallStockpileLoss, 0.0F, 1.0F,
            "Share of each stockpile stack lost when a Hearth falls.");
        relightFuel = c.getString("relightFuel", CATEGORY_HEARTH, relightFuel,
            "Item (registry name) that relights a fallen Hearth. Right-click the Hearth holding it.");
        relightFuelCount = c.getInt("relightFuelCount", CATEGORY_HEARTH, relightFuelCount, 1, 64,
            "How many of relightFuel it takes to relight a fallen Hearth.");

        gloamDamageBlocks = c.getBoolean("gloamDamageBlocks", CATEGORY_GRIEFING, gloamDamageBlocks,
            "Whether the Gloam may damage blocks. Even when true, they only damage blocks inside a"
                + " Hearth's territory, never outside one.");
        siegeDropsBlocks = c.getBoolean("siegeDropsBlocks", CATEGORY_GRIEFING, siegeDropsBlocks,
            "Whether blocks the Gloam break drop as items, so walls can be rebuilt from what is left.");
        breachCostPerHardness = c.getFloat("breachCostPerHardness", CATEGORY_GRIEFING, (float) breachCostPerHardness,
            0.1F, 100.0F, "How strongly raids prefer walking around walls to breaking them: the route cost of a"
                + " block, per point of hardness, in blocks walked.");
        routeNodeBudget = c.getInt("routeNodeBudget", CATEGORY_RAIDS, routeNodeBudget, 1000, 1000000,
            "Most steps the raid router considers for one route before giving up. Bounds the server time a"
                + " route can take.");

        gloamLightThreshold = c.getInt("lightThreshold", CATEGORY_GLOAM, gloamLightThreshold, 0, 15,
            "Block light level at or above which the Gloam are weakened.");
        gloamLitDamageMultiplier = c.getFloat("litDamageMultiplier", CATEGORY_GLOAM, (float) gloamLitDamageMultiplier,
            1.0F, 10.0F, "Damage multiplier the Gloam take while weakened by light.");
        gloamLitSlowdown = c.getFloat("litSlowdown", CATEGORY_GLOAM, (float) gloamLitSlowdown, 0.0F, 0.9F,
            "Fraction of their speed the Gloam lose while weakened by light.");
        gloamBurnInDaylight = c.getBoolean("burnInDaylight", CATEGORY_GLOAM, gloamBurnInDaylight,
            "Whether the Gloam smoulder and take damage in daylight under the open sky.");

        hearthOwnerOnlyBreak = c.getBoolean("ownerOnlyBreak", CATEGORY_HEARTH, hearthOwnerOnlyBreak,
            "Whether only a Hearth's owner (and operators) may break it.");
        territorySpacing = c.getInt("territorySpacing", CATEGORY_HEARTH, territorySpacing, 0, 256,
            "Extra gap, in blocks, required between the edges of two territories.");

        giveGuide = c.getBoolean("giveGuide", CATEGORY_HEARTH, giveGuide,
            "Give each player the Hearthkeeper's Guide (a book explaining LDTD) the first time they join."
                + " /hearth guide gives another copy.");
        healthBars = c.getBoolean("healthBars", CATEGORY_CLIENT, healthBars,
            "Show a small health bar over wounded Gloam within 32 blocks.");
        villageBuildings = c.getBoolean("villageBuildings", CATEGORY_HEARTH, villageBuildings,
            "Whether new villages can have a Hearth Square (an unclaimed Hearth with room to grow) and Watch Posts"
                + " (ready-built archer towers). Needs a restart.");
        spiresEnabled = c.getBoolean("enabled", CATEGORY_SPIRES, spiresEnabled,
            "Whether Dusk Spires generate in new chunks. They send raids and can be destroyed.");
        spireRarity = c.getInt("rarity", CATEGORY_SPIRES, spireRarity, 10, 100000,
            "One chunk in this many gets a spire (before the spacing and distance rules).");
        spireMinSpawnDistance = c.getInt("minSpawnDistance", CATEGORY_SPIRES, spireMinSpawnDistance, 0, 100000,
            "No spire within this many blocks of the world spawn.");
        spireSpacing = c.getInt("spacing", CATEGORY_SPIRES, spireSpacing, 32, 100000,
            "No two spires closer than this many blocks.");
        spireInfluence = c.getInt("influence", CATEGORY_SPIRES, spireInfluence, 0, 100000,
            "Spires within this many blocks of a Hearth make its raids bigger and send them from their side.");
        spireBudgetPerTier = c.getInt("budgetPerTier", CATEGORY_SPIRES, spireBudgetPerTier, 0, 1000,
            "Extra raid budget per spire tier within reach of a Hearth.");
        eclipseAfterSpires = c.getInt("eclipseAfterSpires", CATEGORY_SPIRES, eclipseAfterSpires, 1, 1000,
            "Spires destroyed near a Hearth before the Eclipse comes for it in every raid until defeated.");

        protectionEnabled = c.getBoolean("enabled", CATEGORY_PROTECTION, protectionEnabled,
            "Master switch for territory protection. The Gloam's siege is never affected by it.");
        protectBlocks = c.getBoolean("blocks", CATEGORY_PROTECTION, protectBlocks,
            "Untrusted players can't break or place blocks, empty buckets or light fires in a territory.");
        protectStockpile = c.getBoolean("stockpile", CATEGORY_PROTECTION, protectStockpile,
            "Untrusted players can't open a Hearth's stockpile.");
        protectExplosions = c.getBoolean("explosions", CATEGORY_PROTECTION, protectExplosions,
            "Explosions (creepers, TNT, ...) break no blocks inside a territory. They still hurt entities.");
        protectionOpsBypass = c.getBoolean("opsBypass", CATEGORY_PROTECTION, protectionOpsBypass,
            "Operators ignore territory protection.");

        instantBuild = c.getBoolean("instantBuild", CATEGORY_HEARTH, instantBuild,
            "Whether blueprints build at once in survival (paid up front) instead of Hearth-sprites building"
                + " them block by block. Creative always builds at once.");

        randomSpawnEnabled = c.getBoolean("enabled", CATEGORY_RANDOM_SPAWN, randomSpawnEnabled,
            "Whether /randomspawn (/rs) is available.");
        randomSpawnPermissionLevel = c.getInt("permissionLevel", CATEGORY_RANDOM_SPAWN, randomSpawnPermissionLevel, 0, 4,
            "Permission level /randomspawn needs: 0 = every player, 2 = operators.");
        randomSpawnCooldownSeconds = c.getInt("cooldownSeconds", CATEGORY_RANDOM_SPAWN, randomSpawnCooldownSeconds, 0,
            86400, "Seconds a player waits between uses. Operators skip the cooldown.");
        randomSpawnFallbackRadius = c.getInt("fallbackRadius", CATEGORY_RANDOM_SPAWN, randomSpawnFallbackRadius, 64,
            100000, "If the generated world reaches less than this many blocks from spawn, /randomspawn may pick"
                + " ungenerated ground within this radius. Otherwise it only picks already-generated chunks.");

        edgeParticles = c.getBoolean("edgeParticles", CATEGORY_CLIENT, edgeParticles,
            "Ember sparks along a territory's border when you are near it.");
        showTerritoryBorders = c.getString("showTerritoryBorders", CATEGORY_CLIENT, showTerritoryBorders,
            "When to draw territory borders: holding (while holding a Hearth), always, or never.",
            new String[] {"holding", "always", "never"});

        if (c.hasChanged()) {
            c.save();
        }
    }
}
