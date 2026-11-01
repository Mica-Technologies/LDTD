package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.client.ClientHearths;
import com.micatechnologies.minecraft.ldtd.client.TerritoryBorderRenderer;
import com.micatechnologies.minecraft.ldtd.client.model.ModelShade;
import com.micatechnologies.minecraft.ldtd.client.model.ModelSmother;
import com.micatechnologies.minecraft.ldtd.client.render.RenderGloam;
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
