package io.github.openwhitelist.listener;

import io.github.openwhitelist.OpenWhitelistPlugin;
import io.github.openwhitelist.geyser.FloodgateHandler;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final OpenWhitelistPlugin plugin;

    public PlayerLoginListener(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfigManager().isWhitelistEnabled()) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String name = event.getName();

        FloodgateHandler fh = plugin.getFloodgateHandler();

        if (fh.isFloodgatePlayer(uuid)) {
            var fPlayer = fh.getFloodgatePlayer(uuid);
            if (fPlayer != null) {
                UUID strippedUuid = fPlayer.getJavaUniqueId();
                String xuid = fPlayer.getXuid();
                String bedrockName = plugin.getConfigManager().stripBedrockPrefix(name);

                boolean whitelisted = plugin.getWhitelistManager().isWhitelisted(strippedUuid)
                    || plugin.getWhitelistManager().isWhitelistedByXuid(xuid)
                    || plugin.getWhitelistManager().isWhitelisted(bedrockName);

                if (!whitelisted) {
                    plugin.getLogger().warning("[OpenWhitelist] Player " + name
                        + " was denied - not whitelisted");
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfigManager().getKickMessage()));
                    return;
                }

                autoWhitelistBedrock(strippedUuid, xuid, bedrockName);
            }
        } else {
            if (!plugin.getWhitelistManager().isWhitelisted(name)
                && !plugin.getWhitelistManager().isWhitelisted(uuid)) {
                plugin.getLogger().warning("[OpenWhitelist] Player " + name
                    + " was denied - not whitelisted");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getKickMessage()));
            }
        }
    }

    private void autoWhitelistBedrock(UUID strippedUuid, String xuid, String bedrockName) {
        plugin.getWhitelistManager().getEntry(strippedUuid).ifPresentOrElse(
            existing -> {},
            () -> {
                plugin.getWhitelistManager().getEntry(bedrockName).ifPresentOrElse(
                    existing -> {
                        existing.setUuid(strippedUuid);
                        existing.setXuid(xuid);
                        plugin.getWhitelistManager().save();
                    },
                    () -> {}
                );
            }
        );
    }
}
