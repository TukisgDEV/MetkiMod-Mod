package com.metkimod.gui;

import com.metkimod.MetkiMod;
import com.metkimod.MetkiModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GroupScreen extends Screen {

    public static String currentGroupName = null;
    public static boolean inGroup = false;

    private TextFieldWidget nameField;
    private TextFieldWidget passwordField;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private long statusTime = 0;

    public GroupScreen() {
        super(Text.literal("MetkiMod - Группы и Настройки"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (!inGroup) {
            nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 60, 200, 20,
                    Text.literal("Имя группы"));
            nameField.setPlaceholder(Text.literal("Имя группы...").formatted(Formatting.DARK_GRAY));
            nameField.setMaxLength(32);
            addDrawableChild(nameField);
            passwordField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 30, 200, 20,
                    Text.literal("Пароль"));
            passwordField.setPlaceholder(Text.literal("Пароль...").formatted(Formatting.DARK_GRAY));
            passwordField.setMaxLength(32);
            addDrawableChild(passwordField);
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("✚ Создать группу").formatted(Formatting.GREEN),
                    button -> onCreateGroup())
                    .dimensions(centerX - 100, centerY, 95, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("➤ Войти").formatted(Formatting.AQUA),
                    button -> onJoinGroup())
                    .dimensions(centerX + 5, centerY, 95, 20)
                    .build());
        } else {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("✘ Покинуть группу").formatted(Formatting.RED),
                    button -> onLeaveGroup())
                    .dimensions(centerX - 75, centerY, 150, 20)
                    .build());
        }
        addDrawableChild(ButtonWidget.builder(
                Text.literal("🎨 Сменить цвет"),
                button -> {
                    MetkiModClient.cycleSoloColor();
                    setStatus("Цвет одиночной метки изменён!", 0x44AAFF);
                }).dimensions(centerX - 50, centerY + 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Закрыть"),
                button -> close())
                .dimensions(centerX - 50, centerY + 65, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        String title = "═══ MetkiMod ═══";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer, title,
                centerX - titleWidth / 2, centerY - 100, 0xFFD700);

        if (!inGroup) {
            context.drawTextWithShadow(this.textRenderer, "Создай группу для тиммейтов:",
                    centerX - 100, centerY - 72, 0xAAAAAA);
        } else {
            String groupInfo = "Вы в группе: ";
            int infoW = this.textRenderer.getWidth(groupInfo + currentGroupName);
            context.drawTextWithShadow(this.textRenderer, groupInfo,
                    centerX - infoW / 2, centerY - 40, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, currentGroupName,
                    centerX - infoW / 2 + this.textRenderer.getWidth(groupInfo),
                    centerY - 40, 0x55FF55);

            String hint = "Метки видны всем участникам!";
            int hintW = this.textRenderer.getWidth(hint);
            context.drawTextWithShadow(this.textRenderer, hint,
                    centerX - hintW / 2, centerY - 20, 0xFFFF55);
        }
        String colorText = "Твой цвет: ";
        int clrW = this.textRenderer.getWidth(colorText);
        context.drawTextWithShadow(this.textRenderer, colorText, centerX - clrW / 2 - 10, centerY + 54, 0xAAAAAA);
        int color = MetkiModClient.soloColor | 0xFF000000;
        context.fill(centerX + clrW / 2 - 5, centerY + 53, centerX + clrW / 2 + 5, centerY + 63, color);
        context.drawBorder(centerX + clrW / 2 - 6, centerY + 52, 12, 12, 0xFFFFFFFF);
        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            int sw = this.textRenderer.getWidth(statusMessage);
            float fadeAlpha = Math.min(1.0f, (3000 - (System.currentTimeMillis() - statusTime)) / 1000.0f);
            int alpha = (int) (fadeAlpha * 255) << 24;
            context.drawTextWithShadow(this.textRenderer, statusMessage,
                    centerX - sw / 2, centerY + 90, statusColor | alpha);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void onCreateGroup() {
        String name = nameField.getText().trim();
        String password = passwordField.getText().trim();
        handleGroupCommand("metki group create " + name + " " + password, name);
    }

    private void onJoinGroup() {
        String name = nameField.getText().trim();
        String password = passwordField.getText().trim();
        handleGroupCommand("metki group join " + name + " " + password, name);
    }

    private void handleGroupCommand(String cmd, String name) {
        if (name.isEmpty())
            return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand(cmd);
            currentGroupName = name;
            inGroup = true;
            setStatus("Обновление группы...", 0x55FF55);
            clearAndInit();
        }
    }

    private void onLeaveGroup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("metki group leave");
            currentGroupName = null;
            inGroup = false;
            setStatus("Группа покинута.", 0xFFFF55);
            clearAndInit();
        }
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
