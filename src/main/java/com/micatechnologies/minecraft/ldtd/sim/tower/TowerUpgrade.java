package com.micatechnologies.minecraft.ldtd.sim.tower;

/**
 * Tower tiers (Phase 7): what each tier costs in Duskglass and how it scales a tower's stats.
 * Loaded from the {@code upgrade} block of {@code towers.json}. Minecraft-free.
 *
 * <p>Index 0 is tier I (as built), 1 is tier II, 2 is tier III. {@code cost[i]} is the Duskglass to
 * go from tier i+1 to tier i+2.</p>
 */
public final class TowerUpgrade {

    public static final int MAX_TIER = 3;

    /** Duskglass to reach tier II, then tier III. */
    public int[] cost = {10, 25};

    /** Damage multiplier per tier. */
    public double[] damage = {1.0D, 1.5D, 2.2D};

    /** Range multiplier per tier. */
    public double[] range = {1.0D, 1.15D, 1.3D};

    /** Cooldown multiplier per tier (lower fires faster). */
    public double[] cooldown = {1.0D, 0.85D, 0.7D};

    /**
     * @throws IllegalArgumentException with a human-readable reason
     */
    public TowerUpgrade validate() {
        if (cost == null || cost.length != MAX_TIER - 1) {
            throw new IllegalArgumentException("upgrade.cost needs " + (MAX_TIER - 1) + " entries");
        }
        for (int c : cost) {
            if (c < 0 || c > 10000) {
                throw new IllegalArgumentException("upgrade.cost entries must be 0-10000");
            }
        }
        check("damage", damage, 0.0D, 100.0D);
        check("range", range, 0.1D, 4.0D);
        check("cooldown", cooldown, 0.05D, 10.0D);
        return this;
    }

    private static void check(String name, double[] values, double min, double max) {
        if (values == null || values.length != MAX_TIER) {
            throw new IllegalArgumentException("upgrade." + name + " needs " + MAX_TIER + " entries");
        }
        for (double v : values) {
            if (Double.isNaN(v) || v < min || v > max) {
                throw new IllegalArgumentException("upgrade." + name + " has " + v + ", outside " + min + ".." + max);
            }
        }
    }

    private static int index(int tier) {
        return Math.max(1, Math.min(tier, MAX_TIER)) - 1;
    }

    public double damage(int tier) {
        return damage[index(tier)];
    }

    public double range(int tier) {
        return range[index(tier)];
    }

    public double cooldown(int tier) {
        return cooldown[index(tier)];
    }

    /** Duskglass to go from {@code tier} to the next, or -1 at the top tier. */
    public int costFrom(int tier) {
        return tier >= MAX_TIER ? -1 : cost[index(tier)];
    }

    /** Total Duskglass spent to reach {@code tier}. */
    public int spentTo(int tier) {
        int total = 0;
        for (int t = 1; t < Math.min(tier, MAX_TIER); t++) {
            total += cost[t - 1];
        }
        return total;
    }
}
