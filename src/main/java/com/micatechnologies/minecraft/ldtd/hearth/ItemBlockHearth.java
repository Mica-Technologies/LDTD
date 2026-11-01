package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Places a Hearth, refusing a spot that would crowd an existing territory, and claims it for the
 * player who placed it.
 */
public class ItemBlockHearth extends ItemBlock {

    public ItemBlockHearth(BlockHearth block) {
        super(block);
        setRegistryName(block.getRegistryName());
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                float hitX, float hitY, float hitZ, IBlockState newState) {
        if (!world.isRemote) {
            HearthRecord conflict = HearthRegistry.get(world).conflictFor(pos);
            if (conflict != null) {
                player.sendStatusMessage(new TextComponentTranslation("ldtd.hearth.too_close",
                    conflict.ownerName.isEmpty() ? "-" : conflict.ownerName,
                    conflict.pos.getX(), conflict.pos.getY(), conflict.pos.getZ()), true);
                return false;
            }
        }
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityHearth) {
                ((TileEntityHearth) te).claim(player);
            }
        }
        return true;
    }
}
