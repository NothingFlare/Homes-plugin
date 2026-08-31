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

import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.commands.DelHomeCommand;
import ru.homes.plugin.commands.HomeCommand;
import ru.homes.plugin.commands.HomeNameTabCompleter;
import ru.homes.plugin.commands.HomesCommand;
import ru.homes.plugin.commands.SetHomeCommand;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUIListener;

public class HomesPlugin extends JavaPlugin {

    private HomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.homeManager = new HomeManager(this);

        // homeLimit больше не фиксируется здесь: SetHomeCommand сам читает
        // home-limit из конфига при каждом вызове, поэтому /reload подхватывает
        // изменения без рестарта сервера.
        getCommand("sethome").setExecutor(new SetHomeCommand(this, homeManager));
        getCommand("delhome").setExecutor(new DelHomeCommand(homeManager));
        getCommand("home").setExecutor(new HomeCommand(homeManager));
        getCommand("homes").setExecutor(new HomesCommand(homeManager));

        HomeNameTabCompleter tabCompleter = new HomeNameTabCompleter(homeManager);
        getCommand("home").setTabCompleter(tabCompleter);
        getCommand("delhome").setTabCompleter(tabCompleter);

        getServer().getPluginManager().registerEvents(new HomesGUIListener(homeManager), this);

        getLogger().info("HomesPlugin включён, homes.yml загружен.");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            // При выключении сервера асинхронные задачи планировщика уже не
            // гарантированно выполняются, поэтому здесь пишем файл синхронно.
            homeManager.saveSync();
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }
}
