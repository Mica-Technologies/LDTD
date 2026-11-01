package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Duskglass: the shadow a Gloam leaves when it is destroyed, hardened into glass. The raid currency
 * (design decision D5, Phase 7). Spent at a Hearth to raise its level, and at a tower's core to
 * raise its tier.
 */
public class ItemDuskglass extends Item {

    public ItemDuskglass() {
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "duskglass");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".duskglass");
        setCreativeTab(LdtdTab.TAB);
    }

    /**
     * Vanilla skips a block's right-click when the player sneaks holding an item. Upgrading a tower
     * is a sneak-use with Duskglass in hand, so the block must still get the click.
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }
}
