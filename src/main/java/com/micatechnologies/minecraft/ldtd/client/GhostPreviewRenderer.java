package com.micatechnologies.minecraft.ldtd.client;

import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.tower.BlueprintPlacer;
import com.micatechnologies.minecraft.ldtd.tower.ItemBlueprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * While a player holds a blueprint and looks at a block, shows where the structure would go:
 * a translucent box per cell, green where it can build, red where something is in the way, cyan for
 * the tower core. Uses the same anchor and rotation rules as the build itself
 * ({@link ItemBlueprint#anchor}, {@link ItemBlueprint#rotation}), so the preview never lies.
 *
 * <p>Whether a cell is clear is the client's view of the world. The server checks again (territory,
 * trust, cost) when the player clicks.</p>
 */
public final class GhostPreviewRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }
        ItemStack held = player.getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemBlueprint)) {
            return;
        }
        Blueprint bp = LdtdData.blueprints().get(ItemBlueprint.id(held));
        RayTraceResult hit = mc.objectMouseOver;
        if (bp == null || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos anchor = ItemBlueprint.anchor(mc.world, hit.getBlockPos(), hit.sideHit);
        int rotation = ItemBlueprint.rotation(player);

        float pt = event.getPartialTicks();
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (Blueprint.Cell c : bp.cells(rotation)) {
            BlockPos p = anchor.add(c.dx, c.dy, c.dz);
            boolean clear = BlueprintPlacer.clear(mc.world, p);
            float r;
            float g;
            float b;
            float a;
            if (!clear) {
                r = 1.0F; g = 0.25F; b = 0.2F; a = 0.35F;
            } else if (c.key == Blueprint.CORE) {
                r = 0.36F; g = 0.95F; b = 0.9F; a = 0.45F;
            } else if (c.key == Blueprint.AIR) {
                continue;
            } else {
                r = 0.4F; g = 1.0F; b = 0.45F; a = 0.22F;
            }
            cube(buf, p.getX() - camX, p.getY() - camY, p.getZ() - camZ, r, g, b, a);
        }
        tess.draw();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** A slightly shrunk cube, so the boxes of neighbouring cells don't z-fight. */
    private static void cube(BufferBuilder buf, double x, double y, double z, float r, float g, float b, float a) {
        double e = 0.02D;
        double x0 = x + e;
        double y0 = y + e;
        double z0 = z + e;
        double x1 = x + 1 - e;
        double y1 = y + 1 - e;
        double z1 = z + 1 - e;
        quad(buf, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        quad(buf, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(buf, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(buf, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(buf, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(buf, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void quad(BufferBuilder buf, double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float r, float g, float b, float a) {
        buf.pos(ax, ay, az).color(r, g, b, a).endVertex();
        buf.pos(bx, by, bz).color(r, g, b, a).endVertex();
        buf.pos(cx, cy, cz).color(r, g, b, a).endVertex();
        buf.pos(dx, dy, dz).color(r, g, b, a).endVertex();
    }
}
