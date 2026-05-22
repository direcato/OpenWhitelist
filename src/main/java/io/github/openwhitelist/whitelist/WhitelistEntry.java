package io.github.openwhitelist.whitelist;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class WhitelistEntry {

    private String name;
    private PlayerType type;
    private UUID uuid;
    private String xuid;
    private String addedBy;
    private long addedAt;

    public WhitelistEntry() {
    }

    public WhitelistEntry(String name, PlayerType type, UUID uuid, String xuid, String addedBy) {
        this.name = name;
        this.type = type;
        this.uuid = uuid;
        this.xuid = xuid;
        this.addedBy = addedBy;
        this.addedAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlayerType getType() {
        return type;
    }

    public void setType(PlayerType type) {
        this.type = type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getXuid() {
        return xuid;
    }

    public void setXuid(String xuid) {
        this.xuid = xuid;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("type", type.name());
        map.put("uuid", uuid != null ? uuid.toString() : "");
        if (xuid != null && !xuid.isEmpty()) {
            map.put("xuid", xuid);
        }
        map.put("added-by", addedBy);
        map.put("added-at", addedAt);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static WhitelistEntry deserialize(Map<String, Object> map) {
        WhitelistEntry entry = new WhitelistEntry();
        entry.setName((String) map.get("name"));
        String typeStr = (String) map.get("type");
        entry.setType(typeStr != null ? PlayerType.valueOf(typeStr) : PlayerType.JAVA);
        String uuidStr = (String) map.get("uuid");
        if (uuidStr != null && !uuidStr.isEmpty()) {
            entry.setUuid(UUID.fromString(uuidStr));
        }
        entry.setXuid((String) map.get("xuid"));
        entry.setAddedBy((String) map.get("added-by"));
        Object addedAtObj = map.get("added-at");
        if (addedAtObj instanceof Number) {
            entry.setAddedAt(((Number) addedAtObj).longValue());
        }
        return entry;
    }

    public enum PlayerType {
        JAVA,
        BEDROCK
    }
}
