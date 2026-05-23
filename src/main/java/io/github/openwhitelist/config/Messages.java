package io.github.openwhitelist.config;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Messages {

    private final OpenWhitelistPlugin plugin;
    private YamlConfiguration messages;

    public Messages(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        saveDefaultLanguages();
        reload();
    }

    public void reload() {
        String lang = plugin.getConfigManager().getLanguage();
        File langFile = new File(plugin.getDataFolder(), "lang" + File.separator + "lang_" + lang + ".yml");
        if (!langFile.exists()) {
            langFile = new File(plugin.getDataFolder(), "lang" + File.separator + "lang_en.yml");
        }
        messages = YamlConfiguration.loadConfiguration(langFile);
    }

    private void saveDefaultLanguages() {
        Path langDir = plugin.getDataFolder().toPath().resolve("lang");
        try {
            Files.createDirectories(langDir);
        } catch (IOException ignored) {}

        for (String file : new String[]{"lang_en.yml", "lang_tl.yml"}) {
            File target = langDir.resolve(file).toFile();
            if (!target.exists()) {
                try (InputStream in = plugin.getResource("lang/" + file)) {
                    if (in != null) {
                        Files.copy(in, target.toPath());
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    public String msg(String key) {
        String raw = messages.getString(key);
        if (raw == null) return "&cMissing message: " + key;
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String msg(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key);
        if (raw == null) return "&cMissing message: " + key;
        for (var entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    @SafeVarargs
    public final String msg(String key, Pair<String, String>... placeholders) {
        String raw = messages.getString(key);
        if (raw == null) return "&cMissing message: " + key;
        for (Pair<String, String> p : placeholders) {
            raw = raw.replace("{" + p.key + "}", p.value);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public static Pair<String, String> p(String key, String value) {
        return new Pair<>(key, value);
    }

    public static class Pair<K, V> {
        public final K key;
        public final V value;
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
