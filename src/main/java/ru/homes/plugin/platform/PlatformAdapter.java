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

package ru.homes.plugin.platform;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Модульный адаптер платформы.
 * Предоставляет платформонезависимый интерфейс для Folia, Paper, Purpur, Spigot и Bukkit.
 */
public interface PlatformAdapter {

    PlatformType getPlatformType();

    ServerVersion getServerVersion();

    String getAdapterName();

    /**
     * Планирует отложенную задачу для конкретного игрока с учётом региональности (Folia)
     * или глобального шедулера (Spigot/Paper).
     */
    TaskHandle runDelayed(Player player, Runnable task, long delayTicks);

    /**
     * Выполняет задачу в контексте сущности/основного потока.
     */
    void runAtEntity(Player player, Runnable task);

    /**
     * Асинхронно или безопасно телепортирует игрока к указанной локации.
     */
    CompletableFuture<Boolean> teleportAsync(Player player, Location location);

    /**
     * Создаёт инвентарь с заголовком (Adventure Component на Paper/Folia или String на Spigot/Bukkit).
     */
    Inventory createInventory(InventoryHolder holder, int size, String title);

    /**
     * Устанавливает название предмета.
     */
    void setItemDisplayName(ItemMeta meta, String displayName);

    /**
     * Устанавливает описание (lore) предмета.
     */
    void setItemLore(ItemMeta meta, List<String> lore);

    /**
     * Указывает, работает ли сервер под управлением Folia (региональная многопоточность).
     */
    boolean isFolia();
}
