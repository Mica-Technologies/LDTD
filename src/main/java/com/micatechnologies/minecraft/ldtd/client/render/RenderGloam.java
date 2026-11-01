package com.micatechnologies.minecraft.ldtd.client.render;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * Renders any Gloam unit: its model and atlas, plus a second pass that draws the eyes full-bright
 * so they glow in the dark (docs/design/ART_DIRECTION.md, "Emissive layers").
 */
public class RenderGloam<T extends EntityGloam> extends RenderLiving<T> {

    private final ResourceLocation texture;
    private final float scale;

    public RenderGloam(RenderManager manager, ModelBase model, float shadow, float scale, String name) {
        super(manager, model, shadow);
        this.scale = scale;
        this.texture = new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "textures/entity/" + name + ".png");
        addLayer(new EyesLayer(new ResourceLocation(LdtdConstants.MOD_NAMESPACE,
            "textures/entity/" + name + "_eyes.png")));
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return texture;
    }

    @Override
    protected void preRenderCallback(T entity, float partialTickTime) {
        GlStateManager.scale(scale, scale, scale);
    }

    /** Full-bright, additive eye glow. Dimmed while the Gloam is weakened by light. */
    private final class EyesLayer implements LayerRenderer<T> {

        private final ResourceLocation eyes;

        EyesLayer(ResourceLocation eyes) {
            this.eyes = eyes;
        }

        @Override
        public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            bindTexture(eyes);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!entity.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            float glow = entity.isLit() ? 0.45F : 1.0F;
            GlStateManager.color(glow, glow, glow, 1.0F);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                scale);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            setLightmap(entity);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
