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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.homes.plugin.security.HomeEncryptor;

import java.util.logging.Logger;

public class MySqlStorage extends AbstractSqlStorage {

    public MySqlStorage(String host, int port, String database, String username, String password,
                        String tablePrefix, boolean ssl, int poolSize,
                        HomeEncryptor encryptor, Logger logger) {
        super(createDataSource(host, port, database, username, password, ssl, poolSize),
                encryptor,
                (tablePrefix != null ? tablePrefix : "") + "homes",
                logger);
    }

    private static HikariDataSource createDataSource(String host, int port, String database,
                                                    String username, String password,
                                                    boolean ssl, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("Homes-MySQL");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            config.setDriverClassName("com.mysql.jdbc.Driver");
        }

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=%b&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true",
                host, port, database, ssl
        );
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(Math.max(2, poolSize));
        config.setMinimumIdle(Math.max(1, poolSize / 2));
        config.setMaxLifetime(1800000); // 30 минут
        config.setConnectionTimeout(10000); // 10 секунд
        config.setIdleTimeout(600000); // 10 минут
        config.setConnectionTestQuery("/* ping */ SELECT 1");

        return new HikariDataSource(config);
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(32), " +
                "home_name VARCHAR(64) NOT NULL, " +
                "data TEXT NOT NULL, " +
                "updated_at BIGINT NOT NULL, " +
                "PRIMARY KEY (player_uuid, home_name), " +
                "INDEX idx_homes_player_name (player_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    }

    @Override
    protected String getUpsertQuery() {
        return "INSERT INTO " + tableName + " (player_uuid, player_name, home_name, data, updated_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "player_name = VALUES(player_name), " +
                "data = VALUES(data), " +
                "updated_at = VALUES(updated_at);";
    }
}
