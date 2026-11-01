package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdTab;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
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
        if (player.isSneaking()) {
            player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.info",
                hearth.ownerName().isEmpty() ? "-" : hearth.ownerName(), hearth.level(), hearth.radius()), true);
        } else {
            player.displayGUIChest(hearth.stockpile());
        }
        return true;
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
