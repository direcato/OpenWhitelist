package io.github.openwhitelist.listener;

import io.github.openwhitelist.OpenWhitelistPlugin;
import io.github.openwhitelist.config.Messages;
import io.github.openwhitelist.geyser.FloodgateHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final OpenWhitelistPlugin plugin;
    private final Messages m;

    public PlayerLoginListener(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.m = plugin.getMessages();
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
                    plugin.getRequestManager().add(name, uuid);
                    plugin.getLogger().warning("[OpenWhitelist] Player " + name
                        + " was denied - not whitelisted. Pending request created.");
                    broadcastRequest(name);
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
                plugin.getRequestManager().add(name, uuid);
                plugin.getLogger().warning("[OpenWhitelist] Player " + name
                    + " was denied - not whitelisted. Pending request created.");
                broadcastRequest(name);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getKickMessage()));
            }
        }
    }

    private void broadcastRequest(String name) {
        plugin.getServer().getScheduler().runTask(plugin, () ->
            Bukkit.broadcastMessage(m.msg("broadcast-request", Messages.p("name", name)))
        );
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
