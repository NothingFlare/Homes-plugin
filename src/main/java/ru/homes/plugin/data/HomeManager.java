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

package ru.homes.plugin.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.security.HomeEncryptor;
import ru.homes.plugin.storage.HomeStorage;
import ru.homes.plugin.storage.YamlStorage;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Управляет хомами игроков.
 * Данные кэшируются в оперативной памяти для мгновенного доступа без лагов и задержек,
 * а операции записи и удаления асинхронно сохраняются в базу данных (MySQL / SQLite).
 */
public class HomeManager {

    private final JavaPlugin plugin;
    private volatile HomeStorage storage;
    private volatile HomeEncryptor encryptor;

    private final Map<UUID, Map<String, Home>> homes = new LinkedHashMap<>();
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Homes-DB-Worker");
        thread.setDaemon(true);
        return thread;
    });

    public HomeManager(JavaPlugin plugin, HomeStorage storage, HomeEncryptor encryptor) {
        this.plugin = plugin;
        this.storage = storage;
        this.encryptor = encryptor;
        load();
    }

    public HomesPlugin getPlugin() {
        return (HomesPlugin) plugin;
    }

    public HomeStorage getStorage() {
        return storage;
    }

    public HomeEncryptor getEncryptor() {
        return encryptor;
    }

    /**
     * Безопасно переключает используемое хранилище и шифратор на лету.
     * Завершает все ожидающие записи в старое хранилище, закрывает его и загружает данные из нового.
     */
    public synchronized void switchStorage(HomeStorage newStorage, HomeEncryptor newEncryptor) {
        HomeStorage oldStorage = this.storage;
        this.storage = newStorage;
        this.encryptor = newEncryptor;

        if (oldStorage != null) {
            try {
                // Дожидаемся завершения ранее запланированных операций записи перед закрытием
                ioExecutor.submit(oldStorage::close).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Homes] Ошибка при корректном закрытии предыдущего хранилища", e);
            }
        }

        load();
    }

    /**
     * Загружает данные из базы и при необходимости мигрирует устаревший homes.yml.
     */
    public synchronized void load() {
        homes.clear();
        if (storage != null) {
            try {
                Map<UUID, Map<String, Home>> fromDb = storage.loadAllHomes();
                homes.putAll(fromDb);
                plugin.getLogger().info("[Homes] Загружено " + countTotalHomes(homes) + " хомов из базы данных.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[Homes] Ошибка при загрузке хомов из базы данных!", e);
            }
        }

        // Проверяем возможность миграции со старого homes.yml
        migrateFromYamlIfPresent();
    }

    private int countTotalHomes(Map<UUID, Map<String, Home>> map) {
        int count = 0;
        for (Map<String, Home> entry : map.values()) {
            count += entry.size();
        }
        return count;
    }

    /**
     * Однократная безопасная миграция из старого homes.yml в базу данных.
     */
    private void migrateFromYamlIfPresent() {
        if (storage instanceof YamlStorage) {
            return;
        }

        File oldFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!oldFile.exists()) {
            return;
        }

        // Миграцию производим, если база данных пуста
        if (!homes.isEmpty()) {
            return;
        }

        plugin.getLogger().info("[Homes] Обнаружен старый файл homes.yml. Выполняется автоматическая миграция в базу данных...");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(oldFile);
        int migratedCount = 0;

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

            Map<String, Home> playerHomes = homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>());

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

                playerHomes.put(homeName, home);
                migratedCount++;

                if (storage != null) {
                    try {
                        storage.saveHome(uuid, playerName, homeName, home);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения мигрированного хома " + homeName, e);
                    }
                }
            }
        }

        plugin.getLogger().info("[Homes] Миграция успешно завершена! Перенесено " + migratedCount + " хомов.");
        File backupFile = new File(plugin.getDataFolder(), "homes.yml.migrated");
        oldFile.renameTo(backupFile);
    }

    public synchronized boolean setHome(UUID uuid, String playerName, String name, Location loc) {
        if (playerName != null) {
            playerNames.put(uuid, playerName);
        }
        Map<String, Home> playerHomes = homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        boolean overwritten = playerHomes.containsKey(name);
        Home home = Home.fromLocation(loc);
        playerHomes.put(name, home);

        if (storage != null) {
            ioExecutor.execute(() -> {
                try {
                    storage.saveHome(uuid, playerName, name, home);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка асинхронного сохранения хома " + name + " игрока " + uuid, e);
                }
            });
        }

        return overwritten;
    }

    public synchronized boolean setHome(UUID uuid, String name, Location loc) {
        String nameFromCache = playerNames.get(uuid);
        if (nameFromCache == null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op != null) {
                nameFromCache = op.getName();
            }
        }
        return setHome(uuid, nameFromCache, name, loc);
    }

    public synchronized boolean delHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;

        boolean removed = playerHomes.remove(name) != null;
        if (removed) {
            if (playerHomes.isEmpty()) {
                homes.remove(uuid);
            }
            if (storage != null) {
                ioExecutor.execute(() -> {
                    try {
                        storage.deleteHome(uuid, name);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка асинхронного удаления хома " + name + " игрока " + uuid, e);
                    }
                });
            }
        }
        return removed;
    }

    public synchronized Home getHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = homes.get(uuid);
        if (playerHomes == null) return null;
        return playerHomes.get(name);
    }

    public synchronized Map<String, Home> getHomes(UUID uuid) {
        Map<String, Home> playerHomes = homes.get(uuid);
        if (playerHomes == null || playerHomes.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(playerHomes));
    }

    /**
     * Очищает все хомы конкретного игрока по нику или UUID.
     * Вызывается консольной командой /home clear <ник>.
     * Возвращает количество удаленных хомов.
     */
    public synchronized int clearPlayerHomes(String target) {
        if (target == null || target.trim().isEmpty()) {
            return 0;
        }

        UUID targetUuid = resolvePlayerUuid(target.trim());
        if (targetUuid == null) {
            return 0;
        }

        Map<String, Home> removed = homes.remove(targetUuid);
        int inMemoryCount = (removed != null ? removed.size() : 0);
        int dbDeletedCount = 0;

        if (storage != null) {
            try {
                Future<Integer> future = ioExecutor.submit(() -> storage.deleteAllHomes(targetUuid));
                dbDeletedCount = future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при удалении хомов игрока " + target + " из базы данных", e);
            }
        }

        return Math.max(inMemoryCount, dbDeletedCount);
    }

    /**
     * Очищает все хомы абсолютно всех игроков на сервере.
     * Вызывается консольной командой /home clear all.
     * Возвращает общее количество удаленных хомов.
     */
    public synchronized int clearAllHomes() {
        int inMemoryCount = countTotalHomes(homes);
        homes.clear();
        int dbDeletedCount = 0;

        if (storage != null) {
            try {
                Future<Integer> future = ioExecutor.submit(storage::clearAllHomes);
                dbDeletedCount = future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при полной очистке базы хомов", e);
            }
        }

        return Math.max(inMemoryCount, dbDeletedCount);
    }

    /**
     * Определяет UUID игрока по нику или строке UUID.
     */
    public UUID resolvePlayerUuid(String target) {
        // 1. Попытка распарсить как явный UUID
        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException ignored) {}

        // 2. Поиск среди онлайн-игроков
        Player onlinePlayer = Bukkit.getPlayerExact(target);
        if (onlinePlayer != null) {
            playerNames.put(onlinePlayer.getUniqueId(), onlinePlayer.getName());
            return onlinePlayer.getUniqueId();
        }

        // 3. Поиск по локальному кэшу ников
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(target)) {
                return entry.getKey();
            }
        }

        // 4. Поиск по базе данных
        if (storage != null) {
            try {
                UUID fromDb = storage.findUuidByPlayerName(target);
                if (fromDb != null) {
                    playerNames.put(fromDb, target);
                    return fromDb;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка поиска UUID игрока " + target + " в базе", e);
            }
        }

        // 5. Поиск среди офлайн-игроков сервера Bukkit
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        if (op != null && (op.hasPlayedBefore() || homes.containsKey(op.getUniqueId()))) {
            return op.getUniqueId();
        }

        return null;
    }

    /**
     * Завершение работы менеджера при отключении плагина.
     */
    public void shutdown() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (storage != null) {
            storage.close();
        }
    }
}
