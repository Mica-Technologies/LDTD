package com.micatechnologies.minecraft.ldtd.tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.IStringSerializable;

/**
 * Every tower kind, by its key in {@code towers.json}. Also the tower core block's {@code kind}
 * property, so the metadata order here is saved in worlds: only ever append.
 */
public enum TowerKind implements IStringSerializable {
    ARCHER("archer"),
    CANNON("cannon"),
    FROST("frost"),
    LANTERN("lantern"),
    /** Keeps a squad of Ember Guards (Phase 8). */
    GUARDPOST("guardpost"),
    /** Anti-air: fires flares at flying Gloam, leaving them lit (post-1.0). */
    FLARE("flare");

    private final String id;

    TowerKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    public static TowerKind byId(String id) {
        for (TowerKind k : values()) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        return null;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (TowerKind k : values()) {
            ids.add(k.id);
        }
        return Collections.unmodifiableList(ids);
    }
}
