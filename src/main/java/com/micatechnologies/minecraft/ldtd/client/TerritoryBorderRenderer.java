package com.micatechnologies.minecraft.ldtd.client;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Draws the edge of nearby Hearth territories as an ember-coloured fence of lines around the
 * player's height. By default only while holding a Hearth, which is when it matters: deciding where
 * the next one can go. The {@code client.showTerritoryBorders} config switches it to always or never.
 */
public final class TerritoryBorderRenderer {

    /** How far beyond a territory's edge the player can be and still see it. */
    private static final double VIEW_MARGIN = 48.0D;

    /** Ember core, #FF9A3C (docs/design/ART_DIRECTION.md). */
    private static final float R = 1.0F;
    private static final float G = 0x9A / 255.0F;
    private static final float B = 0x3C / 255.0F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !shouldShow(player)) {
            return;
        }
        float pt = event.getPartialTicks();
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        TileEntityHearth[] hearths = ClientHearths.snapshot();
        if (hearths.length == 0) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);

        for (TileEntityHearth hearth : hearths) {
            if (hearth.isInvalid() || hearth.getWorld() != mc.world || hearth.radius() <= 0 || hearth.isDormant()) {
                continue;
            }
            double cx = hearth.getPos().getX() + 0.5D - camX;
            double cz = hearth.getPos().getZ() + 0.5D - camZ;
            int radius = hearth.radius();
            double reach = radius + VIEW_MARGIN;
            if (cx * cx + cz * cz > reach * reach) {
                continue;
            }
            drawFence(cx, Math.floor(camY) - camY, cz, radius);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static boolean shouldShow(EntityPlayerSP player) {
        switch (LdtdConfig.showTerritoryBorders) {
            case "always":
                return true;
            case "never":
                return false;
            default:
                Item hearth = LdtdRegistry.ITEM_HEARTH;
                return player.getHeldItemMainhand().getItem() == hearth
                    || player.getHeldItemOffhand().getItem() == hearth;
        }
    }

    /**
     * Rings at three heights around the player's feet, plus a vertical post every few segments.
     * Coordinates are relative to the camera. The edge drawn is the outer edge of the outermost
     * block centres inside the territory, which is where a player standing on the line expects it.
     */
    private static void drawFence(double cx, double feetY, double cz, int radius) {
        double r = radius + 0.5D;
        int segments = Math.max(48, Math.min(256, radius * 4));
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double[] heights = {0.05D, 1.5D, 3.0D};
        for (int i = 0; i < segments; i++) {
            double a0 = 2.0D * Math.PI * i / segments;
            double a1 = 2.0D * Math.PI * (i + 1) / segments;
            double x0 = cx + Math.cos(a0) * r;
            double z0 = cz + Math.sin(a0) * r;
            double x1 = cx + Math.cos(a1) * r;
            double z1 = cz + Math.sin(a1) * r;
            for (int h = 0; h < heights.length; h++) {
                float alpha = 0.85F - h * 0.25F;
                double y = feetY + heights[h];
                buf.pos(x0, y, z0).color(R, G, B, alpha).endVertex();
                buf.pos(x1, y, z1).color(R, G, B, alpha).endVertex();
            }
            if (i % 4 == 0) {
                buf.pos(x0, feetY, z0).color(R, G, B, 0.85F).endVertex();
                buf.pos(x0, feetY + 3.0D, z0).color(R, G, B, 0.35F).endVertex();
            }
        }
        tess.draw();
    }
}
