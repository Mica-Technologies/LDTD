package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.client.ClientHearths;
import com.micatechnologies.minecraft.ldtd.client.TerritoryBorderRenderer;
import com.micatechnologies.minecraft.ldtd.client.model.ModelDimcaster;
import com.micatechnologies.minecraft.ldtd.client.model.ModelHearthSprite;
import com.micatechnologies.minecraft.ldtd.hearth.EntityHearthSprite;
import com.micatechnologies.minecraft.ldtd.client.model.ModelMothWraith;
import com.micatechnologies.minecraft.ldtd.client.model.ModelShade;
import com.micatechnologies.minecraft.ldtd.client.model.ModelSmother;
import com.micatechnologies.minecraft.ldtd.client.GhostPreviewRenderer;
import com.micatechnologies.minecraft.ldtd.client.render.RenderBillboard;
import com.micatechnologies.minecraft.ldtd.tower.EntityCannonShell;
import com.micatechnologies.minecraft.ldtd.tower.EntityTowerArrow;
import net.minecraft.client.renderer.entity.RenderTippedArrow;
import com.micatechnologies.minecraft.ldtd.client.render.RenderGloam;
import com.micatechnologies.minecraft.ldtd.gloam.EntityDarkBolt;
import com.micatechnologies.minecraft.ldtd.gloam.EntityDimcaster;
import com.micatechnologies.minecraft.ldtd.gloam.EntityFlicker;
import com.micatechnologies.minecraft.ldtd.gloam.EntityMothWraith;
import com.micatechnologies.minecraft.ldtd.gloam.EntityShade;
import com.micatechnologies.minecraft.ldtd.gloam.EntitySmother;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Client-side proxy. The only path from common code to client-only classes (renderers, HUD,
 * ghost previews). Never reference this class directly from common code.
 */
public class LdtdClientProxy extends LdtdCommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new TerritoryBorderRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntityShade.class,
            m -> new RenderGloam<>(m, new ModelShade(), 0.4F, 1.05F, GloamKind.SHADE.registryPath()));
        RenderingRegistry.registerEntityRenderingHandler(EntitySmother.class,
            m -> new RenderGloam<>(m, new ModelSmother(), 0.8F, 1.3F, GloamKind.SMOTHER.registryPath()));
        RenderingRegistry.registerEntityRenderingHandler(EntityFlicker.class,
            m -> new RenderGloam<>(m, new ModelShade(), 0.35F, 0.85F, 15.0F, GloamKind.FLICKER.registryPath()));
        RenderingRegistry.registerEntityRenderingHandler(EntityMothWraith.class,
            m -> new RenderGloam<>(m, new ModelMothWraith(), 0.3F, 1.0F, GloamKind.MOTH_WRAITH.registryPath()));
        RenderingRegistry.registerEntityRenderingHandler(EntityDimcaster.class,
            m -> new RenderGloam<>(m, new ModelDimcaster(), 0.4F, 1.0F, GloamKind.DIMCASTER.registryPath()));
        RenderingRegistry.registerEntityRenderingHandler(EntityDarkBolt.class,
            m -> new RenderBillboard<>(m, "dark_bolt", 0.5F));
        RenderingRegistry.registerEntityRenderingHandler(EntityCannonShell.class,
            m -> new RenderBillboard<>(m, "cannon_shell", 0.45F));
        RenderingRegistry.registerEntityRenderingHandler(EntityTowerArrow.class, RenderTippedArrow::new);
        MinecraftForge.EVENT_BUS.register(new GhostPreviewRenderer());
        RenderingRegistry.registerEntityRenderingHandler(com.micatechnologies.minecraft.ldtd.tower.EntityEmberGuard.class,
            m -> new net.minecraft.client.renderer.entity.RenderLiving<com.micatechnologies.minecraft.ldtd.tower.EntityEmberGuard>(
                m, new ModelShade(), 0.5F) {
                private final net.minecraft.util.ResourceLocation tex = new net.minecraft.util.ResourceLocation(
                    LdtdConstants.MOD_NAMESPACE, "textures/entity/ember_guard.png");

                @Override
                protected net.minecraft.util.ResourceLocation getEntityTexture(
                    com.micatechnologies.minecraft.ldtd.tower.EntityEmberGuard entity) {
                    return tex;
                }

                @Override
                protected void preRenderCallback(com.micatechnologies.minecraft.ldtd.tower.EntityEmberGuard entity,
                                                 float partialTicks) {
                    net.minecraft.client.renderer.GlStateManager.scale(1.05F, 1.05F, 1.05F);
                }
            });
        RenderingRegistry.registerEntityRenderingHandler(EntityHearthSprite.class,
            m -> new net.minecraft.client.renderer.entity.RenderLiving<EntityHearthSprite>(m, new ModelHearthSprite(), 0.15F) {
                private final net.minecraft.util.ResourceLocation tex = new net.minecraft.util.ResourceLocation(
                    LdtdConstants.MOD_NAMESPACE, "textures/entity/hearth_sprite.png");

                @Override
                protected net.minecraft.util.ResourceLocation getEntityTexture(EntityHearthSprite entity) {
                    return tex;
                }
            });
    }

    @Override
    public void onHearthLoaded(TileEntityHearth hearth) {
        ClientHearths.add(hearth);
    }

    @Override
    public void onHearthUnloaded(TileEntityHearth hearth) {
        ClientHearths.remove(hearth);
    }
}
