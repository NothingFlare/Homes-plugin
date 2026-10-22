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

import com.zaxxer.hikari.HikariDataSource;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSqlStorage implements HomeStorage {

    protected final HikariDataSource dataSource;
    protected final HomeEncryptor encryptor;
    protected final String tableName;
    protected final Logger logger;

    public AbstractSqlStorage(HikariDataSource dataSource, HomeEncryptor encryptor, String tableName, Logger logger) {
        this.dataSource = dataSource;
        this.encryptor = encryptor;
        this.tableName = tableName;
        this.logger = logger != null ? logger : Logger.getLogger(getClass().getName());
    }

    protected abstract String getCreateTableQuery();

    protected abstract String getUpsertQuery();

    @Override
    public void init() throws Exception {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(getCreateTableQuery())) {
            ps.execute();
        }
    }

    @Override
    public Map<UUID, Map<String, Home>> loadAllHomes() throws Exception {
        Map<UUID, Map<String, Home>> result = new LinkedHashMap<>();
        String sql = "SELECT player_uuid, home_name, data FROM " + tableName;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String uuidStr = rs.getString("player_uuid");
                String homeName = rs.getString("home_name");
                String rawData = rs.getString("data");

                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("[Homes] Пропуск некорректного UUID в базе: " + uuidStr);
                    continue;
                }

                try {
                    Home home = encryptor.decrypt(uuid, rawData);
                    if (home != null) {
                        result.computeIfAbsent(uuid, k -> new LinkedHashMap<>()).put(homeName, home);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[Homes] Не удалось расшифровать хом '" + homeName + "' игрока " + uuidStr, e);
                }
            }
        }

        return result;
    }

    @Override
    public void saveHome(UUID playerUuid, String playerName, String homeName, Home home) throws Exception {
        String encryptedData = encryptor.encrypt(playerUuid, home);
        long now = System.currentTimeMillis();

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(getUpsertQuery())) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, homeName);
            ps.setString(4, encryptedData);
            ps.setLong(5, now);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteHome(UUID playerUuid, String homeName) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE player_uuid = ? AND home_name = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, homeName);
            ps.executeUpdate();
        }
    }

    @Override
    public int deleteAllHomes(UUID playerUuid) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE player_uuid = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            return ps.executeUpdate();
        }
    }

    @Override
    public int clearAllHomes() throws Exception {
        String sql = "DELETE FROM " + tableName;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    @Override
    public UUID findUuidByPlayerName(String playerName) throws Exception {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT player_uuid FROM " + tableName + " WHERE LOWER(player_name) = LOWER(?) LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, playerName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("player_uuid"));
                }
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
