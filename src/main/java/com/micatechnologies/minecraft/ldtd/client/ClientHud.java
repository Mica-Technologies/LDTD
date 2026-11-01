package com.micatechnologies.minecraft.ldtd.client;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.gloam.EntityEclipse;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Small client-only aids (Phase 10): help lines on LDTD items, and health bars over wounded Gloam.
 * Reached only through {@code LdtdClientProxy}.
 */
public class ClientHud {

    /**
     * Adds an item's help lines, from the lang keys {@code <translation key>.tip1}, {@code .tip2},
     * and so on. Shift shows them all; otherwise only the first.
     */
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null
            || !LdtdConstants.MOD_NAMESPACE.equals(stack.getItem().getRegistryName().getNamespace())) {
            return;
        }
        String key = stack.getItem().getTranslationKey(stack);
        if (!I18n.hasKey(key + ".tip1")) {
            return;
        }
        boolean all = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        for (int i = 1; I18n.hasKey(key + ".tip" + i); i++) {
            if (i > 1 && !all) {
                event.getToolTip().add(TextFormatting.DARK_GRAY + I18n.format("ldtd.tooltip.more"));
                break;
            }
            event.getToolTip().add(TextFormatting.GRAY + I18n.format(key + ".tip" + i));
        }
    }

    /** A thin health bar over a wounded Gloam within 32 blocks. The Eclipse has its boss bar. */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<?> event) {
        if (!LdtdConfig.healthBars || !(event.getEntity() instanceof EntityGloam)
            || event.getEntity() instanceof EntityEclipse) {
            return;
        }
        EntityGloam g = (EntityGloam) event.getEntity();
        float max = g.getMaxHealth();
        float hp = g.getHealth();
        if (hp >= max || hp <= 0.0F || g.getDistanceSq(Minecraft.getMinecraft().player) > 32 * 32) {
            return;
        }
        float frac = hp / max;
        GlStateManager.pushMatrix();
        // Above the drawn model, which may be bigger than the hitbox.
        float drawn = event.getRenderer() instanceof com.micatechnologies.minecraft.ldtd.client.render.RenderGloam
            ? ((com.micatechnologies.minecraft.ldtd.client.render.RenderGloam<?>) event.getRenderer()).scale() : 1.0F;
        GlStateManager.translate(event.getX(), event.getY() + g.height * drawn + 0.3D, event.getZ());
        GlStateManager.rotate(-event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(event.getRenderer().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.025F, -0.025F, 0.025F);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        quad(buf, -20, 0, 20, 5, 0.05F, 0.03F, 0.08F, 0.8F);
        quad(buf, -19, 1, -19 + 38 * frac, 4, 0.36F + 0.6F * (1 - frac), 0.3F + 0.6F * frac, 0.95F * frac + 0.2F, 1.0F);
        tess.draw();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void quad(BufferBuilder buf, double x0, double y0, double x1, double y1, float r, float g, float b,
                             float a) {
        buf.pos(x0, y0, 0).color(r, g, b, a).endVertex();
        buf.pos(x0, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buf.pos(x1, y0, 0).color(r, g, b, a).endVertex();
    }
}
