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

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.language.LanguageManager;
import ru.homes.plugin.teleport.HomeTeleportManager;

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
        LanguageManager lm = plugin.getLanguageManager();

        // Команда очистки хомов: ТОЛЬКО ИЗ КОНСОЛИ!
        if (args.length >= 1 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("очистить"))) {
            if (!(sender instanceof ConsoleCommandSender)) {
                lm.sendMessage(sender, "clear.console-only");
                return true;
            }

            if (args.length != 2) {
                lm.sendMessage(sender, "clear.usage");
                return true;
            }

            String target = args[1];
            if (target.equalsIgnoreCase("all") || target.equalsIgnoreCase("все") || target.equalsIgnoreCase("всё")) {
                int totalDeleted = homeManager.clearAllHomes();
                lm.sendMessage(sender, "clear.all-success", "count", totalDeleted);
                return true;
            }

            int deleted = homeManager.clearPlayerHomes(target);
            if (deleted > 0) {
                lm.sendMessage(sender, "clear.player-success", "player", target, "count", deleted);
            } else {
                lm.sendMessage(sender, "clear.player-not-found", "player", target);
            }
            return true;
        }

        // Перезагрузка конфигурации и хранилища данных на лету
        if (args.length == 1 && (args[0].equalsIgnoreCase("reload")
                || args[0].equalsIgnoreCase("релоад")
                || args[0].equalsIgnoreCase("перезагрузить"))) {
            if (!sender.hasPermission("homes.reload")) {
                lm.sendMessage(sender, "command.no-permission");
                return true;
            }

            boolean success = plugin.reloadPlugin();
            if (success) {
                lm.sendMessage(sender, "reload.success");
            } else {
                lm.sendMessage(sender, "reload.error");
            }
            return true;
        }

        // Обычная телепортация — только для игроков
        if (!(sender instanceof Player player)) {
            lm.sendMessage(sender, "home.only-players");
            return true;
        }

        if (args.length != 1) {
            String usageKey = player.hasPermission("homes.reload") ? "home.usage-admin" : "home.usage";
            lm.sendMessage(player, usageKey);
            return true;
        }

        String name = args[0];
        Home home = homeManager.getHome(player.getUniqueId(), name);
        if (home == null) {
            lm.sendMessage(player, "home.not-found", "name", name);
            return true;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            lm.sendMessage(player, "home.world-not-loaded", "name", name, "world", home.getWorld());
            return true;
        }

        int timeout = plugin.getConfig().getInt("teleport-timeout", 5);
        teleportManager.teleport(player, loc, name, timeout);
        return true;
    }
}
