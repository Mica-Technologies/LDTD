package com.micatechnologies.minecraft.ldtd;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Proxy for code that runs on both sides. This is also the dedicated-server proxy.
 * {@link LdtdClientProxy} extends it with client-only setup.
 */
public class LdtdCommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
