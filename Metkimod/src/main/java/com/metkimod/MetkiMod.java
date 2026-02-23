package com.metkimod;

import com.metkimod.group.GroupManager;
import com.metkimod.group.MarkerGroup;
import com.metkimod.network.NetworkHandler;
import com.metkimod.network.NetworkPayloads;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetkiMod implements ModInitializer {
    public static final String MOD_ID = "metkimod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int[] PRESET_COLORS = {
            0xFF4444, 
            0x44AAFF, 
            0x44FF44, 
            0xFFFF44, 
            0xFF44FF, 
            0x44FFFF, 
            0xFF8844, 
            0xFFFFFF 
    };

    @Override
    public void onInitialize() {
        LOGGER.info("[MetkiMod] Initializing ping/marker system...");
        NetworkHandler.registerPayloads();
        NetworkHandler.registerServerHandlers();
        registerCommands();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GroupManager.onPlayerDisconnect(handler.getPlayer().getUuid());
        });

        LOGGER.info("[MetkiMod] Initialized! Press V to place markers.");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("metki")
                    .then(CommandManager.literal("group")
                            .then(CommandManager.literal("create")
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .then(CommandManager.argument("password", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String name = StringArgumentType.getString(ctx, "name");
                                                        String password = StringArgumentType.getString(ctx, "password");
                                                        var player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                            return 0;

                                                        boolean success = GroupManager.createGroup(name, password,
                                                                player.getUuid());
                                                        if (success) {
                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.literal("✔ Группа ")
                                                                            .formatted(Formatting.GREEN)
                                                                            .append(Text.literal(name)
                                                                                    .formatted(Formatting.GOLD))
                                                                            .append(Text.literal(" создана! Пароль: ")
                                                                                    .formatted(Formatting.GREEN))
                                                                            .append(Text.literal(password)
                                                                                    .formatted(Formatting.YELLOW)),
                                                                    false);
                                                        } else {
                                                            ctx.getSource().sendError(
                                                                    Text.literal("✘ Группа с именем '" + name
                                                                            + "' уже существует!")
                                                                            .formatted(Formatting.RED));
                                                        }
                                                        return 1;
                                                    }))))
                            .then(CommandManager.literal("join")
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .then(CommandManager.argument("password", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String name = StringArgumentType.getString(ctx, "name");
                                                        String password = StringArgumentType.getString(ctx, "password");
                                                        var player = ctx.getSource().getPlayer();
                                                        if (player == null)
                                                            return 0;

                                                        int result = GroupManager.joinGroup(name, password,
                                                                player.getUuid());
                                                        switch (result) {
                                                            case 0 -> ctx.getSource().sendFeedback(
                                                                    () -> Text.literal("✔ Вы вошли в группу ")
                                                                            .formatted(Formatting.GREEN)
                                                                            .append(Text.literal(name)
                                                                                    .formatted(Formatting.GOLD))
                                                                            .append(Text.literal(
                                                                                    "! Метки будут видны всем участникам.")
                                                                                    .formatted(Formatting.GREEN)),
                                                                    false);
                                                            case 1 -> ctx.getSource().sendError(
                                                                    Text.literal("✘ Группа '" + name + "' не найдена!")
                                                                            .formatted(Formatting.RED));
                                                            case 2 -> ctx.getSource().sendError(
                                                                    Text.literal("✘ Неверный пароль!")
                                                                            .formatted(Formatting.RED));
                                                        }
                                                        return 1;
                                                    }))))
                            .then(CommandManager.literal("leave")
                                    .executes(ctx -> {
                                        var player = ctx.getSource().getPlayer();
                                        if (player == null)
                                            return 0;

                                        String currentGroup = GroupManager.getPlayerGroup(player.getUuid());
                                        if (currentGroup != null) {
                                            GroupManager.leaveGroup(player.getUuid());
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("✔ Вы вышли из группы ")
                                                            .formatted(Formatting.YELLOW)
                                                            .append(Text.literal(currentGroup)
                                                                    .formatted(Formatting.GOLD)),
                                                    false);
                                        } else {
                                            ctx.getSource().sendError(
                                                    Text.literal("✘ Вы не состоите ни в одной группе!")
                                                            .formatted(Formatting.RED));
                                        }
                                        return 1;
                                    }))
                            .then(CommandManager.literal("info")
                                    .executes(ctx -> {
                                        var player = ctx.getSource().getPlayer();
                                        if (player == null)
                                            return 0;

                                        String groupName = GroupManager.getPlayerGroup(player.getUuid());
                                        if (groupName != null) {
                                            MarkerGroup group = GroupManager.getGroup(groupName);
                                            int count = group != null ? group.getMemberCount() : 0;
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("═══ Информация о группе ═══\n")
                                                            .formatted(Formatting.GOLD)
                                                            .append(Text.literal("  Название: ")
                                                                    .formatted(Formatting.GRAY))
                                                            .append(Text.literal(groupName + "\n")
                                                                    .formatted(Formatting.WHITE))
                                                            .append(Text.literal("  Участники: ")
                                                                    .formatted(Formatting.GRAY))
                                                            .append(Text.literal(count + " онлайн\n")
                                                                    .formatted(Formatting.GREEN))
                                                            .append(Text.literal("══════════════════════")
                                                                    .formatted(Formatting.GOLD)),
                                                    false);
                                        } else {
                                            ctx.getSource().sendError(
                                                    Text.literal("✘ Вы не состоите ни в одной группе!\n")
                                                            .formatted(Formatting.RED)
                                                            .append(Text.literal(
                                                                    "  Используйте: /metki group create <имя> <пароль>")
                                                                    .formatted(Formatting.GRAY)));
                                        }
                                        return 1;
                                    })))
                    .then(CommandManager.literal("help")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("═══ MetkiMod - Помощь ═══\n")
                                                .formatted(Formatting.GOLD)
                                                .append(Text.literal("  [V] ").formatted(Formatting.YELLOW))
                                                .append(Text.literal("- Поставить метку\n").formatted(Formatting.WHITE))
                                                .append(Text.literal("  /metki group create <имя> <пароль>")
                                                        .formatted(Formatting.AQUA))
                                                .append(Text.literal("\n    - Создать группу\n")
                                                        .formatted(Formatting.GRAY))
                                                .append(Text.literal("  /metki group join <имя> <пароль>")
                                                        .formatted(Formatting.AQUA))
                                                .append(Text.literal("\n    - Войти в группу\n")
                                                        .formatted(Formatting.GRAY))
                                                .append(Text.literal("  /metki group leave").formatted(Formatting.AQUA))
                                                .append(Text.literal("\n    - Выйти из группы\n")
                                                        .formatted(Formatting.GRAY))
                                                .append(Text.literal("  /metki group info").formatted(Formatting.AQUA))
                                                .append(Text.literal("\n    - Инфо о группе\n")
                                                        .formatted(Formatting.GRAY))
                                                .append(Text.literal("══════════════════════")
                                                        .formatted(Formatting.GOLD)),
                                        false);
                                return 1;
                            }))
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(
                                () -> Text.literal("[MetkiMod] ")
                                        .formatted(Formatting.GOLD)
                                        .append(Text.literal("Используйте /metki help для справки")
                                                .formatted(Formatting.YELLOW)),
                                false);
                        return 1;
                    }));
        });
    }
}
