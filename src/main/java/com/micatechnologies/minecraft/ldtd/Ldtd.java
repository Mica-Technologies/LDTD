package com.micatechnologies.minecraft.ldtd;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for LDTD (Limitless Development: Tower Defense).
 *
 * <p>LDTD is a survival tower-defense system: you claim territory with a Hearth, builders put up
 * towers and walls from blueprints, and raids come at night from enemy outposts that you can
 * assault in turn.</p>
 *
 * <p>Lifecycle contract for the rest of the mod:</p>
 * <ul>
 *   <li>{@code preInit}: config, network channel, event-bus registrations. Nothing here may touch
 *       a {@code World}.</li>
 *   <li>{@code init}: GUI and inter-mod wiring.</li>
 *   <li>{@code postInit}: anything that must observe other mods' completed registries.</li>
 * </ul>
 *
 * <p><b>Side discipline.</b> Everything reachable from this class must load on a dedicated
 * server. Client-only code is reached only through {@link LdtdClientProxy}. One stray import of a
 * {@code net.minecraft.client} type in common code fails the CI server smoke test.</p>
 */
@Mod(modid = LdtdConstants.MOD_NAMESPACE,
     version = LdtdConstants.MOD_VERSION,
     name = LdtdConstants.MOD_NAME,
     acceptedMinecraftVersions = "[1.12.2]")
public class Ldtd {

    public static final Logger LOGGER = LogManager.getLogger(LdtdConstants.MOD_NAMESPACE);

    @SidedProxy(clientSide = "com.micatechnologies.minecraft.ldtd.LdtdClientProxy",
                serverSide = "com.micatechnologies.minecraft.ldtd.LdtdCommonProxy")
    public static LdtdCommonProxy proxy;

    @Mod.Instance(LdtdConstants.MOD_NAMESPACE)
    public static Ldtd instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        LOGGER.info("I am {} at version {}", LdtdConstants.MOD_NAME, LdtdConstants.MOD_VERSION);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
