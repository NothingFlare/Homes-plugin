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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.HomesPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
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
 * Данные держатся в памяти (Map<UUID, Map<String, Home>>) и полностью
 * перезаписываются в файл при каждом изменении. Для сервера с несколькими
 * тысячами хомов этого достаточно; при бОльших объёмах имеет смысл
 * переписать на построчную запись, но для типового SMP это не нужно.
 */
public class HomeManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Home>> homes = new LinkedHashMap<>();
    private boolean saveQueued;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    public HomesPlugin getPlugin() {
        return (HomesPlugin) plugin;
    }

    public void load() {
        homes.clear();
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
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
            homes.put(uuid, playerHomes);
        }
    }

    /**
     * Сохраняет homes.yml. Сборка YamlConfiguration из in-memory карты происходит
     * синхронно (это просто чтение Map, без обращения к диску), а сама запись
     * файла уходит в отдельный поток, чтобы не блокировать основной поток сервера
     * на каждый /sethome и /delhome.
     */
    public synchronized void save() {
        if (saveQueued) return;
        saveQueued = true;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            YamlConfiguration cfg;
            synchronized (HomeManager.this) {
                cfg = buildConfig();
                saveQueued = false;
            }

            try {
                plugin.getDataFolder().mkdirs();
                cfg.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить homes.yml", e);
            }
        });
    }

    /**
     * Синхронный вариант save(), используется только в onDisable(), где сервер
     * в любом случае завершает работу и асинхронные задачи могут не успеть выполниться.
     */
    public void saveSync() {
        YamlConfiguration cfg = buildConfig();
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить homes.yml", e);
        }
    }

    private YamlConfiguration buildConfig() {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Home>> playerEntry : homes.entrySet()) {
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
        if (removed) save();
        return removed;
    }

    public synchronized Home getHome(UUID uuid, String name) {
        Map<String, Home> playerHomes = homes.get(uuid);
        if (playerHomes == null) return null;
        return playerHomes.get(name);
    }

    public synchronized Map<String, Home> getHomes(UUID uuid) {
        return homes.getOrDefault(uuid, Map.of());
    }
}
