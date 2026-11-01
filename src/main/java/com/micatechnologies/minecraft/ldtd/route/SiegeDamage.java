package com.micatechnologies.minecraft.ldtd.route;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Damage the Gloam have done to blocks, per dimension. The only path by which the Gloam break
 * blocks, so the territory rule (design decision D4) is enforced in exactly one place.
 *
 * <p>A block has {@code max(1, hardness) * }{@value #HP_PER_HARDNESS} hit points. Each hit takes the
 * attacker's dig power. Several Gloam hitting one block add up, and they all see one shared crack
 * animation. Damage on a block nobody has hit for {@value #FORGET_TICKS} ticks heals.</p>
 */
public final class SiegeDamage {

    public static final float HP_PER_HARDNESS = 10.0F;
    public static final int FORGET_TICKS = 600;

    private static final Map<Integer, SiegeDamage> BY_DIMENSION = new HashMap<>();

    private final Map<BlockPos, Crack> cracks = new HashMap<>();
    private int nextBreakerId = -1_000_000;

    public static SiegeDamage get(World world) {
        return BY_DIMENSION.computeIfAbsent(world.provider.getDimension(), d -> new SiegeDamage());
    }

    public static void clearAll() {
        BY_DIMENSION.clear();
    }

    /** Whether the Gloam may damage the block at {@code pos} at all. */
    public static boolean mayBreak(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial().blocksMovement()
            && WorldRouteTerrain.SiegeRules.mayBreak(world, HearthRegistry.get(world), pos, state);
    }

    /**
     * Hits the block at {@code pos} with {@code power}.
     *
     * @return true if the block broke (or was already gone), false if it still stands or may not be
     *     damaged
     */
    public boolean hit(World world, BlockPos pos, float power) {
        IBlockState state = world.getBlockState(pos);
        if (!state.getMaterial().blocksMovement()) {
            forget(world, pos);
            return true;
        }
        if (power <= 0.0F || !WorldRouteTerrain.SiegeRules.mayBreak(world, HearthRegistry.get(world), pos, state)) {
            return false;
        }
        BlockPos key = pos.toImmutable();
        Crack crack = cracks.get(key);
        if (crack == null) {
            crack = new Crack(nextBreakerId--);
            cracks.put(key, crack);
        }
        float hp = Math.max(1.0F, state.getBlockHardness(world, pos)) * HP_PER_HARDNESS;
        crack.damage += power;
        crack.lastHit = world.getTotalWorldTime();
        if (crack.damage >= hp) {
            world.sendBlockBreakProgress(crack.breakerId, key, -1);
            cracks.remove(key);
            world.destroyBlock(key, LdtdConfig.siegeDropsBlocks);
            return true;
        }
        world.sendBlockBreakProgress(crack.breakerId, key, (int) (crack.damage / hp * 10.0F));
        return false;
    }

    /** Heals cracks nobody is working on. Called once a second per dimension. */
    public void tick(World world) {
        long now = world.getTotalWorldTime();
        Iterator<Map.Entry<BlockPos, Crack>> it = cracks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Crack> e = it.next();
            if (now - e.getValue().lastHit > FORGET_TICKS) {
                world.sendBlockBreakProgress(e.getValue().breakerId, e.getKey(), -1);
                it.remove();
            }
        }
    }

    private void forget(World world, BlockPos pos) {
        Crack crack = cracks.remove(pos);
        if (crack != null) {
            world.sendBlockBreakProgress(crack.breakerId, pos, -1);
        }
    }

    private static final class Crack {
        final int breakerId;
        float damage;
        long lastHit;

        Crack(int breakerId) {
            this.breakerId = breakerId;
        }
    }
}
