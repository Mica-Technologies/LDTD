package com.micatechnologies.minecraft.ldtd.sim.tower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlueprintTest {

    private static final List<String> TOWERS = Arrays.asList("archer", "cannon");

    private static Blueprint tower() {
        Blueprint b = new Blueprint();
        b.tower = "archer";
        b.palette.put("S", "minecraft:stonebrick");
        b.palette.put("F", "minecraft:fence");
        b.layers.add(Arrays.asList("SSS", "SSS", "SSS"));
        b.layers.add(Arrays.asList("F.F", "...", "F F"));
        b.layers.add(Arrays.asList("   ", " C ", "   "));
        return b.validate("archer_tower", TOWERS);
    }

    @Test
    void cellsAreCentredOnTheAnchorAndSkipAnyCells() {
        List<Blueprint.Cell> cells = tower().cells(0);
        // 9 base + 8 in the middle layer (one ANY) + 1 core.
        assertEquals(18, cells.size());
        Blueprint.Cell core = cells.stream().filter(c -> c.key == Blueprint.CORE).findFirst().get();
        assertEquals(0, core.dx);
        assertEquals(2, core.dy);
        assertEquals(0, core.dz);
    }

    @Test
    void fourQuarterTurnsComeBackToTheStart() {
        Blueprint b = tower();
        Set<String> start = new HashSet<>();
        b.cells(0).forEach(c -> start.add(c.toString()));
        Set<String> turned = new HashSet<>();
        b.cells(4).forEach(c -> turned.add(c.toString()));
        assertEquals(start, turned);
    }

    @Test
    void aQuarterTurnMovesTheMissingCornerClockwise() {
        // The ANY cell sits at the back-centre of layer 1 (x=0, z=+1 once centred). A clockwise
        // quarter turn seen from above (+x east, +z south) maps (x, z) to (-z, x): (0, +1) -> (-1, 0).
        Blueprint b = tower();
        assertTrue(b.cells(0).stream().noneMatch(c -> c.dy == 1 && c.dx == 0 && c.dz == 1));
        assertTrue(b.cells(1).stream().noneMatch(c -> c.dy == 1 && c.dx == -1 && c.dz == 0),
            "the missing cell moved to (-1, 0)");
        assertTrue(b.cells(1).stream().anyMatch(c -> c.dy == 1 && c.dx == 0 && c.dz == 1));
    }

    @Test
    void costCountsPaletteBlocksOnly() {
        Blueprint b = tower();
        assertEquals(Integer.valueOf(9), b.blockCounts().get("minecraft:stonebrick"));
        assertEquals(Integer.valueOf(4), b.blockCounts().get("minecraft:fence"));
        assertEquals(2, b.blockCounts().size());
    }

    @Test
    void rejectsRaggedLayersUnknownCellsAndBadCores() {
        Blueprint ragged = new Blueprint();
        ragged.palette.put("S", "minecraft:stone");
        ragged.layers.add(Arrays.asList("SS", "S"));
        assertThrows(IllegalArgumentException.class, () -> ragged.validate("r", TOWERS));

        Blueprint unknown = new Blueprint();
        unknown.layers.add(Collections.singletonList("X"));
        assertThrows(IllegalArgumentException.class, () -> unknown.validate("u", TOWERS));

        Blueprint noCore = new Blueprint();
        noCore.tower = "archer";
        noCore.palette.put("S", "minecraft:stone");
        noCore.layers.add(Collections.singletonList("S"));
        assertThrows(IllegalArgumentException.class, () -> noCore.validate("n", TOWERS));

        Blueprint wallWithCore = new Blueprint();
        wallWithCore.layers.add(Collections.singletonList("C"));
        assertThrows(IllegalArgumentException.class, () -> wallWithCore.validate("w", TOWERS));

        Blueprint badTower = new Blueprint();
        badTower.tower = "laser";
        badTower.layers.add(Collections.singletonList("C"));
        assertThrows(IllegalArgumentException.class, () -> badTower.validate("b", TOWERS));
    }
}
