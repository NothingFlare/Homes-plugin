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
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.data.HomeManager;
import ru.homes.plugin.language.LanguageManager;

import java.util.regex.Pattern;

public class SetHomeCommand implements CommandExecutor {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9а-яА-ЯёЁ_-]{1,32}$");

    private final HomesPlugin plugin;
    private final HomeManager homeManager;

    public SetHomeCommand(HomesPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lm = plugin.getLanguageManager();

        if (!(sender instanceof Player player)) {
            lm.sendMessage(sender, "command.only-players");
            return true;
        }

        if (args.length != 1) {
            lm.sendMessage(player, "sethome.usage");
            return true;
        }

        String name = args[0];

        if (name.equalsIgnoreCase("reload")
                || name.equalsIgnoreCase("релоад")
                || name.equalsIgnoreCase("clear")
                || name.equalsIgnoreCase("очистить")
                || name.equalsIgnoreCase("перезагрузить")) {
            lm.sendMessage(player, "sethome.name-reserved", "name", name.toLowerCase());
            return true;
        }

        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            lm.sendMessage(player, "sethome.invalid-name");
            return true;
        }

        boolean alreadyExists = homeManager.getHome(player.getUniqueId(), name) != null;
        int currentCount = homeManager.getHomes(player.getUniqueId()).size();
        int homeLimit = plugin.getConfig().getInt("home-limit", 10);

        if (!alreadyExists && currentCount >= homeLimit && !player.hasPermission("homes.bypass")) {
            lm.sendMessage(player, "sethome.limit-reached", "count", currentCount, "limit", homeLimit);
            return true;
        }

        boolean overwritten = homeManager.setHome(player.getUniqueId(), player.getName(), name, player.getLocation());
        if (overwritten) {
            lm.sendMessage(player, "sethome.overwritten", "name", name);
        } else {
            lm.sendMessage(player, "sethome.created", "name", name);
        }
        return true;
    }
}
