package com.metkimod.plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Tukisg
 */
public class GroupManager {
    public static final int[] GROUP_COLORS = {
            0xFF4444,
            0x44AAFF,
            0x44FF44,
            0xFFFF44,
            0xFF44FF,
            0x44FFFF,
            0xFF8844,
            0xBBBBBB
    };

    private static final Map<String, MarkerGroup> groups = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerGroups = new ConcurrentHashMap<>();

    public static class MarkerGroup {
        private final String name;
        private final String password;
        private final UUID creator;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Map<UUID, Integer> memberColors = new ConcurrentHashMap<>();
        private int nextColorIndex = 0;

        public MarkerGroup(String name, String password, UUID creator) {
            this.name = name;
            this.password = password;
            this.creator = creator;
            addMember(creator);
        }

        public void addMember(UUID playerId) {
            members.add(playerId);
            if (!memberColors.containsKey(playerId)) {
                memberColors.put(playerId, GROUP_COLORS[nextColorIndex % GROUP_COLORS.length]);
                nextColorIndex++;
            }
        }

        public void removeMember(UUID playerId) {
            members.remove(playerId);
            memberColors.remove(playerId);
        }

        public boolean isEmpty() {
            return members.isEmpty();
        }

        public Set<UUID> getMembers() {
            return members;
        }

        public int getMemberColor(UUID player) {
            return memberColors.getOrDefault(player, 0xFFFFFF);
        }

        public int getMemberCount() {
            return members.size();
        }
    }

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
        if (!group.password.equals(password))
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

    public static void onPlayerDisconnect(UUID player) {
        leaveGroup(player);
    }
}
