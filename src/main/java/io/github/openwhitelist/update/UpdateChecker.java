package io.github.openwhitelist.update;

import io.github.openwhitelist.OpenWhitelistPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_PATTERN = Pattern.compile(
        "\"name\"\\s*:\\s*\"OpenWhitelist\\.jar\".*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"",
        Pattern.DOTALL
    );

    private final OpenWhitelistPlugin plugin;
    private final HttpClient httpClient;
    private Path pluginJarPath;
    private String currentVersion;

    public UpdateChecker(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public void start() {
        try {
            currentVersion = plugin.getPluginMeta().getVersion();
        } catch (Exception e) {
            currentVersion = plugin.getDescription().getVersion();
        }

        try {
            java.net.URI jarUri = plugin.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI();
            pluginJarPath = Path.of(jarUri);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not determine plugin jar path: " + e.getMessage());
        }

        if (plugin.getConfigManager().isUpdateEnabled()) {
            scheduleCheck();
        }
    }

    private void scheduleCheck() {
        int interval = plugin.getConfigManager().getCheckIntervalHours();
        if (interval <= 0) interval = 24;

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            () -> checkNow(null),
            20L * 60 * 60 * 2,
            20L * 60 * 60 * interval
        );
    }

    public void checkNow(Consumer<Boolean> callback) {
        String repo = plugin.getConfigManager().getGithubRepo();
        if (repo == null || repo.isEmpty()) {
            plugin.getLogger().info("No GitHub repo configured for update checks.");
            if (callback != null) callback.accept(false);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

                HttpResponse<String> resp = httpClient.send(
                    req, HttpResponse.BodyHandlers.ofString()
                );

                if (resp.statusCode() != 200) {
                    plugin.getLogger().warning("GitHub API returned HTTP " + resp.statusCode());
                    if (callback != null) callback.accept(false);
                    return;
                }

                String body = resp.body();

                Matcher tagMatcher = TAG_PATTERN.matcher(body);
                if (!tagMatcher.find()) {
                    plugin.getLogger().warning("Could not find tag_name in GitHub response.");
                    if (callback != null) callback.accept(false);
                    return;
                }
                String latestTag = tagMatcher.group(1);

                if (!isNewerVersion(latestTag, currentVersion)) {
                    plugin.getLogger().info("Already running latest version (" + currentVersion + ").");
                    if (callback != null) callback.accept(false);
                    return;
                }

                Matcher assetMatcher = ASSET_PATTERN.matcher(body);
                if (!assetMatcher.find()) {
                    plugin.getLogger().warning("Could not find download URL for OpenWhitelist.jar in release.");
                    if (callback != null) callback.accept(false);
                    return;
                }
                String downloadUrl = assetMatcher.group(1);

                plugin.getLogger().info("New version found: " + latestTag
                    + " (current: " + currentVersion + "). Downloading...");

                Path downloadDir = plugin.getDataFolder().toPath().resolve("update");
                Files.createDirectories(downloadDir);
                Path tempFile = downloadDir.resolve("OpenWhitelist-" + latestTag + ".jar");

                HttpRequest dlReq = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

                HttpResponse<InputStream> dlResp = httpClient.send(
                    dlReq, HttpResponse.BodyHandlers.ofInputStream()
                );

                if (dlResp.statusCode() != 200) {
                    plugin.getLogger().warning("Download failed: HTTP " + dlResp.statusCode());
                    if (callback != null) callback.accept(false);
                    return;
                }

                Files.copy(dlResp.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                String newHash = computeHash(tempFile);
                if (newHash != null && newHash.equals(currentHash())) {
                    plugin.getLogger().info("Downloaded jar is identical to current. Skipping update.");
                    Files.deleteIfExists(tempFile);
                    if (callback != null) callback.accept(false);
                    return;
                }

                plugin.getLogger().info("Update downloaded (" + latestTag + "). Hot-reloading...");
                boolean reloaded = HotReloader.reload(plugin, tempFile, pluginJarPath);
                if (callback != null) callback.accept(reloaded);
            } catch (Exception e) {
                plugin.getLogger().severe("Update check failed: " + e.getMessage());
                if (callback != null) callback.accept(false);
            }
        });
    }

    private boolean isNewerVersion(String tag, String current) {
        String v1 = tag.replaceAll("^[vV]", "");
        String v2 = current.replaceAll("^[vV]", "");
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? tryParseInt(parts1[i], 0) : 0;
            int n2 = i < parts2.length ? tryParseInt(parts2[i], 0) : 0;
            if (n1 > n2) return true;
            if (n1 < n2) return false;
        }
        return false;
    }

    private int tryParseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String currentHash() {
        if (pluginJarPath == null) return null;
        return computeHash(pluginJarPath);
    }

    private String computeHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }
}
