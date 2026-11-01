package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.LdtdTab;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import com.micatechnologies.minecraft.ldtd.raid.RaidDirector;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The Hearth: the block that claims a territory and holds its stockpile.
 *
 * <p>It glows at full light level on purpose. Light is what the Gloam hunt and what weakens them
 * (design decision D15), so the Hearth is both the goal of every raid and the brightest thing in
 * its territory.</p>
 */
public class BlockHearth extends Block {

    public BlockHearth() {
        super(Material.ROCK);
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "hearth");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".hearth");
        setCreativeTab(LdtdTab.TAB);
        setHardness(5.0F);
        // Blast-proof: a creeper or a TNT trap must not end a territory.
        setResistance(6000.0F);
        setLightLevel(1.0F);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityHearth();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityHearth)) {
            return true;
        }
        TileEntityHearth hearth = (TileEntityHearth) te;
        ItemStack held = player.getHeldItem(hand);
        if (hearth.isDormant() && isFuel(held)) {
            relight(world, pos, player, hearth, held);
        } else if (!hearth.isDormant() && held.getItem() == LdtdRegistry.ITEM_DUSKGLASS) {
            // Feed the Hearth to raise its level (Phase 7). Owner and trusted players only.
            if (!hearth.mayUse(player) && !player.canUseCommand(2, "")) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.protect.stockpile", hearth.ownerName()), true);
            } else {
                player.sendStatusMessage(hearth.levelUp(player, held), true);
            }
        } else if (player.isSneaking() && held.isEmpty()) {
            sneakUse(world, player, hearth);
        } else {
            if (hearth.isDormant()) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.cold", LdtdConfig.relightFuelCount,
                    fuelName()), true);
            }
            if (LdtdConfig.protectionEnabled && LdtdConfig.protectStockpile && !hearth.mayUse(player)
                && !(LdtdConfig.protectionOpsBypass && player.canUseCommand(2, ""))) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.protect.stockpile", hearth.ownerName()),
                    true);
                return true;
            }
            player.displayGUIChest(hearth.stockpile());
        }
        return true;
    }

    /** How long a first sneak-use waits for the second that calls the raid. */
    private static final long CALL_CONFIRM_TICKS = 100L;

    /**
     * Sneak-use with an empty hand: shows the Hearth's state. Doing it again within five seconds
     * calls the next raid early (design decision D7), so a raid is never called by accident.
     */
    private static void sneakUse(World world, EntityPlayer player, TileEntityHearth hearth) {
        if (hearth.owner() == null && !hearth.isDormant()) {
            // An unclaimed Hearth (a village's): sneak-use takes it up (D21).
            hearth.claimFor(player);
            player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.claimed"), true);
            return;
        }
        long now = world.getTotalWorldTime();
        if (hearth.isOwner(player) && hearth.confirmCall(player.getUniqueID(), now, CALL_CONFIRM_TICKS)) {
            String refusal = RaidDirector.call(world, hearth);
            if (refusal != null) {
                player.sendStatusMessage(new TextComponentTranslation(refusal), true);
            }
            return;
        }
        player.sendStatusMessage(new TextComponentTranslation(hearth.isOwner(player) ? "ldtd.hearth.info_call"
                : "ldtd.hearth.info", hearth.ownerName().isEmpty() ? "-" : hearth.ownerName(), hearth.level(),
            hearth.radius(), Math.round(hearth.flameFraction() * 100.0F), hearth.raidsSurvived()), true);
        BuildQueue q = hearth.queue();
        if (!q.isEmpty()) {
            player.sendMessage(new TextComponentTranslation(q.waitingFor().isEmpty() ? "ldtd.hearth.builders"
                : "ldtd.hearth.builders_waiting", q.jobsLeft(), q.repairsLeft(), q.waitingFor()));
        }
    }

    private static boolean isFuel(ItemStack stack) {
        Item fuel = Item.getByNameOrId(LdtdConfig.relightFuel);
        return fuel != null && !stack.isEmpty() && stack.getItem() == fuel;
    }

    private static String fuelName() {
        Item fuel = Item.getByNameOrId(LdtdConfig.relightFuel);
        return fuel == null ? LdtdConfig.relightFuel : new ItemStack(fuel).getDisplayName();
    }

    private static void relight(World world, BlockPos pos, EntityPlayer player, TileEntityHearth hearth,
                                ItemStack held) {
        int need = LdtdConfig.relightFuelCount;
        if (!player.capabilities.isCreativeMode) {
            if (held.getCount() < need) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.cold", need, fuelName()), true);
                return;
            }
            held.shrink(need);
        }
        hearth.relight();
        world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F, 0.8F);
        player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.relit"), true);
    }

    /** Whether {@code player} may break the Hearth at {@code pos} under the owner-only rule. */
    static boolean mayBreak(World world, BlockPos pos, EntityPlayer player) {
        if (!LdtdConfig.hearthOwnerOnlyBreak) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        return !(te instanceof TileEntityHearth)
            || ((TileEntityHearth) te).isOwner(player)
            || player.canUseCommand(2, "");
    }

    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        return mayBreak(world, pos, player) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (!mayBreak(world, pos, player)) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.not_owner"), true);
            }
            return false;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        // Withers and the dragon don't get to delete a territory either.
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityHearth) {
                InventoryHelper.dropInventoryItems(world, pos, ((TileEntityHearth) te).stockpile());
            }
            HearthRegistry.get(world).remove(pos);
        }
        super.breakBlock(world, pos, state);
    }
}
