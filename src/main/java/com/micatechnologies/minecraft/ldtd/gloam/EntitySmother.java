package com.micatechnologies.minecraft.ldtd.gloam;

import net.minecraft.world.World;

/**
 * The Smother: the Gloam's siege unit. Slow, heavily built and armoured, and it hits walls about six
 * times as hard as a Shade (docs/GLOSSARY.md). Snuffing light sources comes with the light work in a
 * later phase.
 */
public class EntitySmother extends EntityGloam {

    public EntitySmother(World world) {
        super(world);
        // The hitbox must fit the 1x2 tunnels the router plans, so it is smaller than the model,
        // which renders at 1.3x and looms over the gap it squeezes through.
        setSize(0.9F, 1.95F);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.SMOTHER;
    }
}
