package io.github.openwhitelist.update;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class HotReloader {

    public static boolean reload(OpenWhitelistPlugin plugin, Path newJar, Path currentJar) {
        try {
            PluginManager pm = Bukkit.getPluginManager();
            pm.disablePlugin(plugin);
            plugin.getLogger().info("Plugin disabled for update");

            Files.deleteIfExists(currentJar);
            Files.move(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Jar replaced: " + currentJar.getFileName());

            if (!unloadPlugin(plugin)) {
                plugin.getLogger().warning("Could not fully unload plugin from provider storage. Update may fail.");
            }

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

    private static boolean unloadPlugin(Plugin plugin) {
        boolean cleaned = false;

        try {
            PluginManager pm = Bukkit.getPluginManager();

            // Remove from SimplePluginManager internal lists (Bukkit/Spigot compat)
            if (pm instanceof SimplePluginManager spm) {
                Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                List<?> plugins = (List<?>) pluginsField.get(spm);
                cleaned |= plugins.remove(plugin);

                Field lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                Map<?, ?> lookupNames = (Map<?, ?>) lookupNamesField.get(spm);
                cleaned |= lookupNames.values().remove(plugin);
            }

            // Paper modern plugin system: try PaperPluginManagerImpl#unloadPlugin
            try {
                Class<?> paperManagerClass = Class.forName("io.papermc.paper.plugin.manager.PaperPluginManagerImpl");
                if (paperManagerClass.isInstance(pm)) {
                    try {
                        Method unloadPlugin = paperManagerClass.getMethod("unloadPlugin", Plugin.class);
                        unloadPlugin.invoke(pm, plugin);
                        cleaned = true;
                    } catch (NoSuchMethodException e) {
                        // Try instanceManager field approach
                        Field instanceManagerField = paperManagerClass.getDeclaredField("instanceManager");
                        instanceManagerField.setAccessible(true);
                        Object instanceManager = instanceManagerField.get(pm);
                        try {
                            Method unload = instanceManager.getClass().getMethod("unloadPlugin", Plugin.class);
                            unload.invoke(instanceManager, plugin);
                            cleaned = true;
                        } catch (NoSuchMethodException ignored) {}
                    }
                }
            } catch (Exception ignored) {
                // Paper classes not available (non-Paper server)
            }

        } catch (Exception e) {
            return cleaned;
        }

        return cleaned;
    }
}
