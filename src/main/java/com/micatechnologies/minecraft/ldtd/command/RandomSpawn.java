package com.micatechnologies.minecraft.ldtd.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * Picks a random, safe place to stand for {@code /randomspawn}.
 *
 * <p>Where it looks depends on how much of the world exists. If the generated area reaches
 * {@code fallbackRadius} blocks from spawn or more, it only picks chunks that are already
 * generated, read from the dimension's region files, so a busy server never generates terrain for
 * a teleport. If the generated area is smaller (a new world), it picks anywhere within
 * {@code fallbackRadius} of spawn and lets the terrain generate.</p>
 */
final class RandomSpawn {

    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final int ATTEMPTS = 40;

    private RandomSpawn() {
    }

    /** A safe feet position, or null if none was found in a reasonable number of tries. */
    @Nullable
    static BlockPos find(WorldServer world, int fallbackRadius, Random random) {
        BlockPos spawn = world.getSpawnPoint();
        List<int[]> regions = regions(world);
        boolean generatedOnly = extent(regions, spawn) >= fallbackRadius;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x;
            int z;
            if (generatedOnly) {
                int[] r = regions.get(random.nextInt(regions.size()));
                int cx = r[0] * 32 + random.nextInt(32);
                int cz = r[1] * 32 + random.nextInt(32);
                if (!world.getChunkProvider().isChunkGeneratedAt(cx, cz)) {
                    continue;
                }
                x = cx * 16 + random.nextInt(16);
                z = cz * 16 + random.nextInt(16);
            } else {
                // Uniform over the disc, not bunched at the centre.
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double dist = Math.sqrt(random.nextDouble()) * fallbackRadius;
                x = spawn.getX() + (int) Math.round(Math.cos(angle) * dist);
                z = spawn.getZ() + (int) Math.round(Math.sin(angle) * dist);
            }
            BlockPos spot = safeSpot(world, x, z);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos safeSpot(WorldServer world, int x, int z) {
        BlockPos column = new BlockPos(x, 64, z);
        if (!world.getWorldBorder().contains(column)) {
            return null;
        }
        BlockPos top = world.getTopSolidOrLiquidBlock(column);
        if (top.getY() <= 1 || top.getY() >= world.getHeight() - 2) {
            return null;
        }
        if (world.getBlockState(top.down()).getMaterial().isLiquid()
            || !world.getBlockState(top.down()).getMaterial().isSolid()
            || world.getBlockState(top).getMaterial().isLiquid()
            || world.getBlockState(top.up()).getMaterial().blocksMovement()) {
            return null;
        }
        return top;
    }

    /** The region files of this dimension, as region coordinates. */
    private static List<int[]> regions(WorldServer world) {
        List<int[]> out = new ArrayList<>();
        File dir = world.getChunkSaveLocation();
        String[] names = dir == null ? null : new File(dir, "region").list();
        if (names == null) {
            return out;
        }
        for (String name : names) {
            Matcher m = REGION.matcher(name);
            if (m.matches()) {
                out.add(new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))});
            }
        }
        return out;
    }

    /** How far from spawn the generated world reaches: the farthest region corner. */
    static double extent(List<int[]> regions, BlockPos spawn) {
        double max = 0.0D;
        for (int[] r : regions) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    double x = (r[0] + dx) * 512.0D - spawn.getX();
                    double z = (r[1] + dz) * 512.0D - spawn.getZ();
                    max = Math.max(max, Math.sqrt(x * x + z * z));
                }
            }
        }
        return max;
    }
}
