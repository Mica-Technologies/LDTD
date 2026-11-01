package com.micatechnologies.minecraft.ldtd.client;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Item model bindings. Client only: {@code value = Side.CLIENT} keeps a server from loading it. */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE, value = Side.CLIENT)
public final class ClientModels {

    private ClientModels() {
        throw new AssertionError("No instances.");
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        bind(LdtdRegistry.ITEM_HEARTH);
        // One model for every blueprint; the NBT only says which structure it builds.
        bind(LdtdRegistry.ITEM_BLUEPRINT);
    }

    private static void bind(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
