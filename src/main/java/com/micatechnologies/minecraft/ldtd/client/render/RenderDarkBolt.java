package com.micatechnologies.minecraft.ldtd.client.render;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.gloam.EntityDarkBolt;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Draws a darkness bolt as a full-bright, camera-facing sprite. */
public class RenderDarkBolt extends Render<EntityDarkBolt> {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(LdtdConstants.MOD_NAMESPACE, "textures/entity/dark_bolt.png");

    public RenderDarkBolt(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityDarkBolt entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 0.15D, z);
        GlStateManager.rotate(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((renderManager.options.thirdPersonView == 2 ? -1 : 1) * -renderManager.playerViewX,
            1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        bindEntityTexture(entity);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(-0.5D, -0.5D, 0.0D).tex(0.0D, 1.0D).endVertex();
        buf.pos(0.5D, -0.5D, 0.0D).tex(1.0D, 1.0D).endVertex();
        buf.pos(0.5D, 0.5D, 0.0D).tex(1.0D, 0.0D).endVertex();
        buf.pos(-0.5D, 0.5D, 0.0D).tex(0.0D, 0.0D).endVertex();
        tess.draw();
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDarkBolt entity) {
        return TEXTURE;
    }
}
