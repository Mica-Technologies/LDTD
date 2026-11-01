package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelRenderer;

/**
 * The Eclipse: a Shade's frame wearing a ring of bone behind its head, a halo of darkness
 * (docs/design/ART_DIRECTION.md). Same atlas layout as {@link ModelShade}, plus the halo's bars at
 * (0, 40) 14x2x1 and (32, 40) 2x10x1. Must match {@code eclipse_atlas} in tools/gen_textures.py.
 */
public class ModelEclipse extends ModelShade {

    public ModelEclipse() {
        ModelRenderer top = new ModelRenderer(this, 0, 40);
        top.addBox(-7.0F, -14.0F, 4.0F, 14, 2, 1);
        ModelRenderer bottom = new ModelRenderer(this, 0, 40);
        bottom.addBox(-7.0F, 0.0F, 4.0F, 14, 2, 1);
        ModelRenderer left = new ModelRenderer(this, 32, 40);
        left.addBox(-7.0F, -12.0F, 4.0F, 2, 12, 1);
        ModelRenderer right = new ModelRenderer(this, 32, 40);
        right.addBox(5.0F, -12.0F, 4.0F, 2, 12, 1);
        // Children of the head, so the halo turns with it.
        head.addChild(top);
        head.addChild(bottom);
        head.addChild(left);
        head.addChild(right);
    }
}
