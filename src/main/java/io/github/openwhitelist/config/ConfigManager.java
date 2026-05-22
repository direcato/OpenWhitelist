package io.github.openwhitelist.config;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final OpenWhitelistPlugin plugin;
    private String bedrockPrefix;
    private boolean autoStripPrefix;
    private String kickMessage;
    private boolean whitelistEnabled;
    private boolean updateEnabled;
    private String updateUrl;
    private String hashUrl;
    private int checkIntervalHours;

    public ConfigManager(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.bedrockPrefix = config.getString("geyser.bedrock-prefix", ".");
        this.autoStripPrefix = config.getBoolean("geyser.auto-strip-prefix", true);
        this.kickMessage = config.getString("whitelist.kick-message", "&cYou are not whitelisted on this server.");
        this.whitelistEnabled = config.getBoolean("whitelist.enabled", true);
        this.updateEnabled = config.getBoolean("update.enabled", true);
        this.updateUrl = config.getString("update.url", "");
        this.hashUrl = config.getString("update.hash-url", "");
        this.checkIntervalHours = config.getInt("update.check-interval-hours", 24);
    }

    public String getBedrockPrefix() {
        return bedrockPrefix;
    }

    public boolean isAutoStripPrefix() {
        return autoStripPrefix;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public boolean isUpdateEnabled() {
        return updateEnabled;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public String getHashUrl() {
        return hashUrl;
    }

    public int getCheckIntervalHours() {
        return checkIntervalHours;
    }

    public String stripBedrockPrefix(String name) {
        if (name != null && bedrockPrefix != null && !bedrockPrefix.isEmpty()
            && name.startsWith(bedrockPrefix)) {
            return name.substring(bedrockPrefix.length());
        }
        return name;
    }

    public boolean hasBedrockPrefix(String name) {
        return name != null && bedrockPrefix != null && !bedrockPrefix.isEmpty()
            && name.startsWith(bedrockPrefix);
    }
}
