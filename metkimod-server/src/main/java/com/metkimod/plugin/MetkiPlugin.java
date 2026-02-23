package com.metkimod.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.IOException;

/**
 * @author Tukisg
 */
public class MetkiPlugin extends JavaPlugin implements PluginMessageListener, CommandExecutor, Listener {

    private static final String CHANNEL_PLACE = "metkimod:place_marker";
    private static final String CHANNEL_SYNC = "metkimod:sync_marker";

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL_PLACE, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL_SYNC);

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("metki") != null) {
            getCommand("metki").setExecutor(this);
        }

        getLogger().info("MetkiMod Companion Plugin enabled! Listening for tactical pings.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MetkiMod Companion Plugin disabled.");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL_PLACE))
            return;

        try {
            PacketUtil.PlaceMarkerData data = PacketUtil.parsePlaceMarker(message);

            String groupName = GroupManager.getPlayerGroup(player.getUniqueId());
            if (groupName == null)
                return;

            GroupManager.MarkerGroup group = GroupManager.getGroup(groupName);
            if (group == null)
                return;

            int senderColor = group.getMemberColor(player.getUniqueId());

            byte[] syncData = PacketUtil.buildSyncMarker(
                    data.x(), data.y(), data.z(),
                    senderColor, player.getName(), data.pingTypeOrdinal());

            for (java.util.UUID memberId : group.getMembers()) {
                if (memberId.equals(player.getUniqueId()))
                    continue;

                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendPluginMessage(this, CHANNEL_SYNC, syncData);
                }
            }

        } catch (IOException e) {
            getLogger().warning("Failed to parse marker packet from " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GroupManager.onPlayerDisconnect(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(ChatColor.GOLD + "═══ MetkiMod - Помощь ═══");
            player.sendMessage(ChatColor.AQUA + "/metki group create <имя> <пароль>");
            player.sendMessage(ChatColor.AQUA + "/metki group join <имя> <пароль>");
            player.sendMessage(ChatColor.AQUA + "/metki group leave");
            player.sendMessage(ChatColor.AQUA + "/metki group info");
            return true;
        }

        if (args[0].equalsIgnoreCase("group")) {
            if (args.length < 2)
                return false;

            String subCommand = args[1];

            if (subCommand.equalsIgnoreCase("create") && args.length >= 4) {
                String name = args[2];
                String password = args[3];
                if (GroupManager.createGroup(name, password, player.getUniqueId())) {
                    player.sendMessage(
                            ChatColor.GREEN + "✔ Группа " + ChatColor.GOLD + name + ChatColor.GREEN + " создана!");
                } else {
                    player.sendMessage(ChatColor.RED + "✘ Группа уже существует!");
                }
                return true;
            }

            if (subCommand.equalsIgnoreCase("join") && args.length >= 4) {
                String name = args[2];
                String password = args[3];
                int result = GroupManager.joinGroup(name, password, player.getUniqueId());
                if (result == 0) {
                    player.sendMessage(
                            ChatColor.GREEN + "✔ Вы вошли в группу " + ChatColor.GOLD + name + ChatColor.GREEN + "!");
                } else if (result == 1) {
                    player.sendMessage(ChatColor.RED + "✘ Группа не найдена!");
                } else {
                    player.sendMessage(ChatColor.RED + "✘ Неверный пароль!");
                }
                return true;
            }

            if (subCommand.equalsIgnoreCase("leave")) {
                String currentGroup = GroupManager.getPlayerGroup(player.getUniqueId());
                if (currentGroup != null) {
                    GroupManager.leaveGroup(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "✔ Вы вышли из группы " + ChatColor.GOLD + currentGroup);
                } else {
                    player.sendMessage(ChatColor.RED + "✘ Вы не в группе!");
                }
                return true;
            }

            if (subCommand.equalsIgnoreCase("info")) {
                String groupName = GroupManager.getPlayerGroup(player.getUniqueId());
                if (groupName != null) {
                    GroupManager.MarkerGroup group = GroupManager.getGroup(groupName);
                    int count = group != null ? group.getMemberCount() : 0;
                    player.sendMessage(ChatColor.GOLD + "═══ Инфо о группе ═══");
                    player.sendMessage(ChatColor.GRAY + "Имя: " + ChatColor.WHITE + groupName);
                    player.sendMessage(ChatColor.GRAY + "Участники: " + ChatColor.GREEN + count);
                } else {
                    player.sendMessage(ChatColor.RED + "✘ Вы не в группе!");
                }
                return true;
            }
        }

        return false;
    }
}
