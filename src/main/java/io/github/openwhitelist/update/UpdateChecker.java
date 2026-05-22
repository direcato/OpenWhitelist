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

public class UpdateChecker {

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
            checkNow();
        }
    }

    public CompletableFuture<Void> checkNow() {
        String url = plugin.getConfigManager().getUpdateUrl();
        String repo = plugin.getConfigManager().getGithubRepo();

        if (url != null && !url.isEmpty()) {
            return checkFromUrl(url);
        } else if (repo != null && !repo.isEmpty()) {
            return checkFromGithubApi(repo);
        } else {
            plugin.getLogger().info("No update URL or GitHub repo configured.");
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> checkFromUrl(String url) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path downloadDir = plugin.getDataFolder().toPath().resolve("update");
                Files.createDirectories(downloadDir);
                Path tempFile = downloadDir.resolve("OpenWhitelist-latest.jar");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

                HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream()
                );

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Update download failed: HTTP " + response.statusCode());
                    return;
                }

                Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                String newHash = computeHash(tempFile);
                String currentHash = currentHash();
                if (newHash != null && newHash.equals(currentHash)) {
                    plugin.getLogger().info("Already running the latest version.");
                    Files.deleteIfExists(tempFile);
                    return;
                }

                applyUpdate(tempFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Update check failed: " + e.getMessage());
            }
        });
    }

    private CompletableFuture<Void> checkFromGithubApi(String repo) {
        return CompletableFuture.runAsync(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "OpenWhitelist/" + currentVersion)
                    .GET()
                    .build();

                HttpResponse<String> resp = httpClient.send(
                    req, HttpResponse.BodyHandlers.ofString()
                );

                if (resp.statusCode() != 200) {
                    plugin.getLogger().warning("GitHub API returned HTTP " + resp.statusCode());
                    return;
                }

                String body = resp.body();
                String latestTag = extractJsonString(body, "tag_name");
                if (latestTag == null) {
                    plugin.getLogger().warning("Could not find tag_name in GitHub response.");
                    return;
                }

                if (!isNewerVersion(latestTag, currentVersion)) {
                    plugin.getLogger().info("Already running latest version (" + currentVersion + ").");
                    return;
                }

                String downloadUrl = findAssetUrl(body, "OpenWhitelist.jar");
                if (downloadUrl == null) {
                    plugin.getLogger().warning("Could not find download URL for OpenWhitelist.jar in release.");
                    return;
                }

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
                    return;
                }

                Files.copy(dlResp.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                String newHash = computeHash(tempFile);
                if (newHash != null && newHash.equals(currentHash())) {
                    plugin.getLogger().info("Downloaded jar is identical to current. Skipping update.");
                    Files.deleteIfExists(tempFile);
                    return;
                }

                applyUpdate(tempFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Update check failed: " + e.getMessage());
            }
        });
    }

    private void applyUpdate(Path downloadedJar) throws IOException {
        if (pluginJarPath == null) {
            plugin.getLogger().info("Update downloaded to " + downloadedJar
                + ". Restart server to apply.");
            return;
        }

        try {
            Files.move(downloadedJar, pluginJarPath, StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Update downloaded. Restart server to apply changes.");
        } catch (IOException e) {
            plugin.getLogger().info("Update downloaded to " + downloadedJar
                + ". Restart server to apply.");
        }
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

    private String extractJsonString(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "\"" + key + "\"\\s*:\\s*\"([^\"]+)\""
        ).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private String findAssetUrl(String json, String assetName) {
        int assetsIdx = json.indexOf("\"assets\"");
        if (assetsIdx < 0) return null;
        String afterAssets = json.substring(assetsIdx);

        int bracketIdx = afterAssets.indexOf('[');
        if (bracketIdx < 0) return null;
        afterAssets = afterAssets.substring(bracketIdx);

        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < afterAssets.length(); i++) {
            char c = afterAssets.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String asset = afterAssets.substring(objStart, i + 1);
                    String name = extractJsonString(asset, "name");
                    if (name != null && name.equals(assetName)) {
                        return extractJsonString(asset, "browser_download_url");
                    }
                    objStart = -1;
                }
            }
        }
        return null;
    }
}
