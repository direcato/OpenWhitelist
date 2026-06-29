package io.github.openwhitelist;

import io.github.openwhitelist.command.OpenWhitelistCommand;
import io.github.openwhitelist.config.ConfigManager;
import io.github.openwhitelist.config.Messages;
import io.github.openwhitelist.geyser.FloodgateHandler;
import io.github.openwhitelist.listener.PlayerLoginListener;
import io.github.openwhitelist.request.RequestManager;
import io.github.openwhitelist.whitelist.WhitelistManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenWhitelistPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private WhitelistManager whitelistManager;
    private FloodgateHandler floodgateHandler;
    private RequestManager requestManager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.whitelistManager = new WhitelistManager(this);
        this.floodgateHandler = new FloodgateHandler(this);
        this.requestManager = new RequestManager();

        whitelistManager.load();

        var command = getCommand("openw");
        if (command != null) {
            var executor = new OpenWhitelistCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(
            new PlayerLoginListener(this), this
        );

        if (floodgateHandler.isAvailable()) {
            getLogger().info("Floodgate detected - Bedrock player support enabled");
        } else {
            getLogger().info("Floodgate not found - Java-only mode");
        }

        getServer().getScheduler().runTaskTimer(this, () ->
            whitelistManager.cleanupExpired(), 600L, 600L);

        getLogger().info("OpenWhitelist v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (whitelistManager != null) {
            whitelistManager.save();
        }
        getLogger().info("OpenWhitelist disabled");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public FloodgateHandler getFloodgateHandler() {
        return floodgateHandler;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public Messages getMessages() {
        return messages;
    }
}
