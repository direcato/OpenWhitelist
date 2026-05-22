package io.github.openwhitelist.request;

import java.util.UUID;

public class PendingRequest {

    private final String name;
    private final UUID uuid;
    private final long timestamp;
    private static final long EXPIRY_MS = 600_000;

    public PendingRequest(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > EXPIRY_MS;
    }

    public long getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - timestamp;
        long remaining = (EXPIRY_MS - elapsed) / 1000;
        return Math.max(0, remaining);
    }
}
