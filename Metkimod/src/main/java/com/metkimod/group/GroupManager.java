package com.metkimod.group;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {
    private static final Map<String, MarkerGroup> groups = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();

    public static boolean createGroup(String name, String password, UUID creator) {
        if (groups.containsKey(name.toLowerCase())) {
            return false;
        }
        MarkerGroup group = new MarkerGroup(name.toLowerCase(), password, creator);
        groups.put(name.toLowerCase(), group);
        playerGroups.put(creator, name.toLowerCase());
        return true;
    }

    public static int joinGroup(String name, String password, UUID player) {
        if (playerGroups.containsKey(player)) {
            leaveGroup(player);
        }

        MarkerGroup group = groups.get(name.toLowerCase());
        if (group == null)
            return 1;
        if (!group.checkPassword(password))
            return 2;

        group.addMember(player);
        playerGroups.put(player, name.toLowerCase());
        return 0;
    }

    public static void leaveGroup(UUID player) {
        String groupName = playerGroups.remove(player);
        if (groupName != null) {
            MarkerGroup group = groups.get(groupName);
            if (group != null) {
                group.removeMember(player);
                if (group.isEmpty()) {
                    groups.remove(groupName);
                }
            }
        }
    }

    public static String getPlayerGroup(UUID player) {
        return playerGroups.get(player);
    }

    public static MarkerGroup getGroup(String name) {
        return groups.get(name.toLowerCase());
    }

        public static int getPlayerColor(UUID player) {
        String groupName = playerGroups.get(player);
        if (groupName == null)
            return 0xFFFFFF;
        MarkerGroup group = groups.get(groupName);
        if (group == null)
            return 0xFFFFFF;
        return group.getMemberColor(player);
    }

    public static List<ServerPlayerEntity> getGroupMembersExcluding(UUID player, MinecraftServer server) {
        String groupName = playerGroups.get(player);
        if (groupName == null)
            return Collections.emptyList();

        MarkerGroup group = groups.get(groupName);
        if (group == null)
            return Collections.emptyList();

        List<ServerPlayerEntity> result = new ArrayList<>();
        for (UUID memberId : group.getMembers()) {
            if (!memberId.equals(player)) {
                ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                if (member != null) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    public static void onPlayerDisconnect(UUID player) {
        leaveGroup(player);
    }

    public static void clear() {
        groups.clear();
        playerGroups.clear();
    }
}
