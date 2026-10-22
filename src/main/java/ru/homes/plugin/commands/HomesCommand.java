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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUI;
import ru.homes.plugin.storage.exporter.HomeExporter;

public class HomesCommand implements CommandExecutor {

    private final HomeManager homeManager;
    private final HomesPlugin plugin;

    public HomesCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
        this.plugin = homeManager.getPlugin();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка: команду /homes add ... может вызывать ТОЛЬКО консоль
        if (args.length > 0 && args[0].equalsIgnoreCase("add")) {
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.RED + "Команду /homes add можно выполнять только из консоли!");
                return true;
            }

            // Формат 1: /homes add <homes.yml|sqlite> to MySQL <логин:пароль> (5 аргументов)
            // Формат 2: /homes add <homes.yml|sqlite> MySQL <логин:пароль>    (4 аргумента)
            String source;
            String target;
            String credentials;

            if (args.length >= 5 && args[2].equalsIgnoreCase("to")) {
                source = args[1];
                target = args[3];
                credentials = args[4];
            } else if (args.length >= 4) {
                source = args[1];
                target = args[2];
                credentials = args[3];
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Использование: /homes add <homes.yml|sqlite> to MySQL <логин:пароль>");
                sender.sendMessage(ChatColor.GRAY + "Пример: /homes add homes.yml to MySQL root:secret123");
                sender.sendMessage(ChatColor.GRAY + "Пример: /homes add sqlite to MySQL root:secret123");
                return true;
            }

            if (!target.equalsIgnoreCase("mysql")) {
                sender.sendMessage(ChatColor.RED + "Неподдерживаемая целевая база: " + target + ". Поддерживается: MySQL");
                return true;
            }

            new HomeExporter(plugin).executeExportAsync(sender, source, credentials);
            return true;
        }

        // Если команду вызывает игрок без аргументов — открываем GUI хомов
        if (sender instanceof Player player) {
            if (args.length > 0) {
                player.sendMessage(ChatColor.YELLOW + "Использование: /homes");
                return true;
            }
            HomesGUI.open(player, plugin);
            return true;
        }

        // Если консоль вызвала /homes без аргументов
        sender.sendMessage(ChatColor.RED + "Команда /homes (GUI) доступна только игрокам.");
        sender.sendMessage(ChatColor.YELLOW + "Использование для консоли: /homes add <homes.yml|sqlite> to MySQL <логин:пароль>");
        return true;
    }
}
