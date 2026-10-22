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

package ru.homes.plugin.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация хранилища хомов в классическом формате homes.yml (Legacy).
 * Поддерживает как классический текстовый формат, так и шифрование AES-256-GCM.
 * Запись на диск выполняется атомарно через временный файл для защиты от повреждений при краше.
 */
public class YamlStorage implements HomeStorage {

    private final File file;
    private final HomeEncryptor encryptor;
    private final Logger logger;
    private final Object fileLock = new Object();

    public YamlStorage(File file, HomeEncryptor encryptor, Logger logger) {
        this.file = file;
        this.encryptor = encryptor;
        this.logger = logger != null ? logger : Logger.getLogger(getClass().getName());
    }

    @Override
    public void init() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    @Override
    public Map<UUID, Map<String, Home>> loadAllHomes() throws Exception {
        Map<UUID, Map<String, Home>> result = new LinkedHashMap<>();
        if (!file.exists()) {
            return result;
        }

        YamlConfiguration cfg;
        synchronized (fileLock) {
            cfg = YamlConfiguration.loadConfiguration(file);
        }

        for (String uuidStr : cfg.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                logger.warning("[Homes] Пропуск некорректного UUID в homes.yml: " + uuidStr);
                continue;
            }

            ConfigurationSection playerSection = cfg.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            Map<String, Home> playerHomes = new LinkedHashMap<>();
            for (String homeName : playerSection.getKeys(false)) {
                ConfigurationSection homeSection = playerSection.getConfigurationSection(homeName);
                if (homeSection == null) continue;

                // 1. Проверяем наличие зашифрованных данных
                if (homeSection.contains("data")) {
                    String rawData = homeSection.getString("data");
                    try {
                        Home home = encryptor.decrypt(uuid, rawData);
                        if (home != null) {
                            playerHomes.put(homeName, home);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "[Homes] Ошибка расшифровки хома '" + homeName + "' в YAML", e);
                    }
                    continue;
                }

                // 2. Классический незашифрованный формат YAML
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
                result.put(uuid, playerHomes);
            }
        }

        return result;
    }

    @Override
    public void saveHome(UUID playerUuid, String playerName, String homeName, Home home) throws Exception {
        synchronized (fileLock) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String path = playerUuid.toString() + "." + homeName;

            if (encryptor.isEnabled()) {
                String encryptedData = encryptor.encrypt(playerUuid, home);
                cfg.set(path + ".data", encryptedData);
                if (playerName != null) {
                    cfg.set(path + ".player_name", playerName);
                }
                // Очищаем старые поля координат, если они были
                cfg.set(path + ".world", null);
                cfg.set(path + ".x", null);
                cfg.set(path + ".y", null);
                cfg.set(path + ".z", null);
                cfg.set(path + ".yaw", null);
                cfg.set(path + ".pitch", null);
            } else {
                cfg.set(path + ".world", home.getWorld());
                cfg.set(path + ".x", home.getX());
                cfg.set(path + ".y", home.getY());
                cfg.set(path + ".z", home.getZ());
                cfg.set(path + ".yaw", home.getYaw());
                cfg.set(path + ".pitch", home.getPitch());
                if (playerName != null) {
                    cfg.set(path + ".player_name", playerName);
                }
                cfg.set(path + ".data", null);
            }

            saveAtomic(cfg);
        }
    }

    @Override
    public void deleteHome(UUID playerUuid, String homeName) throws Exception {
        synchronized (fileLock) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String playerPath = playerUuid.toString();
            cfg.set(playerPath + "." + homeName, null);

            ConfigurationSection sec = cfg.getConfigurationSection(playerPath);
            if (sec != null && sec.getKeys(false).isEmpty()) {
                cfg.set(playerPath, null);
            }

            saveAtomic(cfg);
        }
    }

    @Override
    public int deleteAllHomes(UUID playerUuid) throws Exception {
        synchronized (fileLock) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String playerPath = playerUuid.toString();
            ConfigurationSection sec = cfg.getConfigurationSection(playerPath);
            int count = (sec != null) ? sec.getKeys(false).size() : 0;

            if (count > 0) {
                cfg.set(playerPath, null);
                saveAtomic(cfg);
            }
            return count;
        }
    }

    @Override
    public int clearAllHomes() throws Exception {
        synchronized (fileLock) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            int count = 0;
            for (String key : cfg.getKeys(false)) {
                ConfigurationSection sec = cfg.getConfigurationSection(key);
                if (sec != null) {
                    count += sec.getKeys(false).size();
                }
            }

            YamlConfiguration fresh = new YamlConfiguration();
            saveAtomic(fresh);
            return count;
        }
    }

    @Override
    public UUID findUuidByPlayerName(String playerName) throws Exception {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }

        YamlConfiguration cfg;
        synchronized (fileLock) {
            cfg = YamlConfiguration.loadConfiguration(file);
        }

        String search = playerName.trim().toLowerCase();
        for (String uuidStr : cfg.getKeys(false)) {
            ConfigurationSection playerSection = cfg.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            for (String homeName : playerSection.getKeys(false)) {
                String nameInCfg = playerSection.getString(homeName + ".player_name");
                if (nameInCfg != null && nameInCfg.equalsIgnoreCase(search)) {
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return null;
    }

    @Override
    public void close() {
        // Для YAML хранилища закрытие пулов соединений не требуется
    }

    private void saveAtomic(YamlConfiguration cfg) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tempFile = new File(parent, file.getName() + ".tmp");
        try {
            cfg.save(tempFile);
            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public File getFile() {
        return file;
    }
}
