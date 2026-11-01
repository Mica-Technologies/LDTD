package com.micatechnologies.minecraft.ldtd.gloam;

import net.minecraft.world.World;

/** The Shade: the Gloam's masked footsoldier, and the bulk of every raid (docs/GLOSSARY.md). */
public class EntityShade extends EntityGloam {

    public EntityShade(World world) {
        super(world);
        // Must fit the 1x2 tunnels the router plans (Router assumes a 2-block-tall body).
        setSize(0.6F, 1.95F);
    }

    @Override
    public GloamKind kind() {
        return GloamKind.SHADE;
    }
}
