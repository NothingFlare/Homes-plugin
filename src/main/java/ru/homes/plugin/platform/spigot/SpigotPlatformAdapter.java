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

package ru.homes.plugin.platform.spigot;

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

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Адаптер для Spigot и классического CraftBukkit (1.13 - 26.3+).
 * Полностью автономен от Adventure и Paper/Folia API.
 */
public class SpigotPlatformAdapter implements PlatformAdapter {

    private final Plugin plugin;
    private final PlatformType platformType;
    private final ServerVersion version;
    private final Method asyncTeleportMethod;

    public SpigotPlatformAdapter(Plugin plugin, PlatformType platformType, ServerVersion version) {
        this.plugin = plugin;
        this.platformType = platformType;
        this.version = version;
        this.asyncTeleportMethod = findAsyncTeleportMethod();
    }

    private Method findAsyncTeleportMethod() {
        try {
            return Player.class.getMethod("teleportAsync", Location.class);
        } catch (Throwable t) {
            return null;
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
        String base = (platformType == PlatformType.SPIGOT) ? "Spigot" : "Bukkit";
        return base + "-Standard-Adapter";
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

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Boolean> teleportAsync(Player player, Location location) {
        if (asyncTeleportMethod != null) {
            try {
                return (CompletableFuture<Boolean>) asyncTeleportMethod.invoke(player, location);
            } catch (Throwable ignored) {}
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (Bukkit.isPrimaryThread()) {
            try {
                future.complete(player.teleport(location));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    future.complete(player.teleport(location));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
    }

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, title);
    }

    @Override
    public void setItemDisplayName(ItemMeta meta, String displayName) {
        if (meta != null && displayName != null) {
            meta.setDisplayName(displayName);
        }
    }

    @Override
    public void setItemLore(ItemMeta meta, List<String> lore) {
        if (meta != null && lore != null) {
            meta.setLore(lore);
        }
    }

    @Override
    public boolean isFolia() {
        return false;
    }
}
