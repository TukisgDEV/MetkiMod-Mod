package com.metkimod.render;

import com.metkimod.marker.Marker;
import com.metkimod.marker.MarkerManager;
import com.metkimod.marker.PingType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarkerRenderer {

    private static final Map<UUID, float[]> smoothedPositions = new HashMap<>();
    private static final Map<UUID, Float> smoothedDistances = new HashMap<>();
    private static final float POSITION_SMOOTH = 0.15f;
    private static final float DISTANCE_SMOOTH = 0.1f;

    public static void register() {
        WorldRenderEvents.LAST.register(MarkerRenderer::renderWorldMarkers);
        HudRenderCallback.EVENT.register(MarkerRenderer::renderHudMarkers);
    }

    private static void renderWorldMarkers(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        for (Marker marker : MarkerManager.getMarkers()) {
            float alpha = marker.getAlpha();
            if (alpha <= 0)
                continue;

            float scale = marker.getSpawnScale();
            double dx = marker.getX() - cameraPos.x;
            double dy = marker.getY() - cameraPos.y;
            double dz = marker.getZ() - cameraPos.z;

            matrices.push();
            matrices.translate(dx + 0.5, dy, dz + 0.5);

            if (scale != 1.0f) {
                matrices.scale(scale, scale, scale);
            }

            renderGroundRing(matrices, marker, alpha);
            renderBeam(matrices, marker, alpha);
            renderDiamond(matrices, marker, alpha);

            matrices.pop();
        }
    }

        private static void renderGroundRing(MatrixStack matrices, Marker marker, float alpha) {
        float r = marker.getRed();
        float g = marker.getGreen();
        float b = marker.getBlue();

        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float pulse = 0.4f + 0.2f * (float) Math.sin(time * Math.PI * 2);
        float ringAlpha = alpha * pulse;
        float radius = 0.6f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int segments = 24;
        float innerR = radius - 0.08f;
        float outerR = radius + 0.08f;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * Math.PI * 2 / segments);
            float a2 = (float) ((i + 1) * Math.PI * 2 / segments);

            float x1i = (float) Math.cos(a1) * innerR;
            float z1i = (float) Math.sin(a1) * innerR;
            float x2i = (float) Math.cos(a2) * innerR;
            float z2i = (float) Math.sin(a2) * innerR;

            float x1o = (float) Math.cos(a1) * outerR;
            float z1o = (float) Math.sin(a1) * outerR;
            float x2o = (float) Math.cos(a2) * outerR;
            float z2o = (float) Math.sin(a2) * outerR;

            buffer.vertex(matrix, x1i, 0.02f, z1i).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, x1o, 0.02f, z1o).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, x2o, 0.02f, z2o).color(r, g, b, ringAlpha);

            buffer.vertex(matrix, x1i, 0.02f, z1i).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, x2o, 0.02f, z2o).color(r, g, b, ringAlpha);
            buffer.vertex(matrix, x2i, 0.02f, z2i).color(r, g, b, ringAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderBeam(MatrixStack matrices, Marker marker, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        float r = marker.getRed();
        float g = marker.getGreen();
        float b = marker.getBlue();

        float beamHeight = 3.5f + (1.0f - marker.getProgress()) * 1.5f;
        float innerWidth = 0.04f;
        float outerWidth = 0.12f;

        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float innerAlpha = alpha * 0.8f;
        drawBeamSide(buffer, matrix, -innerWidth, 0, -innerWidth, innerWidth, beamHeight, -innerWidth, r, g, b,
                innerAlpha);
        drawBeamSide(buffer, matrix, innerWidth, 0, -innerWidth, innerWidth, beamHeight, innerWidth, r, g, b,
                innerAlpha);
        drawBeamSide(buffer, matrix, innerWidth, 0, innerWidth, -innerWidth, beamHeight, innerWidth, r, g, b,
                innerAlpha);
        drawBeamSide(buffer, matrix, -innerWidth, 0, innerWidth, -innerWidth, beamHeight, -innerWidth, r, g, b,
                innerAlpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float outerAlpha = alpha * 0.25f;
        drawBeamSide(buffer, matrix, -outerWidth, 0, -outerWidth, outerWidth, beamHeight, -outerWidth, r, g, b,
                outerAlpha);
        drawBeamSide(buffer, matrix, outerWidth, 0, -outerWidth, outerWidth, beamHeight, outerWidth, r, g, b,
                outerAlpha);
        drawBeamSide(buffer, matrix, outerWidth, 0, outerWidth, -outerWidth, beamHeight, outerWidth, r, g, b,
                outerAlpha);
        drawBeamSide(buffer, matrix, -outerWidth, 0, outerWidth, -outerWidth, beamHeight, -outerWidth, r, g, b,
                outerAlpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawBeamSide(BufferBuilder buffer, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a);
    }

    private static void renderDiamond(MatrixStack matrices, Marker marker, float alpha) {
        float beamHeight = 3.5f + (1.0f - marker.getProgress()) * 1.5f;
        float time = (System.currentTimeMillis() % 4000) / 4000.0f;
        float rotation = time * 360.0f;
        float bob = (float) Math.sin(time * Math.PI * 2) * 0.12f;

        matrices.push();
        matrices.translate(0, beamHeight + 0.25f + bob, 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(0.2f, 0.3f, 0.2f);

        float r = marker.getRed();
        float g = marker.getGreen();
        float b = marker.getBlue();
        float dAlpha = alpha * 0.9f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        drawTriangle(buffer, matrix, 0, 1, 0, -1, 0, 0, 0, 0, -1, r, g, b, dAlpha, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, 1, 0, 0, 0, -1, 1, 0, 0, r, g, b, dAlpha, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, 1, 0, 1, 0, 0, 0, 0, 1, r, g, b, dAlpha, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, 1, 0, 0, 0, 1, -1, 0, 0, r, g, b, dAlpha, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, -1, 0, 0, 0, -1, -1, 0, 0, r, g, b, dAlpha * 0.5f, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, -1, 0, 1, 0, 0, 0, 0, -1, r, g, b, dAlpha * 0.5f, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, -1, 0, 0, 0, 1, 1, 0, 0, r, g, b, dAlpha * 0.5f, dAlpha * 0.7f);
        drawTriangle(buffer, matrix, 0, -1, 0, -1, 0, 0, 0, 0, 1, r, g, b, dAlpha * 0.5f, dAlpha * 0.7f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void drawTriangle(BufferBuilder buffer, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r, float g, float b,
            float topAlpha, float baseAlpha) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, topAlpha);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, baseAlpha);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, baseAlpha);
    }

    private static void renderHudMarkers(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;
        if (client.options.hudHidden)
            return;

        float tickDelta = tickCounter.getTickDelta(false);

        double playerX = MathHelper.lerp(tickDelta, client.player.prevX, client.player.getX());
        double playerY = MathHelper.lerp(tickDelta, client.player.prevY, client.player.getY());
        double playerZ = MathHelper.lerp(tickDelta, client.player.prevZ, client.player.getZ());
        float playerYaw = MathHelper.lerp(tickDelta, client.player.prevYaw, client.player.getYaw());
        float playerPitch = MathHelper.lerp(tickDelta, client.player.prevPitch, client.player.getPitch());

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        smoothedPositions.keySet()
                .removeIf(id -> MarkerManager.getMarkers().stream().noneMatch(m -> m.getId().equals(id)));
        smoothedDistances.keySet()
                .removeIf(id -> MarkerManager.getMarkers().stream().noneMatch(m -> m.getId().equals(id)));

        for (Marker marker : MarkerManager.getMarkers()) {
            float alpha = marker.getAlpha();
            if (alpha <= 0.01f)
                continue;

            double rawDistance = marker.distanceTo(playerX, playerY, playerZ);
            Float prevDist = smoothedDistances.get(marker.getId());
            float smoothedDist = prevDist == null ? (float) rawDistance
                    : MathHelper.lerp(DISTANCE_SMOOTH, prevDist, (float) rawDistance);
            smoothedDistances.put(marker.getId(), smoothedDist);

            String distText = String.format("%.0f m", smoothedDist);
            double dx = marker.getX() + 0.5 - playerX;
            double dz = marker.getZ() + 0.5 - playerZ;
            double angleToMarker = Math.atan2(-dx, dz);
            double yawRad = Math.toRadians(playerYaw);
            double relativeAngle = normalizeAngle(angleToMarker - yawRad);

            double dy = (marker.getY() + 3.5) - (playerY + client.player.getStandingEyeHeight());
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double verticalAngle = Math.atan2(dy, horizontalDist);
            double pitchRad = Math.toRadians(-playerPitch);
            double relativeVertAngle = verticalAngle - pitchRad;

            double fov = Math.toRadians(client.options.getFov().getValue());
            double aspectRatio = (double) screenWidth / screenHeight;
            double hFov = fov * aspectRatio * 0.5;
            double vFov = fov * 0.5;

            boolean onScreen = Math.abs(relativeAngle) < hFov && Math.abs(relativeVertAngle) < vFov;

            int markerColor = marker.getColor();
            int cr = (markerColor >> 16) & 0xFF;
            int cg = (markerColor >> 8) & 0xFF;
            int cb = markerColor & 0xFF;
            int alphaInt = (int) (alpha * 255);

            float rawScreenX, rawScreenY;
            if (onScreen) {
                rawScreenX = (float) (screenWidth / 2.0 + (relativeAngle / hFov) * (screenWidth / 2.0));
                rawScreenY = (float) (screenHeight / 2.0 - (relativeVertAngle / vFov) * (screenHeight / 2.0));
            } else {
                double dirX = Math.sin(relativeAngle);
                double dirY = -Math.sin(relativeVertAngle);
                int margin = 30;
                double halfW = screenWidth / 2.0 - margin;
                double halfH = screenHeight / 2.0 - margin;
                double absX = Math.abs(dirX);
                double absY = Math.abs(dirY);
                double sc = (absX / halfW > absY / halfH)
                        ? halfW / Math.max(absX, 0.001)
                        : halfH / Math.max(absY, 0.001);
                rawScreenX = (float) MathHelper.clamp(screenWidth / 2.0 + dirX * sc, margin, screenWidth - margin);
                rawScreenY = (float) MathHelper.clamp(screenHeight / 2.0 + dirY * sc, margin, screenHeight - margin);
            }

            float[] cached = smoothedPositions.computeIfAbsent(marker.getId(),
                    k -> new float[] { rawScreenX, rawScreenY });
            cached[0] = MathHelper.lerp(POSITION_SMOOTH, cached[0], rawScreenX);
            cached[1] = MathHelper.lerp(POSITION_SMOOTH, cached[1], rawScreenY);

            int sx = Math.round(cached[0]);
            int sy = Math.round(cached[1]);

            renderMarkerBadge(drawContext, client.textRenderer, sx, sy,
                    marker, distText, cr, cg, cb, alphaInt, onScreen);
        }
    }

        private static void renderMarkerBadge(DrawContext ctx, TextRenderer textRenderer,
            int x, int y, Marker marker,
            String distText,
            int r, int g, int b, int alpha,
            boolean onScreen) {
        int color = (alpha << 24) | (r << 16) | (g << 8) | b;
        int bgAlpha = Math.max(0, alpha - 60);
        int bgColor = (bgAlpha << 24) | 0x111111;
        int borderColor = (alpha << 24) | (Math.min(255, r + 40) << 16) | (Math.min(255, g + 40) << 8)
                | Math.min(255, b + 40);
        String messageText = null;
        if (marker.getPingType() != PingType.DEFAULT) {
            messageText = marker.getPingType().getIcon() + " " + marker.getMessage();
        }

        String nameText = marker.getOwnerName();
        int maxWidth = textRenderer.getWidth(distText);
        if (messageText != null)
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(messageText));
        maxWidth = Math.max(maxWidth, textRenderer.getWidth(nameText));

        int badgeWidth = maxWidth + 12;
        int badgeHeight = (messageText != null) ? 38 : 26;
        int bx = x - badgeWidth / 2;
        int by = y - badgeHeight - 8;
        ctx.fill(bx, by, bx + badgeWidth, by + badgeHeight, bgColor);
        ctx.fill(bx, by, bx + badgeWidth, by + 2, borderColor);
        ctx.fill(bx, by, bx + 2, by + badgeHeight, borderColor);

        int textY = by + 4;
        if (messageText != null) {
            int msgWidth = textRenderer.getWidth(messageText);
            int msgColor = marker.getPingType().getDefaultColor();
            ctx.drawTextWithShadow(textRenderer, messageText,
                    x - msgWidth / 2, textY, (alpha << 24) | (msgColor & 0xFFFFFF));
            textY += 12;
        }
        int nameWidth = textRenderer.getWidth(nameText);
        ctx.drawTextWithShadow(textRenderer, nameText, x - nameWidth / 2, textY, color);
        textY += 12;
        int distWidth = textRenderer.getWidth(distText);
        int distColor = (alpha << 24) | 0xAAAAAA;
        ctx.drawTextWithShadow(textRenderer, distText, x - distWidth / 2, textY, distColor);
        if (onScreen) {
            drawDiamondIcon(ctx, x, y, 5, color);
        } else {
            drawDiamondIcon(ctx, x, y, 4, color);
        }
    }

    private static void drawDiamondIcon(DrawContext ctx, int centerX, int centerY, int size, int color) {
        for (int i = 0; i <= size; i++) {
            ctx.fill(centerX - i, centerY - size + i, centerX + i + 1, centerY - size + i + 1, color);
            ctx.fill(centerX - i, centerY + size - i, centerX + i + 1, centerY + size - i + 1, color);
        }
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        return angle;
    }
}
