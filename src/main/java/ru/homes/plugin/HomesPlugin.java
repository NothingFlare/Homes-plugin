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
