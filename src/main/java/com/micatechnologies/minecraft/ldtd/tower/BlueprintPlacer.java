package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.HearthStockpile;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Builds a blueprint at once, paying from the territory's Hearth stockpile (design decision D5).
 * From Phase 6, Hearth-sprites take this over and build block by block. This instant path stays
 * for creative players.
 *
 * <p>Everything is checked before anything changes: the spot is inside a lit territory the player
 * may build in, every cell is clear, and the stockpile holds the whole cost. Then it takes the
 * cost and places every block in one go.</p>
 */
public final class BlueprintPlacer {

    private BlueprintPlacer() {
        throw new AssertionError("No instances.");
    }

    /** Whether {@code pos} can take a blueprint block: air, grass, snow layers, flowers. */
    public static boolean clear(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    /**
     * Tries to build.
     *
     * @return the message to show the player (success or why not)
     */
    public static ITextComponent place(World world, EntityPlayer player, String id, BlockPos anchor, int rotation) {
        Blueprint bp = LdtdData.blueprints().get(id);
        if (bp == null) {
            return new TextComponentTranslation("ldtd.blueprint.unknown", id);
        }
        HearthRegistry registry = HearthRegistry.get(world);
        HearthRecord territory = registry.territoryAt(anchor.getX(), anchor.getZ());
        if (territory == null) {
            return new TextComponentTranslation("ldtd.blueprint.outside");
        }
        if (!territory.mayBuild(player.getUniqueID()) && !player.canUseCommand(2, "")) {
            return new TextComponentTranslation("ldtd.protect.blocks", territory.ownerName);
        }
        List<Blueprint.Cell> cells = bp.cells(rotation);
        for (Blueprint.Cell c : cells) {
            BlockPos p = anchor.add(c.dx, c.dy, c.dz);
            HearthRecord here = registry.territoryAt(p.getX(), p.getZ());
            if (here == null || !here.pos.equals(territory.pos)) {
                return new TextComponentTranslation("ldtd.blueprint.outside");
            }
            if (!clear(world, p)) {
                return new TextComponentTranslation("ldtd.blueprint.blocked", p.getX(), p.getY(), p.getZ());
            }
            // Never build into a player or mob: they would be stuck inside the blocks.
            if (c.key != Blueprint.AIR
                && !world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(p)).isEmpty()) {
                return new TextComponentTranslation("ldtd.blueprint.occupied", p.getX(), p.getY(), p.getZ());
            }
        }

        if (!player.capabilities.isCreativeMode) {
            TileEntity te = world.getTileEntity(territory.pos);
            if (!(te instanceof TileEntityHearth)) {
                return new TextComponentTranslation("commands.ldtd.hearth.not_loaded");
            }
            HearthStockpile stockpile = ((TileEntityHearth) te).stockpile();
            List<ItemStack> cost = cost(bp);
            ITextComponent missing = missing(stockpile, cost);
            if (missing != null) {
                return new TextComponentTranslation("ldtd.blueprint.missing", missing);
            }
            for (ItemStack need : cost) {
                take(stockpile, need);
            }
        }

        TowerKind kind = bp.tower == null ? null : TowerKind.byId(bp.tower);
        BlockPos core = null;
        for (Blueprint.Cell c : cells) {
            BlockPos p = anchor.add(c.dx, c.dy, c.dz);
            if (c.key == Blueprint.AIR) {
                world.setBlockToAir(p);
            } else if (c.key == Blueprint.CORE) {
                world.setBlockState(p, LdtdRegistry.BLOCK_TOWER_CORE.getDefaultState()
                    .withProperty(BlockTowerCore.KIND, kind == null ? TowerKind.ARCHER : kind));
                core = p;
            } else {
                IBlockState state = BlockSpec.state(bp.palette.get(String.valueOf(c.key)));
                if (state != null) {
                    world.setBlockState(p, state);
                }
            }
        }
        if (core != null) {
            TileEntity te = world.getTileEntity(core);
            if (te instanceof TileEntityTower) {
                ((TileEntityTower) te).configure(id, anchor, rotation);
            }
        }
        world.playSound(null, anchor, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 0.8F);
        return new TextComponentTranslation("ldtd.blueprint.built", new TextComponentTranslation(nameKey(id)));
    }

    /** The lang key for a blueprint's name. */
    public static String nameKey(String id) {
        return "ldtd.blueprint." + id;
    }

    /** What a blueprint costs: one item per block it places, plus its extra cost. Merged by item. */
    public static List<ItemStack> cost(Blueprint bp) {
        List<ItemStack> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bp.blockCounts().entrySet()) {
            add(out, BlockSpec.costOfBlock(e.getKey()), e.getValue());
        }
        for (Map.Entry<String, Integer> e : bp.extraCost.entrySet()) {
            add(out, BlockSpec.item(e.getKey()), e.getValue());
        }
        return out;
    }

    private static void add(List<ItemStack> out, ItemStack one, int count) {
        if (one.isEmpty() || count <= 0) {
            return;
        }
        for (ItemStack s : out) {
            if (ItemStack.areItemsEqual(s, one)) {
                s.grow(count);
                return;
            }
        }
        ItemStack copy = one.copy();
        copy.setCount(count);
        out.add(copy);
    }

    /** What the stockpile lacks, as "3 Stone Bricks, 16 Arrow", or null if it has it all. */
    @Nullable
    private static ITextComponent missing(HearthStockpile stockpile, List<ItemStack> cost) {
        ITextComponent out = null;
        for (ItemStack need : cost) {
            int have = 0;
            for (int i = 0; i < stockpile.getSizeInventory(); i++) {
                ItemStack s = stockpile.getStackInSlot(i);
                if (ItemStack.areItemsEqual(s, need)) {
                    have += s.getCount();
                }
            }
            if (have < need.getCount()) {
                if (out == null) {
                    out = new TextComponentString("");
                } else {
                    out.appendText(", ");
                }
                out.appendText((need.getCount() - have) + " " + need.getDisplayName());
            }
        }
        return out;
    }

    private static void take(HearthStockpile stockpile, ItemStack need) {
        int left = need.getCount();
        for (int i = 0; i < stockpile.getSizeInventory() && left > 0; i++) {
            ItemStack s = stockpile.getStackInSlot(i);
            if (ItemStack.areItemsEqual(s, need)) {
                int n = Math.min(left, s.getCount());
                stockpile.decrStackSize(i, n);
                left -= n;
            }
        }
    }
}
