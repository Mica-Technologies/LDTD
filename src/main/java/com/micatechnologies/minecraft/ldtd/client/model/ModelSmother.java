package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The Smother: half again as wide as a Shade, huge arms, and a tiny mask sunk into a broad body
 * (docs/design/ART_DIRECTION.md). 64x64 atlas, painted by tools/gen_textures.py. The texture
 * offsets and box sizes here must match {@code smother_atlas} in that script.
 */
public class ModelSmother extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;

    public ModelSmother() {
        textureWidth = 64;
        textureHeight = 64;

        head = new ModelRenderer(this, 0, 0);
        head.addBox(-3.0F, -5.0F, -3.0F, 6, 6, 6);
        head.setRotationPoint(0.0F, 1.0F, -3.0F);

        body = new ModelRenderer(this, 0, 12);
        body.addBox(-7.0F, 0.0F, -4.0F, 14, 14, 8);

        rightArm = new ModelRenderer(this, 40, 34);
        rightArm.addBox(-3.0F, -2.0F, -3.0F, 6, 16, 6);
        rightArm.setRotationPoint(-10.0F, 2.0F, 0.0F);

        leftArm = new ModelRenderer(this, 40, 34);
        leftArm.mirror = true;
        leftArm.addBox(-3.0F, -2.0F, -3.0F, 6, 16, 6);
        leftArm.setRotationPoint(10.0F, 2.0F, 0.0F);

        rightLeg = new ModelRenderer(this, 0, 34);
        rightLeg.addBox(-2.5F, 0.0F, -2.5F, 5, 10, 5);
        rightLeg.setRotationPoint(-4.0F, 14.0F, 0.0F);

        leftLeg = new ModelRenderer(this, 0, 34);
        leftLeg.mirror = true;
        leftLeg.addBox(-2.5F, 0.0F, -2.5F, 5, 10, 5);
        leftLeg.setRotationPoint(4.0F, 14.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                       float headPitch, float scale) {
        setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        head.render(scale);
        body.render(scale);
        rightArm.render(scale);
        leftArm.render(scale);
        rightLeg.render(scale);
        leftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        head.rotateAngleY = netHeadYaw * 0.017453292F * 0.5F;
        head.rotateAngleX = headPitch * 0.017453292F * 0.5F;

        // A heavy, slow gait: short leg swing, arms swinging against it.
        float walk = MathHelper.cos(limbSwing * 0.45F) * 0.8F * limbSwingAmount;
        rightLeg.rotateAngleX = walk;
        leftLeg.rotateAngleX = -walk;
        rightArm.rotateAngleX = -walk * 0.6F;
        leftArm.rotateAngleX = walk * 0.6F;
        if (swingProgress > 0.0F) {
            // Both fists come down together: how a Smother hits a wall.
            float s = MathHelper.sin(swingProgress * (float) Math.PI);
            rightArm.rotateAngleX = -s * 2.0F;
            leftArm.rotateAngleX = -s * 2.0F;
        }
    }
}
