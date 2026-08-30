package ru.homes.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/**
 * Хранит порядок названий хомов по слотам конкретного открытого GUI,
 * чтобы при клике определить, к какому хому телепортировать.
 */
public class HomesInventoryHolder implements InventoryHolder {

    private final List<String> slotToHomeName;
    private Inventory inventory;

    public HomesInventoryHolder(List<String> slotToHomeName) {
        this.slotToHomeName = slotToHomeName;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getHomeNameAt(int slot) {
        if (slot < 0 || slot >= slotToHomeName.size()) return null;
        return slotToHomeName.get(slot);
    }
}
