package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The Dimcaster: a Shade's frame with a hood over the mask and one arm raised, holding a dark orb
 * (docs/design/ART_DIRECTION.md). Same 64x64 atlas layout as {@link ModelShade}, plus the hood at
 * (0, 40) and the orb at (40, 40). Must match {@code dimcaster_atlas} in tools/gen_textures.py.
 */
public class ModelDimcaster extends ModelShade {

    private final ModelRenderer hood;
    private final ModelRenderer orb;

    public ModelDimcaster() {
        hood = new ModelRenderer(this, 0, 40);
        hood.addBox(-4.5F, -9.5F, -4.5F, 9, 3, 9);

        // Child of the right arm, so it rides on the raised hand.
        orb = new ModelRenderer(this, 40, 40);
        orb.addBox(-2.0F, 11.0F, -2.0F, 4, 4, 4);
        rightArm.addChild(orb);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        hood.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        hood.rotateAngleX = head.rotateAngleX;
        hood.rotateAngleY = head.rotateAngleY;
        // The orb arm held high, bobbing slowly; it snaps forward when casting.
        rightArm.rotateAngleX = -2.7F + MathHelper.sin(ageInTicks * 0.1F) * 0.08F;
        if (swingProgress > 0.0F) {
            rightArm.rotateAngleX += MathHelper.sin(swingProgress * (float) Math.PI) * 1.4F;
        }
        orb.rotateAngleY = ageInTicks * 0.15F;
    }
}
