package ru.homes.plugin.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.data.HomeManager;

public class DelHomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public DelHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /delhome <название>");
            return true;
        }

        String name = args[0];
        boolean removed = homeManager.delHome(player.getUniqueId(), name);
        if (removed) {
            player.sendMessage(ChatColor.GREEN + "Хом \"" + name + "\" удалён.");
        } else {
            player.sendMessage(ChatColor.RED + "Хом \"" + name + "\" не найден.");
        }
        return true;
    }
}
