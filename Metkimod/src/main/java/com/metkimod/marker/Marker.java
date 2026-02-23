package com.metkimod.marker;

import java.util.UUID;

public class Marker {
    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private final double x;
    private final double y;
    private final double z;
    private final int color;
    private final long createdAt;
    private final int lifetimeMs;
    private final PingType pingType;
    private final String message;

    public Marker(UUID ownerId, String ownerName, double x, double y, double z,
            int color, int lifetimeMs, PingType pingType) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
        this.lifetimeMs = lifetimeMs;
        this.pingType = pingType;
        this.message = pingType.getMessage();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getColor() {
        return color;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getLifetimeMs() {
        return lifetimeMs;
    }

    public PingType getPingType() {
        return pingType;
    }

    public String getMessage() {
        return message;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > lifetimeMs;
    }

        public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createdAt;
        long remaining = lifetimeMs - elapsed;
        if (remaining <= 0)
            return 0.0f;
        if (remaining < 2000)
            return remaining / 2000.0f;
        return 1.0f;
    }

        public float getProgress() {
        long elapsed = System.currentTimeMillis() - createdAt;
        return Math.min(1.0f, (float) elapsed / lifetimeMs);
    }

        public float getSpawnScale() {
        long elapsed = System.currentTimeMillis() - createdAt;
        if (elapsed > 300)
            return 1.0f;
        float t = elapsed / 300.0f;
        return (float) (1.0 + Math.pow(2, -10 * t) * Math.sin((t - 0.1) * 5 * Math.PI));
    }

    public float getRed() {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    public float getGreen() {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    public float getBlue() {
        return (color & 0xFF) / 255.0f;
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
