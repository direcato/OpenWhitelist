package io.github.openwhitelist.command;

import io.github.openwhitelist.OpenWhitelistPlugin;
import io.github.openwhitelist.geyser.FloodgateHandler;
import io.github.openwhitelist.whitelist.WhitelistEntry;
import io.github.openwhitelist.whitelist.WhitelistEntry.PlayerType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenWhitelistCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final int PAGE_SIZE = 10;

    private final OpenWhitelistPlugin plugin;

    public OpenWhitelistCommand(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender, args);
            case "reload":
                return handleReload(sender);
            case "update":
                return handleUpdate(sender);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "===== OpenWhitelist =====");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " add <name> [java|bedrock]" + ChatColor.GRAY + " - Add a player");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " remove <name>" + ChatColor.GRAY + " - Remove a player");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list [page]" + ChatColor.GRAY + " - List whitelisted players");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload config & whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " update" + ChatColor.GRAY + " - Check for updates");
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.add")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /openw add <name> [java|bedrock]");
            return true;
        }

        String rawName = args[1];
        String name = rawName;
        PlayerType type = PlayerType.JAVA;

        if (args.length >= 3) {
            String typeArg = args[2].toLowerCase();
            if (typeArg.equals("bedrock") || typeArg.equals("b")) {
                type = PlayerType.BEDROCK;
            }
        }

        boolean hasPrefix = plugin.getConfigManager().hasBedrockPrefix(rawName);
        if (hasPrefix) {
            type = PlayerType.BEDROCK;
            if (plugin.getConfigManager().isAutoStripPrefix()) {
                name = plugin.getConfigManager().stripBedrockPrefix(rawName);
            }
        }

        if (plugin.getWhitelistManager().isWhitelisted(name)) {
            sender.sendMessage(ChatColor.RED + name + " is already whitelisted.");
            return true;
        }

        UUID uuid = null;
        String xuid = null;

        FloodgateHandler fh = plugin.getFloodgateHandler();
        if (type == PlayerType.BEDROCK && fh.isAvailable()) {
            if (hasPrefix) {
                var fPlayer = fh.getFloodgatePlayerByUsername(rawName);
                if (fPlayer != null) {
                    uuid = fPlayer.getJavaUniqueId();
                    xuid = fPlayer.getXuid();
                }
            }
        }

        WhitelistEntry entry = new WhitelistEntry(name, type, uuid, xuid, sender.getName());
        plugin.getWhitelistManager().add(entry);

        sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + name
            + ChatColor.GREEN + " (" + type + ") to the whitelist.");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.remove")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /openw remove <name>");
            return true;
        }

        String name = args[1];
        if (plugin.getWhitelistManager().remove(name)) {
            sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + name
                + ChatColor.GREEN + " from the whitelist.");
        } else {
            sender.sendMessage(ChatColor.RED + name + " is not whitelisted.");
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.list")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        int page = 0;
        if (args.length >= 2) {
            try {
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        int totalPages = plugin.getWhitelistManager().getTotalPages(PAGE_SIZE);
        if (totalPages == 0) {
            sender.sendMessage(ChatColor.YELLOW + "The whitelist is empty.");
            return true;
        }
        if (page >= totalPages) {
            sender.sendMessage(ChatColor.RED + "Page " + (page + 1) + " doesn't exist. Max page: " + totalPages);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "===== Whitelist (Page "
            + (page + 1) + "/" + totalPages + ") =====");

        for (WhitelistEntry entry : plugin.getWhitelistManager().getEntries(page, PAGE_SIZE)) {
            String date = DATE_FORMAT.format(Instant.ofEpochMilli(entry.getAddedAt()));
            sender.sendMessage(ChatColor.WHITE + entry.getName()
                + ChatColor.GRAY + " | " + ChatColor.AQUA + entry.getType()
                + ChatColor.GRAY + " | Added: " + date
                + (entry.getXuid() != null ? ChatColor.GRAY + " | XUID: " + entry.getXuid() : ""));
        }

        sender.sendMessage(ChatColor.GOLD + "Total: " + plugin.getWhitelistManager().getEntryCount() + " entries");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        plugin.getConfigManager().reload();
        plugin.getWhitelistManager().load();
        sender.sendMessage(ChatColor.GREEN + "OpenWhitelist config & whitelist reloaded.");
        plugin.getLogger().info("Reloaded by " + sender.getName());
        return true;
    }

    private boolean handleUpdate(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.update")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (!plugin.getConfigManager().isUpdateEnabled()) {
            sender.sendMessage(ChatColor.RED + "Auto-update is disabled in config.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Checking for updates...");
        plugin.getUpdateChecker().checkNow(result ->
            sender.sendMessage(result
                ? ChatColor.GREEN + "Update downloaded. Reloading plugin..."
                : ChatColor.RED + "No update available or check failed.")
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
            completions.add("reload");
            completions.add("update");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            completions.addAll(plugin.getWhitelistManager().getAllEntries().stream()
                .map(WhitelistEntry::getName)
                .collect(Collectors.toList()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            completions.add("java");
            completions.add("bedrock");
        }
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
