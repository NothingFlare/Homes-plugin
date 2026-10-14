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
import ru.homes.plugin.commands.SetHomeCommand;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUIListener;
import ru.homes.plugin.teleport.HomeTeleportManager;
import ru.homes.plugin.teleport.HomeTeleportListener;

public class HomesPlugin extends JavaPlugin {

    private HomeManager homeManager;
    private HomeTeleportManager teleportManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.homeManager = new HomeManager(this);
        this.teleportManager = new HomeTeleportManager(this);

        // Конфигурация читается командами напрямую, поэтому после /home reload
        // новые значения применяются без перезапуска сервера.
        getCommand("sethome").setExecutor(new SetHomeCommand(this, homeManager));
        getCommand("delhome").setExecutor(new DelHomeCommand(homeManager));
        getCommand("home").setExecutor(new HomeCommand(homeManager, teleportManager));
        getCommand("homes").setExecutor(new HomesCommand(homeManager));
        getCommand("home").setTabCompleter(new HomeNameTabCompleter(homeManager));
        getCommand("delhome").setTabCompleter(new HomeNameTabCompleter(homeManager));

        getServer().getPluginManager().registerEvents(new HomesGUIListener(homeManager, teleportManager), this);
        getServer().getPluginManager().registerEvents(new HomeTeleportListener(teleportManager), this);

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
        getLogger().info("  Homes version: 1.4");
        getLogger().info("  Homes is running on " + Bukkit.getName());
        getLogger().info("  Homes is enabled successfully!");
        getLogger().info("");
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
}
