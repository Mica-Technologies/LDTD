package com.micatechnologies.minecraft.ldtd.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class HearthLevelTableTest {

    @Test
    void sortsAndLooksUpLevels() {
        HearthLevelTable t = HearthLevelTable.validate(Arrays.asList(
            new HearthLevel(2, 32, 3), new HearthLevel(1, 24, 2), new HearthLevel(3, 40, 4)));
        assertEquals(3, t.maxLevel());
        assertEquals(24, t.get(1).radius);
        assertEquals(40, t.maxRadius());
    }

    @Test
    void outOfRangeLevelsClamp() {
        HearthLevelTable t = HearthLevelTable.validate(Arrays.asList(
            new HearthLevel(1, 24, 2), new HearthLevel(2, 32, 3)));
        assertEquals(24, t.get(0).radius);
        assertEquals(32, t.get(99).radius);
    }

    @Test
    void rejectsGaps() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> HearthLevelTable.validate(Arrays.asList(new HearthLevel(1, 24, 2), new HearthLevel(3, 32, 3))));
        assertTrue(e.getMessage().contains("level 3"));
    }

    @Test
    void rejectsShrinkingRadius() {
        assertThrows(IllegalArgumentException.class,
            () -> HearthLevelTable.validate(Arrays.asList(new HearthLevel(1, 32, 2), new HearthLevel(2, 24, 3))));
    }

    @Test
    void rejectsRadiusOutOfBounds() {
        assertThrows(IllegalArgumentException.class,
            () -> HearthLevelTable.validate(Collections.singletonList(new HearthLevel(1, 4, 1))));
        assertThrows(IllegalArgumentException.class,
            () -> HearthLevelTable.validate(Collections.singletonList(new HearthLevel(1, 500, 1))));
    }

    @Test
    void rejectsEmptyTable() {
        assertThrows(IllegalArgumentException.class, () -> HearthLevelTable.validate(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> HearthLevelTable.validate(null));
    }
}
