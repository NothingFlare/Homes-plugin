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
        if (sender.hasPermission("homes.reload") && "reload".startsWith(prefix)) {
            suggestions.add("reload");
        }
        for (String name : homeManager.getHomes(player.getUniqueId()).keySet()) {
            if (name.toLowerCase().startsWith(prefix)) {
                suggestions.add(name);
            }
        }

        Collections.sort(suggestions);
        return suggestions;
    }
}
