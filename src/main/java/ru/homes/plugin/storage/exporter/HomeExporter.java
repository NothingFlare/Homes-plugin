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

package ru.homes.plugin.storage.exporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;
import ru.homes.plugin.storage.MySqlStorage;
import ru.homes.plugin.storage.SqliteStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Выполняет экспорт (миграцию) хомов из старого homes.yml или SQLite в MySQL.
 * Команда выполняется строго из консоли сервера.
 */
public class HomeExporter {

    public record ExportedRecord(UUID uuid, String playerName, String homeName, Home home) {}

    private final HomesPlugin plugin;

    public HomeExporter(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Асинхронно экспортирует хомы из источника в MySQL.
     *
     * @param sender      консоль, вызвавшая команду
     * @param sourceStr   homes.yml или sqlite
     * @param credentials строка логин:пароль
     */
    public void executeExportAsync(CommandSender sender, String sourceStr, String credentials) {
        CompletableFuture.runAsync(() -> {
            try {
                executeExport(sender, sourceStr, credentials);
            } catch (Exception e) {
                if (plugin != null && plugin.getLanguageManager() != null) {
                    plugin.getLanguageManager().sendMessage(sender, "export.error", "error", e.getMessage());
                } else {
                    sender.sendMessage(ChatColor.RED + "[Homes] Ошибка при экспорте хомов: " + e.getMessage());
                }
                plugin.getLogger().log(Level.SEVERE, "[Homes] Ошибка при выполнении экспорта хомов", e);
            }
        });
    }

    public void executeExport(CommandSender sender, String sourceStr, String credentials) throws Exception {
        if (plugin != null && plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().sendMessage(sender, "export.start", "source", sourceStr);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "[Homes] Начинается экспорт хомов из " + sourceStr + " в MySQL...");
        }

        // 1. Читаем записи из источника (homes.yml или sqlite)
        List<ExportedRecord> records = loadSourceRecords(sourceStr);
        if (records.isEmpty()) {
            if (plugin != null && plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(sender, "export.empty", "source", sourceStr);
            } else {
                sender.sendMessage(ChatColor.RED + "[Homes] В источнике '" + sourceStr + "' не найдено ни одного хома для экспорта!");
            }
            return;
        }

        if (plugin != null && plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().sendMessage(sender, "export.connecting", "count", records.size());
        } else {
            sender.sendMessage(ChatColor.YELLOW + "[Homes] Найдено хомов для экспорта: " + records.size() + ". Подключение к MySQL...");
        }

        // 2. Разбираем учетные данные логин:пароль
        String username;
        String password;
        int colonIdx = credentials.indexOf(':');
        if (colonIdx != -1) {
            username = credentials.substring(0, colonIdx).trim();
            password = credentials.substring(colonIdx + 1).trim();
        } else {
            username = credentials.trim();
            password = "";
        }

        // Параметры подключения к MySQL из config.yml
        String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin.getConfig().getString("storage.mysql.database", "homes");
        String prefix = plugin.getConfig().getString("storage.mysql.table-prefix", "homes_");
        boolean ssl = plugin.getConfig().getBoolean("storage.mysql.ssl", false);

        HomeEncryptor encryptor = plugin.getEncryptor();

        MySqlStorage targetStorage = null;
        try {
            targetStorage = new MySqlStorage(host, port, database, username, password, prefix, ssl, 2, encryptor, plugin.getLogger());
            targetStorage.init();

            if (plugin != null && plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(sender, "export.writing");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "[Homes] Подключение к MySQL успешно установлено. Идёт запись в базу данных...");
            }

            int exportedCount = 0;
            Set<UUID> uniquePlayers = new HashSet<>();

            for (ExportedRecord record : records) {
                targetStorage.saveHome(record.uuid(), record.playerName(), record.homeName(), record.home());
                uniquePlayers.add(record.uuid());
                exportedCount++;
            }

            // Если сервер сейчас работает на MySQL, перезагружаем кэш в памяти
            String currentStorageType = plugin.getConfig().getString("storage.type", "sqlite");
            if ("mysql".equalsIgnoreCase(currentStorageType)) {
                plugin.getHomeManager().load();
            }

            if (plugin != null && plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(sender, "export.success-header");
                plugin.getLanguageManager().sendMessage(sender, "export.success-title");
                plugin.getLanguageManager().sendMessage(sender, "export.success-stats", "count", exportedCount, "players", uniquePlayers.size());
                plugin.getLanguageManager().sendMessage(sender, "export.success-target", "host", host, "port", port, "database", database, "table", prefix + "homes");
                plugin.getLanguageManager().sendMessage(sender, "export.success-footer");
            } else {
                sender.sendMessage(ChatColor.GREEN + "====================================================");
                sender.sendMessage(ChatColor.GREEN + "[Homes] Экспорт успешно завершен!");
                sender.sendMessage(ChatColor.GREEN + "Перенесено хомов: " + exportedCount + " (игроков: " + uniquePlayers.size() + ")");
                sender.sendMessage(ChatColor.GREEN + "Целевая база: MySQL " + host + ":" + port + "/" + database + " (таблица: " + prefix + "homes)");
                sender.sendMessage(ChatColor.GREEN + "====================================================");
            }
        } finally {
            if (targetStorage != null) {
                targetStorage.close();
            }
        }
    }

    public List<ExportedRecord> loadSourceRecords(String sourceStr) throws Exception {
        String cleanSource = sourceStr.trim().toLowerCase();
        List<ExportedRecord> list = new ArrayList<>();

        if (cleanSource.startsWith("homes.yml") || cleanSource.equals("yaml") || cleanSource.equals("yml") || cleanSource.equals("legacy")) {
            File file = new File(plugin.getDataFolder(), "homes.yml");
            if (!file.exists()) {
                file = new File(plugin.getDataFolder(), "homes.yml.migrated");
            }

            if (!file.exists()) {
                throw new IllegalArgumentException("Файл homes.yml не найден в папке плагина: " + file.getAbsolutePath());
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String uuidStr : cfg.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                ConfigurationSection playerSection = cfg.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;

                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String playerName = op != null ? op.getName() : null;

                for (String homeName : playerSection.getKeys(false)) {
                    ConfigurationSection homeSection = playerSection.getConfigurationSection(homeName);
                    if (homeSection == null) continue;

                    String world = homeSection.getString("world");
                    if (world == null) continue;

                    Home home = new Home(
                            world,
                            homeSection.getDouble("x"),
                            homeSection.getDouble("y"),
                            homeSection.getDouble("z"),
                            (float) homeSection.getDouble("yaw"),
                            (float) homeSection.getDouble("pitch")
                    );

                    list.add(new ExportedRecord(uuid, playerName, homeName, home));
                }
            }
        } else if (cleanSource.startsWith("sqlite") || cleanSource.endsWith(".db")) {
            String fileName = plugin.getConfig().getString("storage.sqlite.file", "homes.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            if (!dbFile.exists()) {
                dbFile = new File(plugin.getDataFolder(), "homes.db");
            }

            if (!dbFile.exists()) {
                throw new IllegalArgumentException("Файл базы SQLite (" + dbFile.getName() + ") не найден в папке плагина!");
            }

            SqliteStorage sqliteStorage = null;
            try {
                sqliteStorage = new SqliteStorage(dbFile, plugin.getEncryptor(), plugin.getLogger());
                sqliteStorage.init();
                Map<UUID, Map<String, Home>> loaded = sqliteStorage.loadAllHomes();

                for (Map.Entry<UUID, Map<String, Home>> playerEntry : loaded.entrySet()) {
                    UUID uuid = playerEntry.getKey();
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String playerName = op != null ? op.getName() : null;

                    for (Map.Entry<String, Home> homeEntry : playerEntry.getValue().entrySet()) {
                        list.add(new ExportedRecord(uuid, playerName, homeEntry.getKey(), homeEntry.getValue()));
                    }
                }
            } finally {
                if (sqliteStorage != null) {
                    sqliteStorage.close();
                }
            }
        } else {
            throw new IllegalArgumentException("Неизвестный источник '" + sourceStr + "'. Допустимо: homes.yml или sqlite");
        }

        return list;
    }
}
