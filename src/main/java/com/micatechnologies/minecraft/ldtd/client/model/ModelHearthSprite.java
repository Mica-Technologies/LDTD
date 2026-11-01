package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The Hearth-sprite: a round ember body, a soot cap and two little tong arms
 * (docs/design/ART_DIRECTION.md). 32x32 atlas: body (0, 0) 4x4x4, cap (0, 8) 6x2x6, arm (24, 0)
 * 1x3x1. Must match {@code sprite_atlas} in tools/gen_textures.py.
 */
public class ModelHearthSprite extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer cap;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;

    public ModelHearthSprite() {
        textureWidth = 32;
        textureHeight = 32;

        body = new ModelRenderer(this, 0, 0);
        body.addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4);
        body.setRotationPoint(0.0F, 18.0F, 0.0F);

        cap = new ModelRenderer(this, 0, 8);
        cap.addBox(-3.0F, -4.0F, -3.0F, 6, 2, 6);
        cap.setRotationPoint(0.0F, 18.0F, 0.0F);

        rightArm = new ModelRenderer(this, 24, 0);
        rightArm.addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1);
        rightArm.setRotationPoint(-2.5F, 18.0F, -1.0F);

        leftArm = new ModelRenderer(this, 24, 0);
        leftArm.mirror = true;
        leftArm.addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1);
        leftArm.setRotationPoint(2.5F, 18.0F, -1.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        body.render(scale);
        cap.render(scale);
        rightArm.render(scale);
        leftArm.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        // Bob gently, and the arms reach forward to work when swinging.
        float bob = MathHelper.sin(ageInTicks * 0.2F) * 0.8F;
        body.rotationPointY = 18.0F + bob;
        cap.rotationPointY = 18.0F + bob;
        rightArm.rotationPointY = 18.0F + bob;
        leftArm.rotationPointY = 18.0F + bob;
        float reach = swingProgress > 0.0F ? MathHelper.sin(swingProgress * (float) Math.PI) * 1.2F : 0.0F;
        rightArm.rotateAngleX = -0.4F - reach;
        leftArm.rotateAngleX = -0.4F - reach;
        cap.rotateAngleZ = MathHelper.sin(ageInTicks * 0.1F) * 0.08F;
    }
}
