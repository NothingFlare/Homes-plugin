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

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.HomesPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Хранит хомы в файле homes.yml в структуре:
 *
 * <uuid>:
 *   <homename>:
 *     world: world
 *     x: 1.0
 *     y: 2.0
 *     z: 3.0
 *     yaw: 0.0
 *     pitch: 0.0
 *
 * Данные хранятся в памяти (Map<UUID, Map<String, Home>>) и атомарно
 * сохраняются на диск через отдельный фоновый поток (SingleThreadExecutor),
 * что обеспечивает потокобезопасность и совместимость со Spigot, Paper и Folia.
 */
public class HomeManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Home>> homes = new LinkedHashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Homes-IO-Worker");
        thread.setDaemon(true);
        return thread;
    });
    private boolean saveQueued;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    public HomesPlugin getPlugin() {
        return (HomesPlugin) plugin;
    }

    public synchronized void load() {
        homes.clear();
        if (!file.exists()) {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String uuidStr : cfg.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Пропускаю некорректный UUID в homes.yml: " + uuidStr);
                continue;
            }

            ConfigurationSection playerSection = cfg.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<String, Home> playerHomes = new LinkedHashMap<>();
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
            }
            if (!playerHomes.isEmpty()) {
                homes.put(uuid, playerHomes);
            }
        }
    }

    /**
     * Планирует асинхронное сохранение homes.yml.
     * Задачи выполняются строго последовательно в SingleThreadExecutor.
     * Снимок данных создается синхронно, а запись и атомарная перезапись на диск происходят асинхронно.
     */
    public synchronized void save() {
        if (saveQueued) return;
        saveQueued = true;
        ioExecutor.execute(this::executeAsyncSave);
    }

    private void executeAsyncSave() {
        Map<UUID, Map<String, Home>> snapshot;
        synchronized (this) {
            saveQueued = false;
            snapshot = snapshotHomes();
        }
        writeSnapshotToFile(snapshot);
    }

    /**
     * Синхронная запись снимка данных на диск.
     */
    public synchronized void saveSync() {
        writeSnapshotToFile(snapshotHomes());
    }

    /**
     * Корректное завершение пула потоков при отключении плагина.
     */
    public void shutdown() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
                saveSync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            saveSync();
        }
    }

    private Map<UUID, Map<String, Home>> snapshotHomes() {
        Map<UUID, Map<String, Home>> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Home>> entry : homes.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    private void writeSnapshotToFile(Map<UUID, Map<String, Home>> snapshot) {
        YamlConfiguration cfg = buildConfig(snapshot);
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File tempFile = new File(dataFolder, "homes.yml.tmp");
        try {
            cfg.save(tempFile);
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить homes.yml", e);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private YamlConfiguration buildConfig(Map<UUID, Map<String, Home>> data) {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Home>> playerEntry : data.entrySet()) {
            if (playerEntry.getValue().isEmpty()) continue;

            ConfigurationSection playerSection = cfg.createSection(playerEntry.getKey().toString());
            for (Map.Entry<String, Home> homeEntry : playerEntry.getValue().entrySet()) {
                ConfigurationSection homeSection = playerSection.createSection(homeEntry.getKey());
                Home h = homeEntry.getValue();
                homeSection.set("world", h.getWorld());
                homeSection.set("x", h.getX());
                homeSection.set("y", h.getY());
                homeSection.set("z", h.getZ());
                homeSection.set("yaw", h.getYaw());
                homeSection.set("pitch", h.getPitch());
            }
        }

        return cfg;
    }

    public synchronized boolean setHome(UUID uuid, String name, Location loc) {
        Map<String, Home> playerHomes = homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        boolean overwritten = playerHomes.containsKey(name);
        playerHomes.put(name, Home.fromLocation(loc));
        save();
        return overwritten;
    }

    public synchronized boolean delHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        boolean removed = playerHomes.remove(name) != null;
        if (removed) {
            if (playerHomes.isEmpty()) {
                homes.remove(uuid);
            }
            save();
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
}
