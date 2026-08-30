package ru.homes.plugin.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;

public class HomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public HomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /home <название>");
            return true;
        }

        String name = args[0];
        Home home = homeManager.getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(ChatColor.RED + "Хом \"" + name + "\" не найден.");
            return true;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Мир хома \"" + name + "\" (" + home.getWorld() + ") не загружен.");
            return true;
        }

        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Телепортация к \"" + name + "\".");
        return true;
    }
}
