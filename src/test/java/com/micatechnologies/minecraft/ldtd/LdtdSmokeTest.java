package com.micatechnologies.minecraft.ldtd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Proves the JUnit pipeline runs on a bare JVM. Replace with real tests as the sim cores land. */
class LdtdSmokeTest {

    @Test
    void ticksPerSecondIsVanilla() {
        assertEquals(20, LdtdConstants.TICKS_PER_SECOND);
    }
}
