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

package ru.homes.plugin.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.platform.PlatformAdapter;
import ru.homes.plugin.platform.TaskHandle;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HomeTeleportManager {
    private final HomesPlugin plugin;
    private final Map<UUID, TaskHandle> pending = new ConcurrentHashMap<>();

    public HomeTeleportManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void teleport(Player player, Location location, String homeName, int timeoutSeconds) {
        if (player == null || !player.isOnline()) return;

        // Отменяем любой предыдущий ожидающий телепорт этого игрока без сообщения
        cancel(player, null);

        if (timeoutSeconds <= 0) {
            executeTeleport(player, location, homeName);
            return;
        }

        final Location start = player.getLocation().clone();
        plugin.getLanguageManager().sendMessage(player, "teleport.warmup", "seconds", timeoutSeconds);

        PlatformAdapter adapter = plugin.getPlatformManager().getAdapter();
        TaskHandle task = adapter.runDelayed(player, () -> {
            TaskHandle existing = pending.remove(player.getUniqueId());
            if (existing == null || !player.isOnline()) return;

            if (hasMoved(start, player.getLocation())) {
                plugin.getLanguageManager().sendMessage(player, "teleport.cancelled-moved");
                return;
            }

            if (location.getWorld() == null) {
                plugin.getLanguageManager().sendMessage(player, "teleport.world-not-loaded");
                return;
            }

            executeTeleport(player, location, homeName);
        }, timeoutSeconds * 20L);

        if (task != null) {
            pending.put(player.getUniqueId(), task);
        }
    }

    private void executeTeleport(Player player, Location location, String homeName) {
        PlatformAdapter adapter = plugin.getPlatformManager().getAdapter();
        adapter.teleportAsync(player, location).whenComplete((success, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.WARNING, "Ошибка телепортации игрока " + player.getName(), throwable);
            }
            adapter.runAtEntity(player, () -> {
                if (!player.isOnline()) return;
                if (throwable != null || !Boolean.TRUE.equals(success)) {
                    plugin.getLanguageManager().sendMessage(player, "teleport.failed");
                } else {
                    plugin.getLanguageManager().sendMessage(player, "teleport.success", "name", homeName);
                }
            });
        });
    }

    public void cancel(Player player, String reason) {
        if (player == null) return;
        TaskHandle task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (player.isOnline() && reason != null) {
                player.sendMessage(reason);
            }
        }
    }

    public void cancelAll() {
        for (TaskHandle task : pending.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pending.clear();
    }

    public HomesPlugin getPlugin() {
        return plugin;
    }

    private boolean hasMoved(Location a, Location b) {
        if (a == null || b == null) return true;
        if (!Objects.equals(a.getWorld(), b.getWorld())) return true;
        return a.getBlockX() != b.getBlockX()
                || a.getBlockY() != b.getBlockY()
                || a.getBlockZ() != b.getBlockZ();
    }
}
