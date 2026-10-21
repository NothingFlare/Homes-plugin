/*
 * MIT License
 *
 * Copyright (c) 2026 NothingFlare
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ru.homes.plugin.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class UpdateChecker {

    public static final String MODRINTH_VERSIONS_URL = "https://modrinth.com/plugin/homes-nothingflare/versions";
    public static final String GITHUB_RELEASES_URL = "https://github.com/NothingFlare/Homes-plugin/releases";

    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/homes-nothingflare/version";
    private static final String GITHUB_API = "https://api.github.com/repos/NothingFlare/Homes-plugin/releases/latest";

    private final String currentVersion;
    private final Logger logger;
    private final HttpClient httpClient;

    public UpdateChecker(String currentVersion, Logger logger) {
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Запускает асинхронную проверку обновлений через API Modrinth и GitHub.
     */
    public CompletableFuture<Void> checkAsync() {
        return CompletableFuture.runAsync(this::checkAndLog);
    }

    /**
     * Синхронная проверка версии с выводом результата в консоль.
     */
    public void checkAndLog() {
        try {
            logger.info("[Homes] Checking for updates...");
            String remoteVersion = fetchLatestVersion();

            if (remoteVersion == null || remoteVersion.trim().isEmpty()) {
                logger.warning("[Homes] Could not retrieve the latest version from Modrinth/GitHub API.");
                return;
            }

            int comparison = compareVersions(remoteVersion, currentVersion);
            if (comparison > 0) {
                logger.warning("****************************************************************");
                logger.warning("[Homes] A new version is available: v" + remoteVersion + " (Current: v" + currentVersion + ")");
                logger.warning("[Homes] Download the update at:");
                logger.warning("[Homes]   Modrinth: " + MODRINTH_VERSIONS_URL);
                logger.warning("[Homes]   GitHub:   " + GITHUB_RELEASES_URL);
                logger.warning("****************************************************************");
            } else {
                logger.info("[Homes] You running on the Latest version (v" + currentVersion + ")");
            }
        } catch (Exception e) {
            logger.warning("[Homes] Could not check for updates: " + e.getMessage());
        }
    }

    /**
     * Пытается получить последнюю версию сначала из Modrinth API, а при неудаче — из GitHub API.
     */
    public String fetchLatestVersion() {
        String fromModrinth = fetchFromModrinth();
        if (fromModrinth != null && !fromModrinth.trim().isEmpty()) {
            return fromModrinth;
        }

        return fetchFromGitHub();
    }

    private String fetchFromModrinth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODRINTH_API))
                    .header("User-Agent", "NothingFlare/Homes-plugin/" + currentVersion)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                if (root.isJsonArray()) {
                    JsonArray array = root.getAsJsonArray();
                    if (array.size() > 0) {
                        JsonObject latest = array.get(0).getAsJsonObject();
                        if (latest.has("version_number")) {
                            return latest.get("version_number").getAsString().trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Ошибка сети или таймаут — попробуем GitHub
        }
        return null;
    }

    private String fetchFromGitHub() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("User-Agent", "NothingFlare/Homes-plugin/" + currentVersion)
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                if (obj.has("tag_name")) {
                    return obj.get("tag_name").getAsString().trim();
                }
            }
        } catch (Exception ignored) {
            // Ошибка сети или лимит запросов GitHub API
        }
        return null;
    }

    /**
     * Сравнивает две строки версий (например "2.0" и "1.4" или "v2.1" и "2.0.0").
     * Возвращает положительное число, если v1 > v2.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0";
        if (v2 == null) v2 = "0";

        String clean1 = v1.trim().replaceAll("^[vV]", "");
        String clean2 = v2.trim().replaceAll("^[vV]", "");

        String[] parts1 = clean1.split("[.\\-_]");
        String[] parts2 = clean2.split("[.\\-_]");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                String p1 = parts1[i].replaceAll("\\D+", "");
                if (!p1.isEmpty()) {
                    try {
                        num1 = Integer.parseInt(p1);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (i < parts2.length) {
                String p2 = parts2[i].replaceAll("\\D+", "");
                if (!p2.isEmpty()) {
                    try {
                        num2 = Integer.parseInt(p2);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }
}
