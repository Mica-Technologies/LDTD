package com.micatechnologies.minecraft.ldtd.network;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * LDTD's network channel. Created in preInit so that its discriminators are fixed before any
 * connection exists.
 *
 * <p>Hearth state reaches clients through the vanilla tile-entity update packet, so no packets are
 * registered here yet. Raid telegraphs (Phase 4) and builder previews (Phase 6) will be. Rules for
 * every packet added here:</p>
 * <ul>
 *   <li>Discriminators are append-only. Never reuse or reorder one: the two sides must agree.</li>
 *   <li>A {@code Side.CLIENT} handler must reach client classes only through the proxy, or a
 *       dedicated server crashes when it loads the handler class.</li>
 *   <li>Server handlers validate everything a client sends. Clients are not trusted.</li>
 * </ul>
 */
public final class LdtdNetwork {

    public static SimpleNetworkWrapper CHANNEL;

    private LdtdNetwork() {
        throw new AssertionError("No instances.");
    }

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(LdtdConstants.MOD_NAMESPACE);
    }
}
