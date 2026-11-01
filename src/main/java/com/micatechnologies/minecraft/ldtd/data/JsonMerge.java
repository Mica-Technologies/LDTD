package com.micatechnologies.minecraft.ldtd.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brings an older, owner-edited data file up to date with a newer LDTD's defaults (config
 * migration, decided 2026-11-01). Anything the owner's file lacks is filled in from the default.
 * Nothing the owner set is ever changed.
 *
 * <ul>
 *   <li>Objects: missing keys are copied from the default, then shared keys are merged
 *       recursively.</li>
 *   <li>Arrays: merged element by element where both have an element. Elements are never added
 *       or removed, so an owner who trimmed a list (for example, fewer Hearth levels) keeps it
 *       trimmed.</li>
 *   <li>Values: the owner's wins.</li>
 *   <li>{@code _comment} keys are refreshed from the default, so the file's help text stays current.</li>
 *   <li>Under a top-level {@code "blueprints"} map, a missing blueprint is restored if the default
 *       marks it {@code "internal"} (a Hearth growth stage), or if it is <i>new</i>: a default the
 *       file has never seen. The file records every default it has seen in a top-level
 *       {@code "_defaultsSeen"} list, so a blueprint an owner deletes stays deleted, while one added
 *       by a later LDTD still arrives. A file with no list (written by LDTD 1.0) is taken to have
 *       seen {@link #FIRST_RELEASE_BLUEPRINTS}.</li>
 * </ul>
 */
public final class JsonMerge {

    /** The blueprints.json key recording which default blueprints a file has seen. */
    static final String SEEN = "_defaultsSeen";

    /** Every default blueprint the first release (2026.11.01) shipped. Never edit: it is history. */
    static final Set<String> FIRST_RELEASE_BLUEPRINTS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "archer_tower", "cannon_tower", "frost_tower", "lantern_post", "guard_post", "stone_wall",
        "hearth_stage_1", "hearth_stage_3", "hearth_stage_5")));

    private JsonMerge() {
        throw new AssertionError("No instances.");
    }

    /**
     * Merges {@code defaults} into {@code user}, in place.
     *
     * @param added collects the paths of everything added, for the log
     */
    public static void merge(JsonElement user, JsonElement defaults, String path, List<String> added) {
        Set<String> seen = Collections.emptySet();
        boolean blueprintFile = path.isEmpty() && user.isJsonObject() && defaults.isJsonObject()
            && defaults.getAsJsonObject().has("blueprints") && defaults.getAsJsonObject().get("blueprints").isJsonObject();
        if (blueprintFile) {
            seen = seenBlueprints(user.getAsJsonObject());
        }
        merge(user, defaults, path, added, seen);
        if (blueprintFile) {
            Set<String> all = new LinkedHashSet<>(seen);
            for (Map.Entry<String, JsonElement> e : defaults.getAsJsonObject().getAsJsonObject("blueprints").entrySet()) {
                all.add(e.getKey());
            }
            JsonArray list = new JsonArray();
            for (String id : all) {
                list.add(id);
            }
            user.getAsJsonObject().add(SEEN, list);
        }
    }

    /** The default blueprints this file has already been offered. */
    private static Set<String> seenBlueprints(JsonObject user) {
        JsonElement list = user.get(SEEN);
        if (list == null || !list.isJsonArray()) {
            return FIRST_RELEASE_BLUEPRINTS;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (JsonElement e : list.getAsJsonArray()) {
            if (e.isJsonPrimitive()) {
                seen.add(e.getAsString());
            }
        }
        return seen;
    }

    private static void merge(JsonElement user, JsonElement defaults, String path, List<String> added,
                              Set<String> seenBlueprints) {
        if (user.isJsonObject() && defaults.isJsonObject()) {
            JsonObject u = user.getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : defaults.getAsJsonObject().entrySet()) {
                String key = e.getKey();
                String child = path.isEmpty() ? key : path + "." + key;
                if ("_comment".equals(key)) {
                    u.add(key, copy(e.getValue()));
                    continue;
                }
                if (!u.has(key)) {
                    if ("blueprints".equals(path) && !isInternal(e.getValue()) && seenBlueprints.contains(key)) {
                        continue;
                    }
                    u.add(key, copy(e.getValue()));
                    added.add(child);
                } else {
                    merge(u.get(key), e.getValue(), child, added, seenBlueprints);
                }
            }
        } else if (user.isJsonArray() && defaults.isJsonArray()) {
            JsonArray u = user.getAsJsonArray();
            JsonArray d = defaults.getAsJsonArray();
            for (int i = 0; i < Math.min(u.size(), d.size()); i++) {
                merge(u.get(i), d.get(i), path + "[" + i + "]", added, seenBlueprints);
            }
        }
    }

    /** A deep copy (this Gson keeps {@code JsonElement.deepCopy} package-private). */
    private static JsonElement copy(JsonElement e) {
        return new com.google.gson.JsonParser().parse(e.toString());
    }

    private static boolean isInternal(JsonElement blueprint) {
        return blueprint.isJsonObject() && blueprint.getAsJsonObject().has("internal")
            && blueprint.getAsJsonObject().get("internal").getAsBoolean();
    }
}
