package ru.homes.plugin.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ru.homes.plugin.HomesPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HomeTeleportManager {
    private final HomesPlugin plugin;
    private final Map<UUID, ScheduledTask> pending = new ConcurrentHashMap<>();

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
        player.sendMessage("§eНе двигайтесь! Телепортация через " + timeoutSeconds + " сек.");

        ScheduledTask task = player.getScheduler().runDelayed(plugin, scheduledTask -> {
            ScheduledTask existing = pending.remove(player.getUniqueId());
            if (existing == null || !player.isOnline()) return;

            if (hasMoved(start, player.getLocation())) {
                player.sendMessage("§cТелепортация отменена: вы двигались.");
                return;
            }

            if (location.getWorld() == null) {
                player.sendMessage("§cТелепортация не удалась: целевой мир не загружен.");
                return;
            }

            executeTeleport(player, location, homeName);
        }, null, timeoutSeconds * 20L);

        if (task != null) {
            pending.put(player.getUniqueId(), task);
        }
    }

    private void executeTeleport(Player player, Location location, String homeName) {
        player.teleportAsync(location).whenComplete((success, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.WARNING, "Ошибка телепортации игрока " + player.getName(), throwable);
            }
            player.getScheduler().run(plugin, messageTask -> {
                if (!player.isOnline()) return;
                if (throwable != null || !Boolean.TRUE.equals(success)) {
                    player.sendMessage("§cТелепортация не удалась.");
                } else {
                    player.sendMessage("§aТелепортация к " + homeName + ".");
                }
            }, null);
        });
    }

    public void cancel(Player player, String reason) {
        if (player == null) return;
        ScheduledTask task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (player.isOnline() && reason != null) {
                player.sendMessage(reason);
            }
        }
    }

    public void cancelAll() {
        for (ScheduledTask task : pending.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pending.clear();
    }

    private boolean hasMoved(Location a, Location b) {
        if (a == null || b == null) return true;
        if (!Objects.equals(a.getWorld(), b.getWorld())) return true;
        return a.getBlockX() != b.getBlockX()
                || a.getBlockY() != b.getBlockY()
                || a.getBlockZ() != b.getBlockZ();
    }
}
