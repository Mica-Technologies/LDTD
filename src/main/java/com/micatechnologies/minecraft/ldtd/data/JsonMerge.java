package com.micatechnologies.minecraft.ldtd.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;

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
 *   <li>Under a top-level {@code "blueprints"} map, a missing blueprint is only restored if the
 *       default marks it {@code "internal"} (a Hearth growth stage). Owners may delete their players'
 *       blueprints on purpose.</li>
 * </ul>
 */
public final class JsonMerge {

    private JsonMerge() {
        throw new AssertionError("No instances.");
    }

    /**
     * Merges {@code defaults} into {@code user}, in place.
     *
     * @param added collects the paths of everything added, for the log
     */
    public static void merge(JsonElement user, JsonElement defaults, String path, List<String> added) {
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
                    if ("blueprints".equals(path) && !isInternal(e.getValue())) {
                        continue;
                    }
                    u.add(key, copy(e.getValue()));
                    added.add(child);
                } else {
                    merge(u.get(key), e.getValue(), child, added);
                }
            }
        } else if (user.isJsonArray() && defaults.isJsonArray()) {
            JsonArray u = user.getAsJsonArray();
            JsonArray d = defaults.getAsJsonArray();
            for (int i = 0; i < Math.min(u.size(), d.size()); i++) {
                merge(u.get(i), d.get(i), path + "[" + i + "]", added);
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
