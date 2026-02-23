package com.metkimod.group;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarkerGroup {
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

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public UUID getCreator() {
        return creator;
    }

    public boolean checkPassword(String attempt) {
        return this.password.equals(attempt);
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

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

        public int getMemberColor(UUID playerId) {
        return memberColors.getOrDefault(playerId, 0xFFFFFF);
    }

        public void setMemberColor(UUID playerId, int color) {
        if (members.contains(playerId)) {
            memberColors.put(playerId, color);
        }
    }
}
