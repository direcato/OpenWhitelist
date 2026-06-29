package io.github.openwhitelist.whitelist;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WhitelistManager {

    private final OpenWhitelistPlugin plugin;
    private final List<WhitelistEntry> entries;
    private File file;

    public WhitelistManager(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.entries = new ArrayList<>();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "whitelist.yml");
        if (!file.exists()) {
            plugin.saveResource("whitelist.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        entries.clear();

        java.util.List<?> raw = config.getList("players", new ArrayList<>());
        for (Object obj : raw) {
            if (obj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                try {
                    entries.add(WhitelistEntry.deserialize(map));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load whitelist entry: " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("Loaded " + entries.size() + " whitelist entries");
    }

    public void save() {
        if (file == null) return;

        YamlConfiguration config = new YamlConfiguration();
        List<java.util.Map<String, Object>> serialized = entries.stream()
            .map(WhitelistEntry::serialize)
            .collect(Collectors.toList());
        config.set("players", serialized);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save whitelist: " + e.getMessage());
        }
    }

    public boolean add(WhitelistEntry entry) {
        if (isWhitelisted(entry.getName())) {
            return false;
        }
        entries.add(entry);
        save();
        return true;
    }

    public boolean remove(String name) {
        boolean removed = entries.removeIf(e -> e.getName().equalsIgnoreCase(name));
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean isWhitelisted(String name) {
        return entries.stream().anyMatch(e -> e.getName().equalsIgnoreCase(name));
    }

    public boolean isWhitelisted(UUID uuid) {
        return entries.stream().anyMatch(e -> uuid.equals(e.getUuid()));
    }

    public boolean isWhitelistedByXuid(String xuid) {
        if (xuid == null) return false;
        return entries.stream().anyMatch(e -> xuid.equals(e.getXuid()));
    }

    public Optional<WhitelistEntry> getEntry(String name) {
        return entries.stream()
            .filter(e -> e.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    public Optional<WhitelistEntry> getEntry(UUID uuid) {
        return entries.stream()
            .filter(e -> uuid.equals(e.getUuid()))
            .findFirst();
    }

    public List<WhitelistEntry> getAllEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<WhitelistEntry> getEntries(int page, int pageSize) {
        int from = page * pageSize;
        if (from >= entries.size()) return List.of();
        int to = Math.min(from + pageSize, entries.size());
        return entries.subList(from, to);
    }

    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) entries.size() / pageSize);
    }

    public int getEntryCount() {
        return entries.size();
    }

    public void cleanupExpired() {
        boolean removed = entries.removeIf(WhitelistEntry::isExpired);
        if (removed) {
            save();
            plugin.getLogger().info("Removed expired timed whitelist entries");
        }
    }
}
