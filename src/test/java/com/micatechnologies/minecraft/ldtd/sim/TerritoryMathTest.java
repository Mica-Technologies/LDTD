package com.micatechnologies.minecraft.ldtd.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerritoryMathTest {

    @Test
    void hearthBlockIsAlwaysInside() {
        assertTrue(TerritoryMath.contains(10, -5, 0, 10, -5));
    }

    @Test
    void edgeIsInsideAndOneBeyondIsNot() {
        assertTrue(TerritoryMath.contains(0, 0, 16, 16, 0));
        assertFalse(TerritoryMath.contains(0, 0, 16, 17, 0));
        // (12, 12) is about 16.97 away: outside a radius-16 circle.
        assertFalse(TerritoryMath.contains(0, 0, 16, 12, 12));
        assertTrue(TerritoryMath.contains(0, 0, 16, 11, 11));
    }

    @Test
    void farCoordinatesDoNotOverflow() {
        assertFalse(TerritoryMath.contains(-29_999_000, 0, 64, 29_999_000, 0));
        assertTrue(TerritoryMath.contains(29_999_000, 29_999_000, 64, 29_999_010, 29_999_010));
    }

    @Test
    void negativeRadiusContainsNothing() {
        assertFalse(TerritoryMath.contains(0, 0, -1, 0, 0));
    }

    @Test
    void touchingTerritoriesOverlapAndSeparatedOnesDoNot() {
        assertTrue(TerritoryMath.overlaps(0, 0, 16, 32, 0, 16));
        assertFalse(TerritoryMath.overlaps(0, 0, 16, 33, 0, 16));
    }

    @Test
    void distanceSqIsSymmetric() {
        assertEquals(25L, TerritoryMath.distanceSq(0, 0, 3, 4));
        assertEquals(25L, TerritoryMath.distanceSq(3, 4, 0, 0));
    }
}
