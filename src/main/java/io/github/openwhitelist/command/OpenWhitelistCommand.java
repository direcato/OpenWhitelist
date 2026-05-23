package io.github.openwhitelist.command;

import io.github.openwhitelist.OpenWhitelistPlugin;
import io.github.openwhitelist.config.Messages;
import io.github.openwhitelist.request.PendingRequest;
import io.github.openwhitelist.whitelist.WhitelistEntry;
import io.github.openwhitelist.whitelist.WhitelistEntry.PlayerType;
import org.bukkit.Bukkit;
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
    private final Messages m;

    public OpenWhitelistCommand(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.m = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
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
            case "on":
                return handleOn(sender);
            case "off":
                return handleOff(sender);
            case "requests":
                return handleRequests(sender);
            case "accept":
                return handleAccept(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(m.msg("usage-header"));
        sender.sendMessage(m.msg("usage-add"));
        sender.sendMessage(m.msg("usage-remove"));
        sender.sendMessage(m.msg("usage-list"));
        sender.sendMessage(m.msg("usage-reload"));
        sender.sendMessage(m.msg("usage-on"));
        sender.sendMessage(m.msg("usage-off"));
        sender.sendMessage(m.msg("usage-requests"));
        sender.sendMessage(m.msg("usage-accept"));
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.add")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(m.msg("add-usage"));
            return true;
        }

        String rawName = args[1];
        String name = rawName;

        if (plugin.getConfigManager().hasBedrockPrefix(rawName)
            && plugin.getConfigManager().isAutoStripPrefix()) {
            name = plugin.getConfigManager().stripBedrockPrefix(rawName);
        }

        if (plugin.getWhitelistManager().isWhitelisted(name)) {
            sender.sendMessage(m.msg("add-already", Messages.p("name", name)));
            return true;
        }

        WhitelistEntry entry = new WhitelistEntry(name, PlayerType.JAVA, null, null, sender.getName());
        plugin.getWhitelistManager().add(entry);

        plugin.getLogger().info(sender.getName() + " added " + name + " to the whitelist");
        sender.sendMessage(m.msg("add-success", Messages.p("name", name)));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.remove")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(m.msg("remove-usage"));
            return true;
        }

        String name = args[1];
        if (plugin.getWhitelistManager().remove(name)) {
            plugin.getLogger().info(sender.getName() + " removed " + name + " from the whitelist");
            sender.sendMessage(m.msg("remove-success", Messages.p("name", name)));
        } else {
            sender.sendMessage(m.msg("remove-not-found", Messages.p("name", name)));
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.list")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }

        int page = 0;
        if (args.length >= 2) {
            try {
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
            } catch (NumberFormatException e) {
                sender.sendMessage(m.msg("list-invalid-page"));
                return true;
            }
        }

        int totalPages = plugin.getWhitelistManager().getTotalPages(PAGE_SIZE);
        if (totalPages == 0) {
            sender.sendMessage(m.msg("list-empty"));
            return true;
        }
        if (page >= totalPages) {
            sender.sendMessage(m.msg("list-page-missing",
                Messages.p("page", String.valueOf(page + 1)),
                Messages.p("max", String.valueOf(totalPages))));
            return true;
        }

        sender.sendMessage(m.msg("list-header",
            Messages.p("page", String.valueOf(page + 1)),
            Messages.p("total", String.valueOf(totalPages))));

        for (WhitelistEntry entry : plugin.getWhitelistManager().getEntries(page, PAGE_SIZE)) {
            String date = DATE_FORMAT.format(Instant.ofEpochMilli(entry.getAddedAt()));
            String xuidStr = entry.getXuid() != null
                ? m.msg("list-xuid", Messages.p("xuid", entry.getXuid()))
                : "";
            sender.sendMessage(m.msg("list-entry",
                Messages.p("name", entry.getName()),
                Messages.p("type", entry.getType().name()),
                Messages.p("date", date),
                Messages.p("xuid", xuidStr)));
        }

        sender.sendMessage(m.msg("list-total",
            Messages.p("count", String.valueOf(plugin.getWhitelistManager().getEntryCount()))));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.reload")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }

        plugin.getConfigManager().reload();
        plugin.getWhitelistManager().load();
        sender.sendMessage(m.msg("reload-success"));
        plugin.getLogger().info("Reloaded by " + sender.getName());
        return true;
    }

    private boolean handleOn(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.on")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }
        plugin.getConfigManager().setWhitelistEnabled(true);
        plugin.getConfigManager().save();
        plugin.getLogger().info("Whitelist enabled by " + sender.getName());
        sender.sendMessage(m.msg("on-success"));
        return true;
    }

    private boolean handleOff(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.off")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }
        plugin.getConfigManager().setWhitelistEnabled(false);
        plugin.getConfigManager().save();
        plugin.getLogger().info("Whitelist disabled by " + sender.getName());
        sender.sendMessage(m.msg("off-success"));
        return true;
    }

    private boolean handleRequests(CommandSender sender) {
        if (!sender.hasPermission("openwhitelist.requests")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }

        List<PendingRequest> all = plugin.getRequestManager().getAll();
        if (all.isEmpty()) {
            sender.sendMessage(m.msg("requests-empty"));
            return true;
        }

        sender.sendMessage(m.msg("requests-header"));
        for (PendingRequest req : all) {
            sender.sendMessage(m.msg("requests-entry",
                Messages.p("name", req.getName()),
                Messages.p("seconds", String.valueOf(req.getRemainingSeconds()))));
        }
        sender.sendMessage(m.msg("requests-total",
            Messages.p("count", String.valueOf(all.size()))));
        return true;
    }

    private boolean handleAccept(CommandSender sender, String[] args) {
        if (!sender.hasPermission("openwhitelist.accept")) {
            sender.sendMessage(m.msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(m.msg("accept-usage"));
            return true;
        }

        String name = args[1];
        Optional<PendingRequest> req = plugin.getRequestManager().get(name);
        if (req.isEmpty()) {
            sender.sendMessage(m.msg("accept-no-request", Messages.p("name", name)));
            return true;
        }

        if (plugin.getWhitelistManager().isWhitelisted(name)) {
            sender.sendMessage(m.msg("accept-already", Messages.p("name", name)));
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
        sender.sendMessage(m.msg("accept-success", Messages.p("name", name)));
        Bukkit.broadcastMessage(m.msg("broadcast-accept",
            Messages.p("name", name),
            Messages.p("acceptor", sender.getName())));
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
            completions.add("on");
            completions.add("off");
            completions.add("requests");
            completions.add("accept");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            completions.addAll(plugin.getWhitelistManager().getAllEntries().stream()
                .map(WhitelistEntry::getName)
                .collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            completions.addAll(plugin.getRequestManager().getAll().stream()
                .map(PendingRequest::getName)
                .collect(Collectors.toList()));
        }
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
