package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdTab;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A blueprint: a reusable plan for one structure from {@code blueprints.json}, named in the stack's
 * NBT. Right-click the ground inside your territory to build it there, facing the way you face.
 * While you hold it, a ghost of the structure shows where it would go (client side).
 */
public class ItemBlueprint extends Item {

    public static final String TAG = "blueprint";

    public ItemBlueprint() {
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "blueprint");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".blueprint");
        setCreativeTab(LdtdTab.TAB);
        setMaxStackSize(1);
        setHasSubtypes(true);
    }

    /** A blueprint stack for {@code id}. */
    public static ItemStack of(Item item, String id) {
        ItemStack stack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TAG, id);
        stack.setTagCompound(tag);
        return stack;
    }

    /** The blueprint id a stack holds, or "" if none. */
    public static String id(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getString(TAG) : "";
    }

    /** Where a click on {@code pos}/{@code side} anchors a structure: the clicked block if it can be replaced. */
    public static BlockPos anchor(World world, BlockPos pos, EnumFacing side) {
        return BlueprintPlacer.clear(world, pos) ? pos : pos.offset(side);
    }

    /** Which way a structure turns: the way the player faces (0 = south, then clockwise). */
    public static int rotation(EntityPlayer player) {
        return player.getHorizontalFacing().getHorizontalIndex();
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
                                      float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            String id = id(player.getHeldItem(hand));
            player.sendStatusMessage(BlueprintPlacer.place(world, player, id, anchor(world, pos, side), rotation(player)),
                true);
        }
        return EnumActionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String id = id(stack);
        String key = BlueprintPlacer.nameKey(id);
        String name = I18n.canTranslate(key) ? I18n.translateToLocal(key) : id;
        return I18n.translateToLocalFormatted("item.ldtd.blueprint.named", name);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (java.util.Map.Entry<String, com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint> e
                : LdtdData.blueprints().entrySet()) {
                if (!e.getValue().internal) {
                    items.add(of(this, e.getKey()));
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint bp = LdtdData.blueprints().get(id(stack));
        if (bp == null) {
            return;
        }
        tooltip.add(net.minecraft.client.resources.I18n.format("item.ldtd.blueprint.size", bp.width(), bp.depth(),
            bp.height()));
        tooltip.add(net.minecraft.client.resources.I18n.format("item.ldtd.blueprint.cost"));
        for (ItemStack s : BlueprintPlacer.cost(bp)) {
            tooltip.add("  " + s.getCount() + " " + s.getDisplayName());
        }
    }
}
