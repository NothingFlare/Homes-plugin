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

package ru.homes.plugin.platform.paper;

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
import org.bukkit.scheduler.BukkitTask;
import ru.homes.plugin.platform.PlatformAdapter;
import ru.homes.plugin.platform.PlatformType;
import ru.homes.plugin.platform.ServerVersion;
import ru.homes.plugin.platform.TaskHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Адаптер для Paper и Purpur (1.13 - 26.3+).
 * Поддерживает Paper teleportAsync и Adventure API (с 1.16.5+).
 */
public class PaperPlatformAdapter implements PlatformAdapter {

    private final Plugin plugin;
    private final PlatformType platformType;
    private final ServerVersion version;
    private final boolean hasAdventure;

    public PaperPlatformAdapter(Plugin plugin, PlatformType platformType, ServerVersion version) {
        this.plugin = plugin;
        this.platformType = platformType;
        this.version = version;
        this.hasAdventure = checkAdventure();
    }

    private boolean checkAdventure() {
        try {
            Class.forName("net.kyori.adventure.text.Component");
            ItemMeta.class.getMethod("displayName", Component.class);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public PlatformType getPlatformType() {
        return platformType;
    }

    @Override
    public ServerVersion getServerVersion() {
        return version;
    }

    @Override
    public String getAdapterName() {
        String base = (platformType == PlatformType.PURPUR) ? "Purpur" : "Paper";
        return base + (hasAdventure ? "-Adventure" : "-Legacy") + "-Adapter";
    }

    @Override
    public TaskHandle runDelayed(Player player, Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return new TaskHandle() {
            @Override
            public void cancel() {
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                }
            }

            @Override
            public boolean isCancelled() {
                return bukkitTask == null || bukkitTask.isCancelled();
            }
        };
    }

    @Override
    public void runAtEntity(Player player, Runnable task) {
        if (player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Player player, Location location) {
        return player.teleportAsync(location);
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        if (hasAdventure) {
            return Bukkit.createInventory(holder, size, toComponent(title));
        }
        return Bukkit.createInventory(holder, size, title);
    }

    @Override
    public void setItemDisplayName(ItemMeta meta, String displayName) {
        if (meta == null || displayName == null) return;
        if (hasAdventure) {
            meta.displayName(toComponent(displayName));
        } else {
            meta.setDisplayName(displayName);
        }
    }

    @Override
    public void setItemLore(ItemMeta meta, List<String> lore) {
        if (meta == null || lore == null) return;
        if (hasAdventure) {
            List<Component> components = new ArrayList<>(lore.size());
            for (String line : lore) {
                components.add(toComponent(line));
            }
            meta.lore(components);
        } else {
            meta.setLore(lore);
        }
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    private Component toComponent(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }
}
