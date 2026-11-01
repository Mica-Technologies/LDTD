package com.micatechnologies.minecraft.ldtd.tower;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Turns a blueprint palette or cost string ({@code modid:name} or {@code modid:name@meta}) into a
 * block state or an item stack. Results are cached, since towers check their structure often.
 */
public final class BlockSpec {

    private static final Map<String, IBlockState> STATES = new HashMap<>();

    private BlockSpec() {
        throw new AssertionError("No instances.");
    }

    @Nullable
    public static IBlockState state(String spec) {
        return STATES.computeIfAbsent(spec, BlockSpec::parseState);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private static IBlockState parseState(String spec) {
        String[] parts = spec.split("@", 2);
        Block block = Block.getBlockFromName(parts[0]);
        if (block == null) {
            return null;
        }
        int meta = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return block.getStateFromMeta(meta);
    }

    /** The item a block spec costs: the block's own item, with its dropped metadata. */
    public static ItemStack costOfBlock(String spec) {
        IBlockState state = state(spec);
        if (state == null) {
            return ItemStack.EMPTY;
        }
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item == net.minecraft.init.Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, state.getBlock().damageDropped(state));
    }

    /** An item spec ({@code modid:name[@meta]}) as a one-item stack. */
    public static ItemStack item(String spec) {
        String[] parts = spec.split("@", 2);
        Item item = Item.getByNameOrId(parts[0]);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
    }

    /** Forget cached states (after {@code /ldtd reload}). */
    public static void clear() {
        STATES.clear();
    }
}
