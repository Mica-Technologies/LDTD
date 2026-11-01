package com.micatechnologies.minecraft.ldtd;

import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Proxy for code that runs on both sides. This is also the dedicated-server proxy.
 * {@link LdtdClientProxy} extends it with client-only setup.
 *
 * <p>Common code that needs to tell the client something (a Hearth came into view, say) calls a
 * method here. On a server it does nothing, and the client proxy overrides it. That is the only
 * route from common code to client classes.</p>
 */
public class LdtdCommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /** A Hearth tile entity loaded on the client. No-op on a server. */
    public void onHearthLoaded(TileEntityHearth hearth) {
    }

    /** A Hearth tile entity unloaded or was removed on the client. No-op on a server. */
    public void onHearthUnloaded(TileEntityHearth hearth) {
    }
}
