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

package ru.homes.plugin.platform.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import ru.homes.plugin.platform.PlatformAdapter;
import ru.homes.plugin.platform.PlatformType;
import ru.homes.plugin.platform.ServerVersion;
import ru.homes.plugin.platform.TaskHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Адаптер для многопоточного ядра Folia (региональные тики и планировщики).
 */
public class FoliaPlatformAdapter implements PlatformAdapter {

    private final Plugin plugin;
    private final ServerVersion version;

    public FoliaPlatformAdapter(Plugin plugin, ServerVersion version) {
        this.plugin = plugin;
        this.version = version;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FOLIA;
    }

    @Override
    public ServerVersion getServerVersion() {
        return version;
    }

    @Override
    public String getAdapterName() {
        return "Folia-Regional-Adapter";
    }

    @Override
    public TaskHandle runDelayed(Player player, Runnable task, long delayTicks) {
        ScheduledTask scheduledTask = player.getScheduler().runDelayed(plugin, st -> task.run(), null, delayTicks);
        return new TaskHandle() {
            @Override
            public void cancel() {
                if (scheduledTask != null) {
                    scheduledTask.cancel();
                }
            }

            @Override
            public boolean isCancelled() {
                return scheduledTask == null || scheduledTask.isCancelled();
            }
        };
    }

    @Override
    public void runAtEntity(Player player, Runnable task) {
        if (player.isOnline()) {
            player.getScheduler().run(plugin, st -> task.run(), null);
        }
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Player player, Location location) {
        return player.teleportAsync(location);
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, toComponent(title));
    }

    @Override
    public void setItemDisplayName(ItemMeta meta, String displayName) {
        if (meta != null && displayName != null) {
            meta.displayName(toComponent(displayName));
        }
    }

    @Override
    public void setItemLore(ItemMeta meta, List<String> lore) {
        if (meta != null && lore != null) {
            List<Component> components = new ArrayList<>(lore.size());
            for (String line : lore) {
                components.add(toComponent(line));
            }
            meta.lore(components);
        }
    }

    @Override
    public boolean isFolia() {
        return true;
    }

    private Component toComponent(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }
}
