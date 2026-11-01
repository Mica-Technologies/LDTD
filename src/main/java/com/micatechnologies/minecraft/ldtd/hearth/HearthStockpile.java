package com.micatechnologies.minecraft.ldtd.hearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * The Hearth's material store: what Hearth-sprites build with (design decision D5).
 *
 * <p>An {@link InventoryBasic}, so the vanilla chest screen can show it through
 * {@code EntityPlayer#displayGUIChest}, the same way the ender chest does. That means no custom
 * container or GUI class and no GUI texture of our own.</p>
 */
public final class HearthStockpile extends InventoryBasic {

    public static final int SIZE = 27;

    private final TileEntityHearth hearth;

    HearthStockpile(TileEntityHearth hearth) {
        // Not a custom name, so the title is treated as a translation key.
        super("container.ldtd.stockpile", false, SIZE);
        this.hearth = hearth;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return !hearth.isInvalid()
            && hearth.getWorld().getTileEntity(hearth.getPos()) == hearth
            && player.getDistanceSq(hearth.getPos().getX() + 0.5D, hearth.getPos().getY() + 0.5D,
                hearth.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        hearth.markDirty();
    }

    NBTTagList write() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                stack.writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        return list;
    }

    void read(NBTTagList list) {
        clear();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot < getSizeInventory()) {
                setInventorySlotContents(slot, new ItemStack(tag));
            }
        }
    }
}
