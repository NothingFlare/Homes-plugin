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

public class YamlStorageTest {

    @TempDir
    Path tempDir;

    @Test
    public void testYamlStorageClassicAndEncrypted() throws Exception {
        File file = tempDir.resolve("homes.yml").toFile();
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 3);

        // 1. Тест с шифрованием
        HomeEncryptor encryptor = new HomeEncryptor(true, key);
        YamlStorage storage = new YamlStorage(file, encryptor, Logger.getLogger("YamlTest"));
        storage.init();

        UUID p1 = UUID.randomUUID();
        storage.saveHome(p1, "PlayerOne", "main", new Home("world", 100.5, 64.0, -200.5, 45.0f, 0.0f));

        var loaded = storage.loadAllHomes();
        assertEquals(1, loaded.size());
        Home home = loaded.get(p1).get("main");
        assertNotNull(home);
        assertEquals("world", home.getWorld());
        assertEquals(100.5, home.getX(), 0.001);

        // Поиск по нику
        UUID found = storage.findUuidByPlayerName("playerone");
        assertEquals(p1, found);

        // Удаление
        storage.deleteHome(p1, "main");
        assertTrue(storage.loadAllHomes().isEmpty());
    }

    @Test
    public void testYamlStorageUnencrypted() throws Exception {
        File file = tempDir.resolve("homes_plain.yml").toFile();
        HomeEncryptor encryptor = new HomeEncryptor(false, new byte[32]);
        YamlStorage storage = new YamlStorage(file, encryptor, Logger.getLogger("YamlPlainTest"));
        storage.init();

        UUID p1 = UUID.randomUUID();
        storage.saveHome(p1, "PlayerPlain", "base", new Home("world_nether", 50, 70, 50, 0, 0));

        var loaded = storage.loadAllHomes();
        assertEquals(1, loaded.size());
        Home home = loaded.get(p1).get("base");
        assertNotNull(home);
        assertEquals("world_nether", home.getWorld());

        int deleted = storage.deleteAllHomes(p1);
        assertEquals(1, deleted);
        assertTrue(storage.loadAllHomes().isEmpty());
    }
}
