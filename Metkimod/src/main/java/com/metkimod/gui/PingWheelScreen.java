package com.metkimod.gui;

import com.metkimod.MetkiModClient;
import com.metkimod.marker.PingType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class PingWheelScreen extends Screen {

    private static final PingType[] WHEEL_TYPES = {
            PingType.ENEMY,
            PingType.TRAP,
            PingType.DANGER,
            PingType.CHASING,
            PingType.HELP,
            PingType.FULL_STACK,
            PingType.GO_HERE,
            PingType.DEFAULT
    };

    private static final float OUTER_RADIUS = 110;
    private static final float INNER_RADIUS = 35;
    private static final float ICON_RADIUS = 72;

    private int selectedIndex = -1;

    public PingWheelScreen() {
        super(Text.literal("Ping Wheel"));
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        context.fillGradient(0, 0, this.width, this.height, 0x88000000, 0x88000000);
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > INNER_RADIUS) {
            double angle = Math.atan2(dy, dx);
            double normalizedAngle = (angle + Math.PI * 2.5) % (Math.PI * 2);
            double segmentAngle = Math.PI * 2.0 / WHEEL_TYPES.length;
            selectedIndex = (int) (normalizedAngle / segmentAngle) % WHEEL_TYPES.length;
        } else {
            selectedIndex = -1;
        }
        drawWheelSegments(context, cx, cy);
        drawCenterCircle(context, cx, cy);
        drawSegmentLabels(context, cx, cy);
        String hint = "Отпусти для выбора";
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawTextWithShadow(this.textRenderer, hint,
                cx - hintWidth / 2, cy + (int) OUTER_RADIUS + 20, 0x888888);
    }

    private void drawWheelSegments(DrawContext context, int cx, int cy) {
        int count = WHEEL_TYPES.length;
        double segmentAngle = Math.PI * 2.0 / count;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        for (int i = 0; i < count; i++) {
            boolean isSelected = (i == selectedIndex);
            PingType type = WHEEL_TYPES[i];

            float startAngle = (float) (i * segmentAngle - Math.PI / 2);
            float endAngle = (float) ((i + 1) * segmentAngle - Math.PI / 2);
            int baseColor = type.getDefaultColor();
            float br = ((baseColor >> 16) & 0xFF) / 255f;
            float bg = ((baseColor >> 8) & 0xFF) / 255f;
            float bb = (baseColor & 0xFF) / 255f;

            float bgAlpha = isSelected ? 0.45f : 0.18f;
            float outerR = isSelected ? OUTER_RADIUS + 8 : OUTER_RADIUS;

            drawArcSegment(context, cx, cy, INNER_RADIUS, outerR,
                    startAngle, endAngle, br, bg, bb, bgAlpha);
            float sepX1 = cx + (float) Math.cos(startAngle) * INNER_RADIUS;
            float sepY1 = cy + (float) Math.sin(startAngle) * INNER_RADIUS;
            float sepX2 = cx + (float) Math.cos(startAngle) * outerR;
            float sepY2 = cy + (float) Math.sin(startAngle) * outerR;
            drawLine(context, sepX1, sepY1, sepX2, sepY2, 0.3f, 0.3f, 0.3f, 0.5f);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawArcSegment(DrawContext context, int cx, int cy,
            float innerR, float outerR,
            float startAngle, float endAngle,
            float r, float g, float b, float a) {
        int steps = 20;
        float angleStep = (endAngle - startAngle) / steps;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        for (int i = 0; i < steps; i++) {
            float a1 = startAngle + i * angleStep;
            float a2 = startAngle + (i + 1) * angleStep;

            float x1i = cx + (float) Math.cos(a1) * innerR;
            float y1i = cy + (float) Math.sin(a1) * innerR;
            float x2i = cx + (float) Math.cos(a2) * innerR;
            float y2i = cy + (float) Math.sin(a2) * innerR;

            float x1o = cx + (float) Math.cos(a1) * outerR;
            float y1o = cy + (float) Math.sin(a1) * outerR;
            float x2o = cx + (float) Math.cos(a2) * outerR;
            float y2o = cy + (float) Math.sin(a2) * outerR;
            buffer.vertex(matrix, x1i, y1i, 0).color(r, g, b, a);
            buffer.vertex(matrix, x1o, y1o, 0).color(r, g, b, a);
            buffer.vertex(matrix, x2o, y2o, 0).color(r, g, b, a);
            buffer.vertex(matrix, x1i, y1i, 0).color(r, g, b, a);
            buffer.vertex(matrix, x2o, y2o, 0).color(r, g, b, a);
            buffer.vertex(matrix, x2i, y2i, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawLine(DrawContext context, float x1, float y1, float x2, float y2,
            float r, float g, float b, float a) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawCenterCircle(DrawContext context, int cx, int cy) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        buffer.vertex(matrix, cx, cy, 0).color(0.08f, 0.08f, 0.08f, 0.85f);

        int circleSteps = 32;
        for (int i = 0; i <= circleSteps; i++) {
            float angle = (float) (i * Math.PI * 2 / circleSteps);
            float x = cx + (float) Math.cos(angle) * INNER_RADIUS;
            float y = cy + (float) Math.sin(angle) * INNER_RADIUS;
            buffer.vertex(matrix, x, y, 0).color(0.15f, 0.15f, 0.15f, 0.85f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        if (selectedIndex >= 0 && selectedIndex < WHEEL_TYPES.length) {
            PingType type = WHEEL_TYPES[selectedIndex];
            String name = type.getMessage();
            int nameWidth = this.textRenderer.getWidth(name);
            int color = type.getDefaultColor() | 0xFF000000;
            context.drawTextWithShadow(this.textRenderer, name,
                    cx - nameWidth / 2, cy - 4, color);
        } else {
            String txt = "Выбери пинг";
            int w = this.textRenderer.getWidth(txt);
            context.drawTextWithShadow(this.textRenderer, txt, cx - w / 2, cy - 4, 0x888888);
        }
    }

    private void drawSegmentLabels(DrawContext context, int cx, int cy) {
        int count = WHEEL_TYPES.length;
        double segmentAngle = Math.PI * 2.0 / count;

        for (int i = 0; i < count; i++) {
            PingType type = WHEEL_TYPES[i];
            boolean isSelected = (i == selectedIndex);

            float midAngle = (float) ((i + 0.5) * segmentAngle - Math.PI / 2);
            float labelRadius = ICON_RADIUS;

            int lx = cx + (int) (Math.cos(midAngle) * labelRadius);
            int ly = cy + (int) (Math.sin(midAngle) * labelRadius);

            int color = type.getDefaultColor();
            if (!isSelected) {
                int r = ((color >> 16) & 0xFF) / 2;
                int g = ((color >> 8) & 0xFF) / 2;
                int b = (color & 0xFF) / 2;
                color = (r << 16) | (g << 8) | b;
            }
            color |= 0xFF000000;
            String icon = type.getIcon();
            int iconWidth = this.textRenderer.getWidth(icon);
            context.drawTextWithShadow(this.textRenderer, icon,
                    lx - iconWidth / 2, ly - 9, color);
            String shortLabel = type.getMessage().replace("!", "").trim();
            if (shortLabel.length() > 10)
                shortLabel = shortLabel.substring(0, 10);
            int labelWidth = this.textRenderer.getWidth(shortLabel);
            int labelColor = isSelected ? 0xFFFFFFFF : 0xAA888888;
            context.drawTextWithShadow(this.textRenderer, shortLabel,
                    lx - labelWidth / 2, ly + 2, labelColor);
        }
    }

    private void confirmAndClose() {
        if (selectedIndex >= 0 && selectedIndex < WHEEL_TYPES.length) {
            PingType selected = WHEEL_TYPES[selectedIndex];
            MetkiModClient.placeMarkerWithType(selected);
        }
        close();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MetkiModClient.placeMarkerKey.matchesKey(keyCode, scanCode)) {
            confirmAndClose();
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB) {
            confirmAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        confirmAndClose();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { 
            confirmAndClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
