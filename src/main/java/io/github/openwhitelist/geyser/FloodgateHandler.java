package io.github.openwhitelist.geyser;

import io.github.openwhitelist.OpenWhitelistPlugin;
import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Optional;
import java.util.UUID;

public class FloodgateHandler {

    private final OpenWhitelistPlugin plugin;
    private FloodgateApi floodgateApi;
    private boolean available;

    public FloodgateHandler(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().getPlugin("Floodgate") != null;
        if (available) {
            try {
                this.floodgateApi = FloodgateApi.getInstance();
            } catch (Exception e) {
                this.available = false;
                plugin.getLogger().warning("Floodgate API not accessible: " + e.getMessage());
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isFloodgatePlayer(UUID uuid) {
        if (!available || floodgateApi == null) return false;
        try {
            return floodgateApi.isFloodgatePlayer(uuid);
        } catch (Exception e) {
            return false;
        }
    }

    public FloodgatePlayer getFloodgatePlayer(UUID uuid) {
        if (!available || floodgateApi == null) return null;
        try {
            return floodgateApi.getPlayer(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public FloodgatePlayer getFloodgatePlayerByUsername(String username) {
        if (!available || floodgateApi == null) return null;
        try {
            Optional<FloodgatePlayer> found = floodgateApi.getPlayers().stream()
                .filter(p -> {
                    String clean = plugin.getConfigManager().stripBedrockPrefix(username);
                    return p.getUsername().equalsIgnoreCase(clean)
                        || p.getJavaUsername().equalsIgnoreCase(clean)
                        || p.getCorrectUsername().equalsIgnoreCase(username);
                })
                .findFirst();
            return found.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public String getXuid(UUID uuid) {
        FloodgatePlayer player = getFloodgatePlayer(uuid);
        return player != null ? player.getXuid() : null;
    }

    public UUID getStrippedUuid(UUID uuid) {
        FloodgatePlayer player = getFloodgatePlayer(uuid);
        return player != null ? player.getJavaUniqueId() : null;
    }
}
