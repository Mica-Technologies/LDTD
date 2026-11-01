package com.micatechnologies.minecraft.ldtd.client;

import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The Hearths the client currently has loaded, for the territory border overlay.
 *
 * <p>Kept up to date from {@link TileEntityHearth#onLoad()} and its unload hooks, through the
 * client proxy. Scanning {@code World#loadedTileEntityList} every frame instead would cost a pass
 * over every tile entity in render distance. Weak keys mean a missed unload hook can't leak a
 * whole world.</p>
 */
public final class ClientHearths {

    private static final Set<TileEntityHearth> LOADED =
        Collections.newSetFromMap(new WeakHashMap<TileEntityHearth, Boolean>());

    private ClientHearths() {
        throw new AssertionError("No instances.");
    }

    public static void add(TileEntityHearth hearth) {
        LOADED.add(hearth);
    }

    public static void remove(TileEntityHearth hearth) {
        LOADED.remove(hearth);
    }

    /** A snapshot, safe to iterate while hooks add or remove entries. */
    public static TileEntityHearth[] snapshot() {
        return LOADED.toArray(new TileEntityHearth[0]);
    }
}
