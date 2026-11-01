package com.micatechnologies.minecraft.ldtd.sim.tower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TowerUpgradeTest {

    @Test
    void defaultsScaleAndCost() {
        TowerUpgrade u = new TowerUpgrade().validate();
        assertEquals(1.0D, u.damage(1));
        assertEquals(2.2D, u.damage(3));
        assertEquals(10, u.costFrom(1));
        assertEquals(25, u.costFrom(2));
        assertEquals(-1, u.costFrom(3));
        assertEquals(0, u.spentTo(1));
        assertEquals(10, u.spentTo(2));
        assertEquals(35, u.spentTo(3));
    }

    @Test
    void tiersOutsideTheRangeClamp() {
        TowerUpgrade u = new TowerUpgrade().validate();
        assertEquals(u.range(1), u.range(0));
        assertEquals(u.range(3), u.range(9));
    }

    @Test
    void rejectsWrongLengthsAndBadValues() {
        TowerUpgrade shortCost = new TowerUpgrade();
        shortCost.cost = new int[] {5};
        assertThrows(IllegalArgumentException.class, shortCost::validate);
        TowerUpgrade zeroRange = new TowerUpgrade();
        zeroRange.range = new double[] {1.0D, 0.0D, 1.0D};
        assertThrows(IllegalArgumentException.class, zeroRange::validate);
    }
}
