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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.gui.HomesGUI;
import ru.homes.plugin.language.LanguageManager;
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
        LanguageManager lm = plugin.getLanguageManager();

        // Проверка: команду /homes add ... может вызывать ТОЛЬКО консоль
        if (args.length > 0 && (args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("добавить")
                || args[0].equalsIgnoreCase("export")
                || args[0].equalsIgnoreCase("экспорт"))) {
            if (!(sender instanceof ConsoleCommandSender)) {
                lm.sendMessage(sender, "export.console-only");
                return true;
            }

            // Формат 1: /homes add <homes.yml|sqlite|legacy> to MySQL <логин:пароль> (5 аргументов)
            // Формат 2: /homes add <homes.yml|sqlite|legacy> MySQL <логин:пароль>    (4 аргумента)
            String source;
            String target;
            String credentials;

            if (args.length >= 5 && (args[2].equalsIgnoreCase("to") || args[2].equalsIgnoreCase("в") || args[2].equalsIgnoreCase("к"))) {
                source = args[1];
                target = args[3];
                credentials = args[4];
            } else if (args.length >= 4) {
                source = args[1];
                target = args[2];
                credentials = args[3];
            } else {
                lm.sendMessage(sender, "export.usage");
                lm.sendMessage(sender, "export.example-1");
                lm.sendMessage(sender, "export.example-2");
                return true;
            }

            if (!target.equalsIgnoreCase("mysql")) {
                lm.sendMessage(sender, "export.unsupported-target", "target", target);
                return true;
            }

            new HomeExporter(plugin).executeExportAsync(sender, source, credentials);
            return true;
        }

        // Если команду вызывает игрок без аргументов — открываем GUI хомов
        if (sender instanceof Player player) {
            if (args.length > 0) {
                lm.sendMessage(player, "homes.usage");
                return true;
            }
            HomesGUI.open(player, plugin);
            return true;
        }

        // Если консоль вызвала /homes без аргументов
        lm.sendMessage(sender, "homes.only-players");
        lm.sendMessage(sender, "homes.console-usage");
        return true;
    }
}
