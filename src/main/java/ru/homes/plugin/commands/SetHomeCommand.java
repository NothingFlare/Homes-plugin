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

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.homes.plugin.data.HomeManager;

public class SetHomeCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final HomeManager homeManager;

    public SetHomeCommand(JavaPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /sethome <название>");
            return true;
        }

        String name = args[0];
        if (name.length() > 32) {
            player.sendMessage(ChatColor.RED + "Название хома слишком длинное (макс. 32 символа).");
            return true;
        }

        boolean alreadyExists = homeManager.getHome(player.getUniqueId(), name) != null;
        int currentCount = homeManager.getHomes(player.getUniqueId()).size();
        // Читается каждый вызов, а не сохраняется на onEnable — /reload сразу
        // применяет новое значение home-limit из config.yml.
        int homeLimit = plugin.getConfig().getInt("home-limit", 10);

        if (!alreadyExists && currentCount >= homeLimit && !player.hasPermission("homes.bypass")) {
            player.sendMessage(ChatColor.RED + "Лимит хомов исчерпан (" + currentCount + "/" + homeLimit
                    + "). Удали ненужный через /delhome <название>.");
            return true;
        }

        boolean overwritten = homeManager.setHome(player.getUniqueId(), name, player.getLocation());
        if (overwritten) {
            player.sendMessage(ChatColor.GREEN + "Хом \"" + name + "\" перезаписан.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Хом \"" + name + "\" установлен.");
        }
        return true;
    }
}
