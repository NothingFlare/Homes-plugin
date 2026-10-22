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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class SqliteStorageTest {

    @TempDir
    Path tempDir;

    private SqliteStorage storage;
    private File dbFile;
    private HomeEncryptor encryptor;

    @BeforeEach
    public void setup() throws Exception {
        dbFile = tempDir.resolve("test-homes.db").toFile();
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 5);
        encryptor = new HomeEncryptor(true, key);

        storage = new SqliteStorage(dbFile, encryptor, Logger.getLogger("TestLogger"));
        storage.init();
    }

    @AfterEach
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    public void testSaveAndLoadEncryptedHomes() throws Exception {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        Home home1 = new Home("world", 100.5, 64.0, -200.5, 45.0f, 10.0f);
        Home home2 = new Home("world_nether", 50.0, 70.0, 50.0, 90.0f, 0.0f);

        storage.saveHome(player1, "PlayerOne", "main", home1);
        storage.saveHome(player2, "PlayerTwo", "base", home2);

        // Проверяем прямое содержимое таблицы SQLite (то, что видит администратор БД)
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT player_uuid, player_name, home_name, data FROM homes WHERE player_uuid = '" + player1 + "'")) {

            assertTrue(rs.next(), "Запись хома должна существовать в БД");
            String rawData = rs.getString("data");
            assertTrue(rawData.startsWith("ENC:v1:"), "Данные хома в базе данных ОБЯЗАНЫ быть зашифрованы!");
            // Координаты и мир не должны быть видны админу
            assertFalse(rawData.contains("world"));
            assertFalse(rawData.contains("100.5"));
            assertFalse(rawData.contains("-200.5"));
        }

        // Проверяем загрузку и расшифровку плагином
        Map<UUID, Map<String, Home>> allHomes = storage.loadAllHomes();
        assertEquals(2, allHomes.size());

        Home loadedHome1 = allHomes.get(player1).get("main");
        assertNotNull(loadedHome1);
        assertEquals("world", loadedHome1.getWorld());
        assertEquals(100.5, loadedHome1.getX(), 0.001);

        Home loadedHome2 = allHomes.get(player2).get("base");
        assertNotNull(loadedHome2);
        assertEquals("world_nether", loadedHome2.getWorld());
    }

    @Test
    public void testFindUuidByPlayerName() throws Exception {
        UUID uuid = UUID.randomUUID();
        Home home = new Home("world", 0.0, 60.0, 0.0, 0.0f, 0.0f);
        storage.saveHome(uuid, "AlexTheBest", "home1", home);

        UUID found = storage.findUuidByPlayerName("alexthebest");
        assertEquals(uuid, found, "Поиск по нику должен быть регистронезависимым");

        assertNull(storage.findUuidByPlayerName("NonExistentPlayer"));
    }

    @Test
    public void testDeleteHome() throws Exception {
        UUID uuid = UUID.randomUUID();
        Home home = new Home("world", 0, 0, 0, 0, 0);
        storage.saveHome(uuid, "Player", "h1", home);
        storage.saveHome(uuid, "Player", "h2", home);

        storage.deleteHome(uuid, "h1");

        Map<UUID, Map<String, Home>> loaded = storage.loadAllHomes();
        assertFalse(loaded.get(uuid).containsKey("h1"));
        assertTrue(loaded.get(uuid).containsKey("h2"));
    }

    @Test
    public void testDeleteAllHomesForPlayer() throws Exception {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        Home home = new Home("world", 0, 0, 0, 0, 0);

        storage.saveHome(p1, "P1", "h1", home);
        storage.saveHome(p1, "P1", "h2", home);
        storage.saveHome(p2, "P2", "h1", home);

        int deleted = storage.deleteAllHomes(p1);
        assertEquals(2, deleted);

        Map<UUID, Map<String, Home>> loaded = storage.loadAllHomes();
        assertNull(loaded.get(p1));
        assertNotNull(loaded.get(p2));
    }

    @Test
    public void testClearAllHomes() throws Exception {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        Home home = new Home("world", 0, 0, 0, 0, 0);

        storage.saveHome(p1, "P1", "h1", home);
        storage.saveHome(p2, "P2", "h1", home);

        int deleted = storage.clearAllHomes();
        assertEquals(2, deleted);

        Map<UUID, Map<String, Home>> loaded = storage.loadAllHomes();
        assertTrue(loaded.isEmpty());
    }
}
