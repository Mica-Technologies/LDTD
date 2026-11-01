package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.hearth.BlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.ItemBlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Every block, item and tile entity LDTD registers. Registry names are part of saved worlds, so
 * never rename one without a remapping (MissingMappings) handler.
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class LdtdRegistry {

    public static final BlockHearth BLOCK_HEARTH = new BlockHearth();
    public static final ItemBlockHearth ITEM_HEARTH = new ItemBlockHearth(BLOCK_HEARTH);

    private LdtdRegistry() {
        throw new AssertionError("No instances.");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BLOCK_HEARTH);
        GameRegistry.registerTileEntity(TileEntityHearth.class,
            new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "hearth"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ITEM_HEARTH);
    }
}
