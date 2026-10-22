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

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

public class SqliteStorage extends AbstractSqlStorage {

    public SqliteStorage(File dbFile, HomeEncryptor encryptor, Logger logger) {
        super(createDataSource(dbFile), encryptor, "homes", logger);
    }

    private static HikariDataSource createDataSource(File dbFile) {
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}

        HikariConfig config = new HikariConfig();
        config.setPoolName("Homes-SQLite");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setConnectionTestQuery("SELECT 1");
        // SQLite является файловой БД — пул из 1 соединения исключает проблемы блокировки файла
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(0);

        return new HikariDataSource(config);
    }

    @Override
    public void init() throws Exception {
        // Оптимизация производительности SQLite (WAL режим)
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            st.execute("PRAGMA journal_mode = WAL;");
            st.execute("PRAGMA synchronous = NORMAL;");
        }

        super.init();

        // Индекс по нику для быстрой очистки через консоль /home clear <ник>
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE INDEX IF NOT EXISTS idx_" + tableName + "_player_name ON " + tableName + " (player_name);");
        }
    }

    @Override
    protected String getCreateTableQuery() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(32), " +
                "home_name VARCHAR(64) NOT NULL, " +
                "data TEXT NOT NULL, " +
                "updated_at BIGINT NOT NULL, " +
                "PRIMARY KEY (player_uuid, home_name)" +
                ");";
    }

    @Override
    protected String getUpsertQuery() {
        return "INSERT INTO " + tableName + " (player_uuid, player_name, home_name, data, updated_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, home_name) DO UPDATE SET " +
                "player_name = excluded.player_name, " +
                "data = excluded.data, " +
                "updated_at = excluded.updated_at;";
    }
}
