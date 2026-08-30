package ru.homes.plugin.gui;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.data.HomeManager;

public class HomesGUIListener implements Listener {

    private final HomeManager homeManager;

    public HomesGUIListener(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomesInventoryHolder holder)) {
            return;
        }

        // блокируем любое перемещение предметов из этого GUI
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        String homeName = holder.getHomeNameAt(event.getSlot());
        if (homeName == null) return;

        Home home = homeManager.getHome(player.getUniqueId(), homeName);
        if (home == null) {
            player.sendMessage(ChatColor.RED + "Хом \"" + homeName + "\" больше не существует.");
            return;
        }

        Location loc = home.toLocation();
        if (loc == null) {
            player.sendMessage(ChatColor.RED + "Мир хома \"" + homeName + "\" (" + home.getWorld() + ") не загружен.");
            return;
        }

        player.closeInventory();
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Телепортация к \"" + homeName + "\".");
    }
}
