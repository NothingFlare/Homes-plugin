package ru.homes.plugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.homes.plugin.data.HomeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Общий tab-completer для команд /home и /delhome: подсказывает названия
 * хомов игрока, который вводит команду, отфильтрованные по уже набранному
 * тексту (регистронезависимо).
 */
public class HomeNameTabCompleter implements TabCompleter {

    private final HomeManager homeManager;

    public HomeNameTabCompleter(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        // Подсказки нужны только для первого аргумента (название хома).
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase();
        List<String> suggestions = new ArrayList<>();
        for (String name : homeManager.getHomes(player.getUniqueId()).keySet()) {
            if (name.toLowerCase().startsWith(prefix)) {
                suggestions.add(name);
            }
        }

        Collections.sort(suggestions);
        return suggestions;
    }
}
