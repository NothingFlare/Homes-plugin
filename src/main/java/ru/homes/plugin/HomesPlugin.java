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
import ru.homes.plugin.language.LanguageManager;
import ru.homes.plugin.platform.PlatformManager;
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
    private LanguageManager languageManager;
    private PlatformManager platformManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.platformManager = new PlatformManager(this);
        this.languageManager = new LanguageManager(this);
        this.languageManager.load();

        boolean encEnabled = getConfig().getBoolean("encryption.enabled", true);
        String encKey = getConfig().getString("encryption.secret-key", "");
        this.encryptor = new HomeEncryptor(encEnabled, encKey, getDataFolder(), getLogger());

        try {
            this.storage = StorageFactory.createStorage(this, encryptor);
        } catch (Exception e) {
            String failMsg = languageManager != null
                    ? languageManager.get("console.init-failed")
                    : "[Homes] Failed to initialize data storage! Plugin is shutting down.";
            getLogger().log(Level.SEVERE, failMsg, e);
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
            new UpdateChecker(getDescription().getVersion(), getLogger(), languageManager).checkAsync();
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
        getLogger().info(languageManager != null ? languageManager.get("banner.hello") : "  Hello World!");
        getLogger().info(languageManager != null ? languageManager.get("banner.telegram") : "  Check our telegram channel!");
        getLogger().info("  t.me/lighthouse_top");
        getLogger().info(languageManager != null ? languageManager.get("banner.version", "version", getDescription().getVersion()) : "  Homes version: " + getDescription().getVersion());
        getLogger().info(languageManager != null ? languageManager.get("banner.language", "language", languageManager.getCurrentLanguage().toUpperCase(java.util.Locale.ROOT)) : "  Language: " + (languageManager != null ? languageManager.getCurrentLanguage().toUpperCase(java.util.Locale.ROOT) : "RU"));
        String storageDisplay = getConfig().getString("storage.type", "sqlite").toUpperCase();
        if ("LEGACY".equals(storageDisplay) || "YAML".equals(storageDisplay) || "YML".equals(storageDisplay)) {
            storageDisplay = "LEGACY (YAML)";
        }
        getLogger().info(languageManager != null ? languageManager.get("banner.storage", "storage", storageDisplay) : "  Storage: " + storageDisplay);
        String encStatus = (encryptor != null && encryptor.isEnabled())
                ? (languageManager != null ? languageManager.get("banner.encryption-enabled") : "ENABLED (AES-256-GCM)")
                : (languageManager != null ? languageManager.get("banner.encryption-disabled") : "DISABLED");
        getLogger().info(languageManager != null ? languageManager.get("banner.encryption", "status", encStatus) : "  Encryption: " + encStatus);
        String platformName = platformManager != null ? platformManager.getPlatformType().getFriendlyName() : Bukkit.getName();
        String serverVer = platformManager != null ? platformManager.getServerVersion().getFriendlyName() : "Unknown";
        String adapterName = platformManager != null ? platformManager.getAdapter().getAdapterName() : "Standard";
        getLogger().info(languageManager != null ? languageManager.get("banner.platform", "platform", platformName) : "  Platform: " + platformName);
        getLogger().info("  Version: " + serverVer + " (" + Bukkit.getName() + ")");
        getLogger().info("  Adapter: " + adapterName);
        if (isReload) {
            getLogger().info(languageManager != null ? languageManager.get("banner.reloaded") : "  Homes is reloaded successfully!");
        } else {
            getLogger().info(languageManager != null ? languageManager.get("banner.enabled") : "  Homes is enabled successfully!");
        }
        getLogger().info("");
    }

    public boolean reloadPlugin() {
        reloadConfig();

        if (this.languageManager != null) {
            this.languageManager.reload();
        }

        boolean encEnabled = getConfig().getBoolean("encryption.enabled", true);
        String encKey = getConfig().getString("encryption.secret-key", "");
        HomeEncryptor newEncryptor = new HomeEncryptor(encEnabled, encKey, getDataFolder(), getLogger());

        HomeStorage newStorage;
        try {
            newStorage = StorageFactory.createStorage(this, newEncryptor);
        } catch (Exception e) {
            String errMsg = languageManager != null
                    ? languageManager.get("console.storage-reload-error")
                    : "[Homes] Error connecting to new storage on reload! Previous storage kept active.";
            getLogger().log(Level.SEVERE, errMsg, e);
            return false;
        }

        this.encryptor = newEncryptor;
        this.storage = newStorage;
        this.homeManager.switchStorage(newStorage, newEncryptor);

        printBanner(true);

        if (getConfig().getBoolean("check-updates", true)) {
            new UpdateChecker(getDescription().getVersion(), getLogger(), languageManager).checkAsync();
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
        getLogger().info(languageManager != null ? languageManager.get("console.plugin-disabled") : "[Homes] Homes is disabled!");
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

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public PlatformManager getPlatformManager() {
        return platformManager;
    }
}
