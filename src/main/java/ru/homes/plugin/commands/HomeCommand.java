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
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.teleport.HomeTeleportManager;
import ru.homes.plugin.update.UpdateChecker;

public class HomeCommand implements CommandExecutor {

    private final HomeManager homeManager;
    private final HomeTeleportManager teleportManager;
    private final HomesPlugin plugin;

    public HomeCommand(HomeManager homeManager, HomeTeleportManager teleportManager) {
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
        this.plugin = homeManager.getPlugin();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Команда очистки хомов: ТОЛЬКО ИЗ КОНСОЛИ!
        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.RED + "Команду /home clear можно выполнять только из консоли!");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.YELLOW + "Использование: /home clear <ник|all>");
                return true;
            }

            String target = args[1];
            if (target.equalsIgnoreCase("all")) {
                int totalDeleted = homeManager.clearAllHomes();
                sender.sendMessage(ChatColor.GREEN + "Все хомы всех игроков успешно удалены (всего удалено: " + totalDeleted + ").");
                return true;
            }

            int deleted = homeManager.clearPlayerHomes(target);
            if (deleted > 0) {
                sender.sendMessage(ChatColor.GREEN + "Все хомы игрока " + target + " успешно удалены (удалено: " + deleted + ").");
            } else {
                sender.sendMessage(ChatColor.RED + "У игрока " + target + " не найдено ни одного хома.");
            }
            return true;
        }

        // Перезагрузка конфигурации
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("homes.reload")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав.");
                return true;
            }
            plugin.reloadConfig();
            int timeout = plugin.getConfig().getInt("teleport-timeout", 5);
            int limit = plugin.getConfig().getInt("home-limit", 10);
            sender.sendMessage(ChatColor.GREEN + "Homes config reloaded successfully!");
            plugin.getLogger().info("Configuration reloaded by " + sender.getName()
                    + " (teleport-timeout=" + timeout + ", home-limit=" + limit + ")");
            if (plugin.getConfig().getBoolean("check-updates", true)) {
                new UpdateChecker(plugin.getDescription().getVersion(), plugin.getLogger()).checkAsync();
            }
            return true;
        }

        // Обычная телепортация — только для игроков
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Только для игроков. Консоль может использовать: /home clear <ник|all> или /home reload");
            return true;
        }

        if (args.length != 1) {
            String usage = player.hasPermission("homes.reload")
                    ? "Использование: /home <название> | /home reload"
                    : "Использование: /home <название>";
            player.sendMessage(ChatColor.YELLOW + usage);
            return true;
        }

        String name = args[0];
        Home home = homeManager.getHome(player.getUniqueId(), name);
        if (home == null) {
            player.sendMessage(ChatColor.RED + "Хом " + name + " не найден.");
            return true;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Мир хома " + name + " (" + home.getWorld() + ") не загружен.");
            return true;
        }

        int timeout = plugin.getConfig().getInt("teleport-timeout", 5);
        teleportManager.teleport(player, loc, name, timeout);
        return true;
    }
}
