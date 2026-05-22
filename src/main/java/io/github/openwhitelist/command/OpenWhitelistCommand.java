package io.github.openwhitelist.command;

import io.github.openwhitelist.OpenWhitelistPlugin;
import io.github.openwhitelist.request.PendingRequest;
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
            case "on":
                return handleOn(sender);
            case "off":
                return handleOff(sender);
            case "requests":
                return handleRequests(sender);
            case "accept":
                return handleAccept(sender, args);
            default:
                sendUsage(sender, label);
                return true;
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "===== OpenWhitelist =====");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " add <name>" + ChatColor.GRAY + " - Add a player");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " remove <name>" + ChatColor.GRAY + " - Remove a player");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list [page]" + ChatColor.GRAY + " - List whitelisted players");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload config & whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " update" + ChatColor.GRAY + " - Check for updates");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " on" + ChatColor.GRAY + " - Enable the whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " off" + ChatColor.GRAY + " - Disable the whitelist");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " requests" + ChatColor.GRAY + " - View pending requests");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " accept <name>" + ChatColor.GRAY + " - Accept a whitelist request");
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.add")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /openw add <name>");
            return true;
        }

        String rawName = args[1];
        String name = rawName;

        if (plugin.getConfigManager().hasBedrockPrefix(rawName)
            && plugin.getConfigManager().isAutoStripPrefix()) {
            name = plugin.getConfigManager().stripBedrockPrefix(rawName);
        }

        if (plugin.getWhitelistManager().isWhitelisted(name)) {
            sender.sendMessage(ChatColor.RED + name + " is already whitelisted.");
            return true;
        }

        WhitelistEntry entry = new WhitelistEntry(name, PlayerType.JAVA, null, null, sender.getName());
        plugin.getWhitelistManager().add(entry);

        plugin.getLogger().info(sender.getName() + " added " + name + " to the whitelist");
        sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + name
            + ChatColor.GREEN + " to the whitelist.");
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
            plugin.getLogger().info(sender.getName() + " removed " + name + " from the whitelist");
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

        plugin.getLogger().info(sender.getName() + " triggered update check");
        sender.sendMessage(ChatColor.YELLOW + "Checking for updates...");
        plugin.getUpdateChecker().checkNow(result ->
            sender.sendMessage(result
                ? ChatColor.GREEN + "Update downloaded. Reloading plugin..."
                : ChatColor.RED + "No update available or check failed.")
        );
        return true;
    }

    private boolean handleOn(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.on")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        plugin.getConfigManager().setWhitelistEnabled(true);
        plugin.getConfigManager().save();
        plugin.getLogger().info("Whitelist enabled by " + sender.getName());
        sender.sendMessage(ChatColor.GREEN + "Whitelist enabled.");
        return true;
    }

    private boolean handleOff(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.off")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        plugin.getConfigManager().setWhitelistEnabled(false);
        plugin.getConfigManager().save();
        plugin.getLogger().info("Whitelist disabled by " + sender.getName());
        sender.sendMessage(ChatColor.GREEN + "Whitelist disabled.");
        return true;
    }

    private boolean handleRequests(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.requests")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        List<PendingRequest> all = plugin.getRequestManager().getAll();
        if (all.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No pending requests.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "===== Pending Requests =====");
        for (PendingRequest req : all) {
            sender.sendMessage(ChatColor.WHITE + req.getName()
                + ChatColor.GRAY + " | Expires in " + req.getRemainingSeconds() + "s"
                + ChatColor.GRAY + " | Accept: " + ChatColor.YELLOW + "/openw accept " + req.getName());
        }
        sender.sendMessage(ChatColor.GOLD + "Total: " + all.size() + " pending");
        return true;
    }

    private boolean handleAccept(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.accept")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /openw accept <name>");
            return true;
        }

        String name = args[1];
        Optional<PendingRequest> req = plugin.getRequestManager().get(name);
        if (req.isEmpty()) {
            sender.sendMessage(ChatColor.RED + name + " has no pending request or it expired.");
            return true;
        }

        if (plugin.getWhitelistManager().isWhitelisted(name)) {
            sender.sendMessage(ChatColor.RED + name + " is already whitelisted.");
            plugin.getRequestManager().remove(name);
            return true;
        }

        PendingRequest pending = req.get();
        WhitelistEntry entry = new WhitelistEntry(
            pending.getName(), PlayerType.JAVA, pending.getUuid(), null, sender.getName()
        );
        plugin.getWhitelistManager().add(entry);
        plugin.getRequestManager().remove(name);

        plugin.getLogger().info(sender.getName() + " accepted " + name + "'s whitelist request");
        sender.sendMessage(ChatColor.GREEN + "Accepted " + ChatColor.WHITE + name
            + ChatColor.GREEN + "'s request. They are now whitelisted.");
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
            completions.add("on");
            completions.add("off");
            completions.add("requests");
            completions.add("accept");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            completions.addAll(plugin.getWhitelistManager().getAllEntries().stream()
                .map(WhitelistEntry::getName)
                .collect(Collectors.toList()));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("accept"))) {
            completions.addAll(plugin.getRequestManager().getAll().stream()
                .map(PendingRequest::getName)
                .collect(Collectors.toList()));
        }
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
