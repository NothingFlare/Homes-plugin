package ru.homes.plugin.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ru.homes.plugin.HomesPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeTeleportManager {
    private final HomesPlugin plugin;
    private final Map<UUID, ScheduledTask> pending = new HashMap<>();

    public HomeTeleportManager(HomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Location location, String homeName, int timeoutSeconds) {
        cancel(player, false);

        if (timeoutSeconds <= 0) {
            player.teleportAsync(location).whenComplete((success, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Ошибка телепортации игрока " + player.getName() + ": " + throwable.getMessage());
                }
                player.getScheduler().run(plugin, messageTask -> {
                    if (!player.isOnline()) return;
                    if (throwable != null || !Boolean.TRUE.equals(success)) {
                        player.sendMessage("§cТелепортация не удалась.");
                    } else {
                        player.sendMessage("§aТелепортация к «" + homeName + "».");
                    }
                }, null);
            });
            return;
        }

        final Location start = player.getLocation().clone();
        player.sendMessage("§eНе двигайтесь! Телепортация через " + timeoutSeconds + " сек.");

        ScheduledTask task = player.getScheduler().runDelayed(plugin, scheduledTask -> {
            pending.remove(player.getUniqueId());
            if (!player.isOnline()) return;

            if (hasMoved(start, player.getLocation())) {
                player.sendMessage("§cТелепортация отменена: вы двигались.");
                return;
            }

            player.teleportAsync(location).whenComplete((success, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Ошибка телепортации игрока " + player.getName() + ": " + throwable.getMessage());
                }
                player.getScheduler().run(plugin, messageTask -> {
                    if (!player.isOnline()) return;
                    if (throwable != null || !Boolean.TRUE.equals(success)) {
                        player.sendMessage("§cТелепортация не удалась.");
                    } else {
                        player.sendMessage("§aТелепортация к «" + homeName + "».");
                    }
                }, null);
            });
        }, null, timeoutSeconds * 20L);

        pending.put(player.getUniqueId(), task);
    }

    public void cancel(Player player, boolean notify) {
        ScheduledTask task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (notify) player.sendMessage("§cТелепортация отменена: вы двинулись.");
        }
    }

    private boolean hasMoved(Location a, Location b) {
        if (a.getWorld() != b.getWorld()) return true;
        return a.getX() != b.getX() || a.getY() != b.getY() || a.getZ() != b.getZ();
    }
}
