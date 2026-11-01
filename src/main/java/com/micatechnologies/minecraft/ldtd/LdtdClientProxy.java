package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.client.ClientHearths;
import com.micatechnologies.minecraft.ldtd.client.TerritoryBorderRenderer;
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
