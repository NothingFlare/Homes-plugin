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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class HomeStorageSwitchTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSwitchStorageOnTheFly() throws Exception {
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 1);
        HomeEncryptor encryptor = new HomeEncryptor(true, key);

        File db1 = tempDir.resolve("storage1.db").toFile();
        File db2 = tempDir.resolve("storage2.db").toFile();

        SqliteStorage storage1 = new SqliteStorage(db1, encryptor, Logger.getLogger("TestSwitch1"));
        storage1.init();

        UUID p1 = UUID.randomUUID();
        storage1.saveHome(p1, "PlayerOne", "home_s1", new Home("world", 10, 64, 10, 0, 0));

        assertEquals(1, storage1.loadAllHomes().size());

        // Создаем второе хранилище (имитация смены БД в config.yml)
        SqliteStorage storage2 = new SqliteStorage(db2, encryptor, Logger.getLogger("TestSwitch2"));
        storage2.init();

        UUID p2 = UUID.randomUUID();
        storage2.saveHome(p2, "PlayerTwo", "home_s2", new Home("world_nether", 50, 70, 50, 0, 0));

        // Закрываем первое хранилище
        storage1.close();

        // Проверяем работу второго хранилища
        var loaded2 = storage2.loadAllHomes();
        assertEquals(1, loaded2.size());
        assertNotNull(loaded2.get(p2).get("home_s2"));
        assertNull(loaded2.get(p1));

        storage2.close();
    }
}
