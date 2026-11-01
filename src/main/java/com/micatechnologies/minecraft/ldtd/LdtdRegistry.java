package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.gloam.EntityDarkBolt;
import com.micatechnologies.minecraft.ldtd.gloam.EntityDimcaster;
import com.micatechnologies.minecraft.ldtd.gloam.EntityFlicker;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.gloam.EntityMothWraith;
import com.micatechnologies.minecraft.ldtd.gloam.EntityShade;
import com.micatechnologies.minecraft.ldtd.gloam.EntitySmother;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.BlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.EntityHearthSprite;
import com.micatechnologies.minecraft.ldtd.hearth.ItemDuskglass;
import com.micatechnologies.minecraft.ldtd.hearth.ItemBlockHearth;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.micatechnologies.minecraft.ldtd.tower.BlockTowerCore;
import com.micatechnologies.minecraft.ldtd.tower.EntityCannonShell;
import com.micatechnologies.minecraft.ldtd.tower.EntityTowerArrow;
import com.micatechnologies.minecraft.ldtd.tower.ItemBlueprint;
import com.micatechnologies.minecraft.ldtd.tower.TileEntityTower;
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
    public static final BlockTowerCore BLOCK_TOWER_CORE = new BlockTowerCore();
    public static final ItemBlueprint ITEM_BLUEPRINT = new ItemBlueprint();
    /** The raid currency (design decision D5, Phase 7). */
    public static final ItemDuskglass ITEM_DUSKGLASS = new ItemDuskglass();

    private LdtdRegistry() {
        throw new AssertionError("No instances.");
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BLOCK_HEARTH);
        event.getRegistry().register(BLOCK_TOWER_CORE);
        GameRegistry.registerTileEntity(TileEntityTower.class,
            new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "tower"));
        GameRegistry.registerTileEntity(TileEntityHearth.class,
            new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "hearth"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(ITEM_HEARTH);
        event.getRegistry().register(ITEM_BLUEPRINT);
        event.getRegistry().register(ITEM_DUSKGLASS);
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
        event.getRegistry().register(gloam(EntityFlicker.class, GloamKind.FLICKER, id++, 0x2E2440, 0x5CF2E6));
        event.getRegistry().register(gloam(EntityMothWraith.class, GloamKind.MOTH_WRAITH, id++, 0x141019, 0xE9E4D8));
        event.getRegistry().register(gloam(EntityDimcaster.class, GloamKind.DIMCASTER, id++, 0x4B2E6F, 0x5CF2E6));
        event.getRegistry().register(EntityEntryBuilder.create()
            .entity(EntityDarkBolt.class)
            .id(new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "dark_bolt"), id++)
            .name(LdtdConstants.MOD_NAMESPACE + ".dark_bolt")
            .tracker(64, 10, true)
            .build());
        event.getRegistry().register(EntityEntryBuilder.create()
            .entity(EntityTowerArrow.class)
            .id(new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "tower_arrow"), id++)
            .name(LdtdConstants.MOD_NAMESPACE + ".tower_arrow")
            .tracker(64, 20, true)
            .build());
        event.getRegistry().register(EntityEntryBuilder.create()
            .entity(EntityCannonShell.class)
            .id(new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "cannon_shell"), id++)
            .name(LdtdConstants.MOD_NAMESPACE + ".cannon_shell")
            .tracker(64, 10, true)
            .build());
        event.getRegistry().register(EntityEntryBuilder.create()
            .entity(EntityHearthSprite.class)
            .id(new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "hearth_sprite"), id++)
            .name(LdtdConstants.MOD_NAMESPACE + ".hearth_sprite")
            .tracker(64, 3, true)
            .build());
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
