package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The Moth-wraith: wide flat wings, a small body, and a mask with two eye holes
 * (docs/design/ART_DIRECTION.md). 64x64 atlas: body (0, 0) 4x6x3, mask (16, 0) 6x6x2,
 * wing (0, 16) 14x1x10. Must match {@code moth_atlas} in tools/gen_textures.py.
 */
public class ModelMothWraith extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer mask;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;

    public ModelMothWraith() {
        textureWidth = 64;
        textureHeight = 64;

        body = new ModelRenderer(this, 0, 0);
        body.addBox(-2.0F, 0.0F, -1.5F, 4, 6, 3);
        body.setRotationPoint(0.0F, 14.0F, 0.0F);

        mask = new ModelRenderer(this, 16, 0);
        mask.addBox(-3.0F, -6.0F, -2.5F, 6, 6, 2);
        mask.setRotationPoint(0.0F, 14.0F, 0.0F);

        rightWing = new ModelRenderer(this, 0, 16);
        rightWing.addBox(-14.0F, 0.0F, -2.0F, 14, 1, 10);
        rightWing.setRotationPoint(-2.0F, 15.0F, 0.0F);

        leftWing = new ModelRenderer(this, 0, 16);
        leftWing.mirror = true;
        leftWing.addBox(0.0F, 0.0F, -2.0F, 14, 1, 10);
        leftWing.setRotationPoint(2.0F, 15.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        body.render(scale);
        mask.render(scale);
        rightWing.render(scale);
        leftWing.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        float flap = MathHelper.cos(ageInTicks * 1.3F) * 0.6F;
        rightWing.rotateAngleZ = flap;
        leftWing.rotateAngleZ = -flap;
        // Float the whole body slightly with each beat.
        float bob = MathHelper.sin(ageInTicks * 1.3F) * 0.5F;
        body.rotationPointY = 14.0F + bob;
        mask.rotationPointY = 14.0F + bob;
        rightWing.rotationPointY = 15.0F + bob;
        leftWing.rotationPointY = 15.0F + bob;
    }
}
