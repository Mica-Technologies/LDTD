package com.micatechnologies.minecraft.ldtd.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * The Shade: tall and thin, a ragged cloak, long arms that hang forward, and a blank mask
 * (docs/design/ART_DIRECTION.md). 64x64 atlas, painted by tools/gen_textures.py. The texture
 * offsets and box sizes here must match {@code shade_atlas} in that script.
 */
public class ModelShade extends ModelBase {

    protected final ModelRenderer head;
    protected final ModelRenderer body;
    protected final ModelRenderer rightArm;
    protected final ModelRenderer leftArm;
    protected final ModelRenderer rightLeg;
    protected final ModelRenderer leftLeg;

    public ModelShade() {
        textureWidth = 64;
        textureHeight = 64;

        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8);

        body = new ModelRenderer(this, 16, 16);
        body.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4);

        rightArm = new ModelRenderer(this, 40, 16);
        rightArm.addBox(-2.0F, -2.0F, -1.5F, 3, 14, 3);
        rightArm.setRotationPoint(-5.5F, 2.0F, 0.0F);

        leftArm = new ModelRenderer(this, 40, 16);
        leftArm.mirror = true;
        leftArm.addBox(-1.0F, -2.0F, -1.5F, 3, 14, 3);
        leftArm.setRotationPoint(5.5F, 2.0F, 0.0F);

        rightLeg = new ModelRenderer(this, 0, 16);
        rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4);
        rightLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);

        leftLeg = new ModelRenderer(this, 0, 16);
        leftLeg.mirror = true;
        leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4);
        leftLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
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
        head.rotateAngleY = netHeadYaw * 0.017453292F;
        head.rotateAngleX = headPitch * 0.017453292F;

        float walk = MathHelper.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.rotateAngleX = walk;
        leftLeg.rotateAngleX = -walk;

        // Arms reach forward and drift, like something not quite solid.
        float drift = MathHelper.sin(ageInTicks * 0.067F) * 0.06F;
        rightArm.rotateAngleX = -0.45F + drift - walk * 0.2F;
        leftArm.rotateAngleX = -0.45F - drift + walk * 0.2F;
        rightArm.rotateAngleZ = 0.06F;
        leftArm.rotateAngleZ = -0.06F;
        if (swingProgress > 0.0F) {
            float s = MathHelper.sin(swingProgress * (float) Math.PI);
            rightArm.rotateAngleX -= s * 1.2F;
            leftArm.rotateAngleX -= s * 1.2F;
        }
    }
}
