package io.github.openwhitelist.update;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class HotReloader {

    public static boolean reload(OpenWhitelistPlugin plugin, Path newJar, Path currentJar) {
        try {
            PluginManager pm = Bukkit.getPluginManager();
            String pluginName = plugin.getName();

            pm.disablePlugin(plugin);
            plugin.getLogger().info("Plugin disabled for update");

            Files.deleteIfExists(currentJar);
            Files.move(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Jar replaced: " + currentJar.getFileName());

            Plugin loaded = pm.loadPlugin(currentJar.toFile());
            if (loaded == null) {
                plugin.getLogger().severe("Failed to load updated plugin jar");
                return false;
            }

            loaded.onLoad();
            pm.enablePlugin(loaded);
            plugin.getLogger().info("Update complete - " + loaded.getName() + " v" + loaded.getDescription().getVersion());
            return true;
        } catch (IOException | InvalidPluginException | InvalidDescriptionException e) {
            plugin.getLogger().severe("Hot reload failed: " + e.getMessage());
            return false;
        }
    }
}
