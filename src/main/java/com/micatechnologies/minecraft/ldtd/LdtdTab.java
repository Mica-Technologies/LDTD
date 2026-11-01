package com.micatechnologies.minecraft.ldtd;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/** LDTD's creative tab. */
public final class LdtdTab {

    public static final CreativeTabs TAB = new CreativeTabs(LdtdConstants.MOD_NAMESPACE) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(LdtdRegistry.ITEM_HEARTH);
        }
    };

    private LdtdTab() {
        throw new AssertionError("No instances.");
    }
}
