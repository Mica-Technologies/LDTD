package com.micatechnologies.minecraft.ldtd.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonMergeTest {

    private static JsonElement json(String s) {
        return new JsonParser().parse(s);
    }

    @Test
    void addsMissingFieldsAndKeepsOwnerValues() {
        JsonElement user = json("{\"levels\":[{\"level\":1,\"radius\":30},{\"level\":2,\"radius\":36}]}");
        JsonElement def = json("{\"levels\":[{\"level\":1,\"radius\":24,\"duskglass\":0},"
            + "{\"level\":2,\"radius\":32,\"duskglass\":8},{\"level\":3,\"radius\":40,\"duskglass\":20}]}");
        List<String> added = new ArrayList<>();
        JsonMerge.merge(user, def, "", added);
        JsonObject l2 = user.getAsJsonObject().getAsJsonArray("levels").get(1).getAsJsonObject();
        assertEquals(36, l2.get("radius").getAsInt(), "the owner's edit stays");
        assertEquals(8, l2.get("duskglass").getAsInt(), "the new field arrives");
        assertEquals(2, user.getAsJsonObject().getAsJsonArray("levels").size(), "a trimmed list stays trimmed");
        assertTrue(added.contains("levels[1].duskglass"));
    }

    @Test
    void addsNewMapEntriesLikeNewUnits() {
        JsonElement user = json("{\"units\":{\"shade\":{\"health\":30}}}");
        JsonElement def = json("{\"units\":{\"shade\":{\"health\":20,\"speed\":0.25},\"eclipse\":{\"health\":400}}}");
        List<String> added = new ArrayList<>();
        JsonMerge.merge(user, def, "", added);
        JsonObject units = user.getAsJsonObject().getAsJsonObject("units");
        assertEquals(30, units.getAsJsonObject("shade").get("health").getAsInt());
        assertTrue(units.has("eclipse"));
        assertTrue(added.contains("units.shade.speed"));
    }

    @Test
    void restoresOnlyInternalBlueprints() {
        JsonElement user = json("{\"blueprints\":{\"stone_wall\":{}}}");
        JsonElement def = json("{\"blueprints\":{\"stone_wall\":{},\"archer_tower\":{\"tower\":\"archer\"},"
            + "\"hearth_stage_1\":{\"internal\":true}}}");
        List<String> added = new ArrayList<>();
        JsonMerge.merge(user, def, "", added);
        JsonObject bps = user.getAsJsonObject().getAsJsonObject("blueprints");
        assertFalse(bps.has("archer_tower"), "a blueprint the owner removed stays removed");
        assertTrue(bps.has("hearth_stage_1"), "growth stages come back");
    }

    @Test
    void refreshesCommentsWithoutCountingThem() {
        JsonElement user = json("{\"_comment\":\"old\",\"a\":1}");
        JsonElement def = json("{\"_comment\":\"new\",\"a\":2}");
        List<String> added = new ArrayList<>();
        JsonMerge.merge(user, def, "", added);
        assertEquals("new", user.getAsJsonObject().get("_comment").getAsString());
        assertEquals(1, user.getAsJsonObject().get("a").getAsInt());
        assertTrue(added.isEmpty());
    }
}
