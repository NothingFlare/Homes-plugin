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

package ru.homes.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.commands.DelHomeCommand;
import ru.homes.plugin.commands.HomeCommand;
import ru.homes.plugin.commands.HomeNameTabCompleter;
import ru.homes.plugin.commands.HomesCommand;
import ru.homes.plugin.commands.HomesTabCompleter;
import ru.homes.plugin.commands.SetHomeCommand;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUIListener;
import ru.homes.plugin.security.HomeEncryptor;
import ru.homes.plugin.storage.HomeStorage;
import ru.homes.plugin.storage.StorageFactory;
import ru.homes.plugin.teleport.HomeTeleportListener;
import ru.homes.plugin.teleport.HomeTeleportManager;
import ru.homes.plugin.update.UpdateChecker;

import java.util.logging.Level;

public class HomesPlugin extends JavaPlugin {

    private HomeEncryptor encryptor;
    private HomeStorage storage;
    private HomeManager homeManager;
    private HomeTeleportManager teleportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        boolean encEnabled = getConfig().getBoolean("encryption.enabled", true);
        String encKey = getConfig().getString("encryption.secret-key", "");
        this.encryptor = new HomeEncryptor(encEnabled, encKey, getDataFolder(), getLogger());

        try {
            this.storage = StorageFactory.createStorage(this, encryptor);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "[Homes] Не удалось инициализировать хранилище данных! Плагин отключается.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.homeManager = new HomeManager(this, storage, encryptor);
        this.teleportManager = new HomeTeleportManager(this);

        getCommand("sethome").setExecutor(new SetHomeCommand(this, homeManager));
        getCommand("delhome").setExecutor(new DelHomeCommand(homeManager));
        getCommand("home").setExecutor(new HomeCommand(homeManager, teleportManager));
        getCommand("homes").setExecutor(new HomesCommand(homeManager));
        getCommand("home").setTabCompleter(new HomeNameTabCompleter(homeManager));
        getCommand("delhome").setTabCompleter(new HomeNameTabCompleter(homeManager));
        getCommand("homes").setTabCompleter(new HomesTabCompleter());

        getServer().getPluginManager().registerEvents(new HomesGUIListener(homeManager, teleportManager), this);
        getServer().getPluginManager().registerEvents(new HomeTeleportListener(teleportManager), this);

        printBanner(false);

        if (getConfig().getBoolean("check-updates", true)) {
            new UpdateChecker(getDescription().getVersion(), getLogger()).checkAsync();
        }
    }

    public void printBanner(boolean isReload) {
        getLogger().info("");
        getLogger().info("  _   _                         ");
        getLogger().info(" | | | | ___  _ __ ___   ___  ");
        getLogger().info(" | |_| |/ _ \\| '_ ` _ \\ / _ \\ ");
        getLogger().info(" |  _  | (_) | | | | | |  __/ ");
        getLogger().info(" |_| |_|\\___/|_| |_| |_|\\___| ");
        getLogger().info("");
        getLogger().info("  Hello World!");
        getLogger().info("  Check our telegram channel!");
        getLogger().info("  t.me/lighthouse_top");
        getLogger().info("  Homes version: " + getDescription().getVersion());
        String storageDisplay = getConfig().getString("storage.type", "sqlite").toUpperCase();
        if ("LEGACY".equals(storageDisplay) || "YAML".equals(storageDisplay) || "YML".equals(storageDisplay)) {
            storageDisplay = "LEGACY (YAML)";
        }
        getLogger().info("  Storage: " + storageDisplay);
        getLogger().info("  Encryption: " + (encryptor != null && encryptor.isEnabled() ? "ENABLED (AES-256-GCM)" : "DISABLED"));
        getLogger().info("  Homes is running on " + Bukkit.getName());
        if (isReload) {
            getLogger().info("  Homes is reloaded successfully!");
        } else {
            getLogger().info("  Homes is enabled successfully!");
        }
        getLogger().info("");
    }

    public boolean reloadPlugin() {
        reloadConfig();

        boolean encEnabled = getConfig().getBoolean("encryption.enabled", true);
        String encKey = getConfig().getString("encryption.secret-key", "");
        HomeEncryptor newEncryptor = new HomeEncryptor(encEnabled, encKey, getDataFolder(), getLogger());

        HomeStorage newStorage;
        try {
            newStorage = StorageFactory.createStorage(this, newEncryptor);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "[Homes] Ошибка подключения к новому хранилищу при перезагрузке! Предыдущее хранилище оставлено активным.", e);
            return false;
        }

        this.encryptor = newEncryptor;
        this.storage = newStorage;
        this.homeManager.switchStorage(newStorage, newEncryptor);

        printBanner(true);

        if (getConfig().getBoolean("check-updates", true)) {
            new UpdateChecker(getDescription().getVersion(), getLogger()).checkAsync();
        }

        return true;
    }

    @Override
    public void onDisable() {
        if (teleportManager != null) {
            teleportManager.cancelAll();
        }
        if (homeManager != null) {
            homeManager.shutdown();
        }
        getLogger().info("Homes is disabled!");
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public HomeTeleportManager getTeleportManager() {
        return teleportManager;
    }

    public HomeEncryptor getEncryptor() {
        return encryptor;
    }

    public HomeStorage getStorage() {
        return storage;
    }
}
