package com.micatechnologies.minecraft.ldtd;

/**
 * Compile-time constants for the mod's identity.
 *
 * <p>The values come from {@code Tags}, a class the GTCEu buildscript generates at build time
 * from {@code buildscript.properties} ({@code generateGradleTokenClass}). Do not hard-code the
 * mod id or version anywhere else. The version is derived from the latest git tag, so a literal
 * would drift the moment a release is cut.</p>
 */
public final class LdtdConstants {

    /** Registry namespace and Forge mod id. Every {@code ResourceLocation} we create uses this. */
    public static final String MOD_NAMESPACE = Tags.MODID;

    /** Human-readable mod name, as shown in the Forge mod list. */
    public static final String MOD_NAME = Tags.MODNAME;

    /** Version string, derived from the latest git tag ({@code YYYY.MM.DD} for releases). */
    public static final String MOD_VERSION = Tags.VERSION;

    /** Minecraft ticks per second. */
    public static final int TICKS_PER_SECOND = 20;

    private LdtdConstants() {
        throw new AssertionError("No instances.");
    }
}
