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

import org.bukkit.configuration.file.FileConfiguration;
import ru.homes.plugin.HomesPlugin;
import ru.homes.plugin.security.HomeEncryptor;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageFactory {

    public static HomeStorage createStorage(HomesPlugin plugin, HomeEncryptor encryptor) throws Exception {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();
        String type = config.getString("storage.type", "sqlite").trim().toLowerCase();

        HomeStorage storage;
        if ("mysql".equals(type)) {
            String host = config.getString("storage.mysql.host", "localhost");
            int port = config.getInt("storage.mysql.port", 3306);
            String database = config.getString("storage.mysql.database", "homes");
            String username = config.getString("storage.mysql.username", "root");
            String password = config.getString("storage.mysql.password", "");
            String prefix = config.getString("storage.mysql.table-prefix", "homes_");
            boolean ssl = config.getBoolean("storage.mysql.ssl", false);
            int poolSize = config.getInt("storage.mysql.pool-size", 10);

            logger.info("[Homes] Подключение к хранилищу MySQL (" + host + ":" + port + "/" + database + ")...");
            storage = new MySqlStorage(host, port, database, username, password, prefix, ssl, poolSize, encryptor, logger);
        } else {
            String fileName = config.getString("storage.sqlite.file", "homes.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            logger.info("[Homes] Инициализация локального хранилища SQLite (" + dbFile.getName() + ")...");
            storage = new SqliteStorage(dbFile, encryptor, logger);
        }

        try {
            storage.init();
            logger.info("[Homes] Хранилище " + type.toUpperCase() + " успешно инициализировано.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Homes] Критическая ошибка при инициализации хранилища " + type + "!", e);
            throw e;
        }

        return storage;
    }
}
