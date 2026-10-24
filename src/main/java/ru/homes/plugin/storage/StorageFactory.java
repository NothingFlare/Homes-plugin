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
import ru.homes.plugin.language.LanguageManager;
import ru.homes.plugin.security.HomeEncryptor;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageFactory {

    public static HomeStorage createStorage(HomesPlugin plugin, HomeEncryptor encryptor) throws Exception {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();
        LanguageManager lm = plugin != null ? plugin.getLanguageManager() : null;
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

            String logMsg = lm != null
                    ? lm.get("console.storage-init-mysql", "host", host, "port", port, "database", database)
                    : "[Homes] Connecting to MySQL storage (" + host + ":" + port + "/" + database + ")...";
            logger.info(logMsg);
            storage = new MySqlStorage(host, port, database, username, password, prefix, ssl, poolSize, encryptor, logger);
        } else if ("legacy".equals(type) || "yaml".equals(type) || "yml".equals(type)) {
            String fileName = config.getString("storage.legacy.file", "homes.yml");
            File yamlFile = new File(plugin.getDataFolder(), fileName);
            String logMsg = lm != null
                    ? lm.get("console.storage-init-yaml", "file", yamlFile.getName())
                    : "[Homes] Initializing Legacy YAML storage (" + yamlFile.getName() + ")...";
            logger.info(logMsg);
            storage = new YamlStorage(yamlFile, encryptor, logger);
        } else {
            String fileName = config.getString("storage.sqlite.file", "homes.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            String logMsg = lm != null
                    ? lm.get("console.storage-init-sqlite", "file", dbFile.getName())
                    : "[Homes] Initializing SQLite local storage (" + dbFile.getName() + ")...";
            logger.info(logMsg);
            storage = new SqliteStorage(dbFile, encryptor, logger);
        }

        try {
            storage.init();
            String successMsg = lm != null
                    ? lm.get("console.storage-success", "type", type.toUpperCase())
                    : "[Homes] Storage " + type.toUpperCase() + " successfully initialized.";
            logger.info(successMsg);
        } catch (Exception e) {
            String errorMsg = lm != null
                    ? lm.get("console.storage-error", "type", type.toUpperCase())
                    : "[Homes] Critical error initializing storage " + type + "!";
            logger.log(Level.SEVERE, errorMsg, e);
            throw e;
        }

        return storage;
    }
}
