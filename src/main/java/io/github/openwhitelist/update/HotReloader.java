package io.github.openwhitelist.update;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

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
        Path backupJar = currentJar.resolveSibling(currentJar.getFileName() + ".bak");
        Object paperPm = getPaperPluginManager();

        try {
            // Backup original jar
            if (Files.exists(currentJar)) {
                Files.copy(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
            }

            String pluginName = plugin.getName();
            if (paperPm != null) {
                callMethod(paperPm, "disablePlugin", new Class<?>[]{Plugin.class}, plugin);
            } else {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
            plugin.getLogger().info("Plugin disabled for update");

            Files.deleteIfExists(currentJar);
            Files.move(newJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Jar replaced: " + currentJar.getFileName());

            if (paperPm != null) {
                unloadFromProviderStorage(paperPm, plugin, pluginName);
            }

            PluginManager pm = Bukkit.getPluginManager();
            Plugin loaded = pm.loadPlugin(currentJar.toFile());
            if (loaded == null) {
                throw new InvalidPluginException("Plugin didn't load any plugin providers?");
            }

            loaded.onLoad();
            pm.enablePlugin(loaded);
            Files.deleteIfExists(backupJar);
            plugin.getLogger().info("Update complete - " + loaded.getName() + " v" + loaded.getDescription().getVersion());
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Hot reload failed: " + e.getMessage());
            plugin.getLogger().info("Restoring backup...");

            try {
                if (Files.exists(backupJar)) {
                    Files.deleteIfExists(currentJar);
                    Files.move(backupJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
                }

                PluginManager pm = Bukkit.getPluginManager();
                if (paperPm != null) {
                    callMethod(paperPm, "disablePlugin", new Class<?>[]{Plugin.class}, plugin);
                } else {
                    pm.disablePlugin(plugin);
                }
                unloadFromProviderStorage(paperPm, plugin, plugin.getName());

                Plugin restored = pm.loadPlugin(currentJar.toFile());
                if (restored != null) {
                    restored.onLoad();
                    pm.enablePlugin(restored);
                    plugin.getLogger().info("Backup restored and plugin re-enabled");
                }
            } catch (Exception restoreError) {
                plugin.getLogger().severe("Failed to restore backup: " + restoreError.getMessage());
            }

            return false;
        }
    }

    private static Object getPaperPluginManager() {
        try {
            Class<?> craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
            Object craftServer = craftServerClass.cast(Bukkit.getServer());
            Field paperPmField = craftServerClass.getDeclaredField("paperPluginManager");
            paperPmField.setAccessible(true);
            return paperPmField.get(craftServer);
        } catch (Exception e) {
            return null;
        }
    }

    private static void unloadFromProviderStorage(Object paperPm, Plugin plugin, String pluginName) {
        if (paperPm == null) {
            try {
                PluginManager pm = Bukkit.getPluginManager();
                Class<?> spmClass = Class.forName("org.bukkit.plugin.SimplePluginManager");
                if (spmClass.isInstance(pm)) {
                    Field pluginsField = spmClass.getDeclaredField("plugins");
                    pluginsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<Plugin> plugins = (List<Plugin>) pluginsField.get(pm);
                    plugins.remove(plugin);

                    Field lookupNamesField = spmClass.getDeclaredField("lookupNames");
                    lookupNamesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(pm);
                    lookupNames.values().remove(plugin);
                }
            } catch (Exception ignored) {
            }
            return;
        }

        try {
            Field instanceManagerField = paperPm.getClass().getDeclaredField("instanceManager");
            instanceManagerField.setAccessible(true);
            Object instanceManager = instanceManagerField.get(paperPm);

            Field pluginsField = instanceManager.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Plugin> plugins = (List<Plugin>) pluginsField.get(instanceManager);
            plugins.remove(plugin);

            Field lookupNamesField = instanceManager.getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(instanceManager);
            lookupNames.values().remove(plugin);
        } catch (Exception ignored) {
        }
    }

    private static Object callMethod(Object obj, String name, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = obj.getClass().getMethod(name, paramTypes);
            return m.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }
}
