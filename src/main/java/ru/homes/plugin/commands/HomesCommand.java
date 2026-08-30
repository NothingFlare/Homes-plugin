package ru.homes.plugin.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUI;

public class HomesCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public HomesCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков.");
            return true;
        }

        if (homeManager.getHomes(player.getUniqueId()).isEmpty()) {
            player.sendMessage(ChatColor.RED + "У тебя нет ни одного хома. Поставь через /sethome <название>.");
            return true;
        }

        player.openInventory(HomesGUI.build(player, homeManager));
        return true;
    }
}
