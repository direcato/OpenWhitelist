package io.github.openwhitelist;

import io.github.openwhitelist.command.OpenWhitelistCommand;
import io.github.openwhitelist.config.ConfigManager;
import io.github.openwhitelist.geyser.FloodgateHandler;
import io.github.openwhitelist.listener.PlayerLoginListener;
import io.github.openwhitelist.request.RequestManager;
import io.github.openwhitelist.update.UpdateChecker;
import io.github.openwhitelist.whitelist.WhitelistManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OpenWhitelistPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private WhitelistManager whitelistManager;
    private FloodgateHandler floodgateHandler;
    private UpdateChecker updateChecker;
    private RequestManager requestManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.whitelistManager = new WhitelistManager(this);
        this.floodgateHandler = new FloodgateHandler(this);
        this.updateChecker = new UpdateChecker(this);
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

        updateChecker.start();

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

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }
}
