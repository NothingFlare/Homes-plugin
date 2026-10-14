package ru.homes.plugin.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public class HomeTeleportListener implements Listener {
    private final HomeTeleportManager teleportManager;

    public HomeTeleportListener(HomeTeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Быстрый выход: если вообще никто не ждёт телепортации на сервере, ничего не вычисляем
        if (!teleportManager.hasPending()) return;

        Player player = event.getPlayer();
        if (!teleportManager.hasPending(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Проверяем перемещение между блоками (вращение головой не сбивает телепорт)
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || !Objects.equals(from.getWorld(), to.getWorld())) {
            teleportManager.cancel(player, "§cТелепортация отменена: вы двинулись.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!teleportManager.hasPending()) return;

        if (event.getEntity() instanceof Player player) {
            if (teleportManager.hasPending(player.getUniqueId())) {
                teleportManager.cancel(player, "§cТелепортация отменена: вы получили урон.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        teleportManager.cancel(event.getEntity(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        teleportManager.cancel(event.getPlayer(), null);
    }
}
