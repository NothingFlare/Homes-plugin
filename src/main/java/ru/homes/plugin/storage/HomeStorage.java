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

package ru.homes.plugin.storage;

import ru.homes.plugin.data.Home;

import java.util.Map;
import java.util.UUID;

public interface HomeStorage {

    /**
     * Инициализирует таблицы и пул соединений.
     */
    void init() throws Exception;

    /**
     * Загружает все хомы всех игроков из базы данных.
     */
    Map<UUID, Map<String, Home>> loadAllHomes() throws Exception;

    /**
     * Сохраняет или обновляет хом игрока.
     */
    void saveHome(UUID playerUuid, String playerName, String homeName, Home home) throws Exception;

    /**
     * Удаляет один конкретный хом игрока.
     */
    void deleteHome(UUID playerUuid, String homeName) throws Exception;

    /**
     * Удаляет все хомы указанного игрока.
     * Возвращает количество удаленных записей.
     */
    int deleteAllHomes(UUID playerUuid) throws Exception;

    /**
     * Удаляет все хомы абсолютно всех игроков.
     * Возвращает количество удаленных записей.
     */
    int clearAllHomes() throws Exception;

    /**
     * Ищет UUID игрока по нику в сохранённых записях базы данных.
     */
    UUID findUuidByPlayerName(String playerName) throws Exception;

    /**
     * Закрывает соединение с базой данных и освобождает ресурсы.
     */
    void close();
}
