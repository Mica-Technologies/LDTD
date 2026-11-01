package com.micatechnologies.minecraft.ldtd.gloam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every Gloam unit, by its data id (the key in {@code gloam.json}) and registry path. Names come
 * from docs/GLOSSARY.md.
 */
public enum GloamKind {
    SHADE("shade"),
    SMOTHER("smother"),
    FLICKER("flicker"),
    MOTH_WRAITH("moth_wraith"),
    DIMCASTER("dimcaster"),
    /** The boss (Phase 9). Never bought by a raid budget: it comes when spires fall. */
    ECLIPSE("eclipse");

    private final String id;

    GloamKind(String id) {
        this.id = id;
    }

    /** The key in {@code gloam.json}. */
    public String id() {
        return id;
    }

    /** The entity registry path, {@code gloam_<id>}. */
    public String registryPath() {
        return "gloam_" + id;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (GloamKind k : values()) {
            ids.add(k.id);
        }
        return Collections.unmodifiableList(ids);
    }
}
