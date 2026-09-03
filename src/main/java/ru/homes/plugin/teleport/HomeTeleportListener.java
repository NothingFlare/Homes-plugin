package ru.homes.plugin.teleport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;

public class HomeTeleportListener implements Listener {
    private final HomeTeleportManager teleportManager;

    public HomeTeleportListener(HomeTeleportManager teleportManager) {
        this.teleportManager = teleportManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) return;
        Player player = event.getPlayer();
        teleportManager.cancel(player, true);
    }
}
