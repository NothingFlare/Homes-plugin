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
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomesTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            if ("add".startsWith(prefix)) list.add("add");
            return list;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[1].toLowerCase();
            List<String> list = new ArrayList<>();
            if ("homes.yml".startsWith(prefix)) list.add("homes.yml");
            if ("sqlite".startsWith(prefix)) list.add("sqlite");
            if ("legacy".startsWith(prefix)) list.add("legacy");
            return list;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[2].toLowerCase();
            List<String> list = new ArrayList<>();
            if ("to".startsWith(prefix)) list.add("to");
            if ("mysql".startsWith(prefix)) list.add("MySQL");
            return list;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[3].toLowerCase();
            List<String> list = new ArrayList<>();
            if ("mysql".startsWith(prefix)) list.add("MySQL");
            return list;
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("add")) {
            return Collections.singletonList("root:password");
        }

        return Collections.emptyList();
    }
}
