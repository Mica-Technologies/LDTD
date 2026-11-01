package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.gloam.EntityShade;
import com.micatechnologies.minecraft.ldtd.gloam.EntitySmother;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.BlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.ItemBlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
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

    /**
     * Entity network ids are allocated in this order and must stay stable across versions, so only
     * ever append. Spawn egg colours are the Gloam palette (docs/design/ART_DIRECTION.md).
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        int id = 0;
        event.getRegistry().register(gloam(EntityShade.class, GloamKind.SHADE, id++, 0x141019, 0x5CF2E6));
        event.getRegistry().register(gloam(EntitySmother.class, GloamKind.SMOTHER, id++, 0x4B2E6F, 0xE9E4D8));
    }

    private static EntityEntry gloam(Class<? extends EntityGloam> type, GloamKind kind, int id, int egg, int spots) {
        return EntityEntryBuilder.create()
            .entity(type)
            .id(new ResourceLocation(LdtdConstants.MOD_NAMESPACE, kind.registryPath()), id)
            .name(LdtdConstants.MOD_NAMESPACE + "." + kind.registryPath())
            .tracker(80, 3, true)
            .egg(egg, spots)
            .build();
    }
}
