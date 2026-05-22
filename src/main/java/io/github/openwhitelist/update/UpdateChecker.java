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

public class UpdateChecker {

    private final OpenWhitelistPlugin plugin;
    private final HttpClient httpClient;
    private Path pluginJarPath;
    private String currentHash;

    public UpdateChecker(OpenWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public void start() {
        try {
            java.net.URI jarUri = plugin.getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI();
            pluginJarPath = Path.of(jarUri);
            currentHash = computeHash(pluginJarPath);
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
        String url = plugin.getConfigManager().getUpdateUrl();
        if (url == null || url.isEmpty()) {
            plugin.getLogger().info("No update URL configured.");
            if (callback != null) callback.accept(false);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Path downloadDir = plugin.getDataFolder().toPath().resolve("update");
                Files.createDirectories(downloadDir);
                Path tempFile = downloadDir.resolve("OpenWhitelist-latest.jar");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

                HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream()
                );

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Update check failed: HTTP " + response.statusCode());
                    if (callback != null) callback.accept(false);
                    return;
                }

                Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                String hashUrl = plugin.getConfigManager().getHashUrl();
                if (hashUrl != null && !hashUrl.isEmpty()) {
                    String expectedHash = fetchHash(hashUrl);
                    if (expectedHash != null) {
                        String downloadedHash = computeHash(tempFile);
                        if (!expectedHash.equalsIgnoreCase(downloadedHash)) {
                            plugin.getLogger().warning("Update hash mismatch - rejecting update");
                            Files.deleteIfExists(tempFile);
                            if (callback != null) callback.accept(false);
                            return;
                        }
                    }
                }

                if (currentHash != null) {
                    String newHash = computeHash(tempFile);
                    if (newHash.equalsIgnoreCase(currentHash)) {
                        plugin.getLogger().info("Already running the latest version.");
                        Files.deleteIfExists(tempFile);
                        if (callback != null) callback.accept(false);
                        return;
                    }
                }

                plugin.getLogger().info("Update downloaded. Performing hot reload...");
                boolean reloaded = HotReloader.reload(plugin, tempFile, pluginJarPath);
                if (callback != null) callback.accept(reloaded);
            } catch (Exception e) {
                plugin.getLogger().severe("Update check failed: " + e.getMessage());
                if (callback != null) callback.accept(false);
            }
        });
    }

    private String fetchHash(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 200) {
                return response.body().trim().split("\\s+")[0];
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch hash: " + e.getMessage());
        }
        return null;
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
