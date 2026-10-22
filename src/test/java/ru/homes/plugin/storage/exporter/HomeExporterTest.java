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

package ru.homes.plugin.storage.exporter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.homes.plugin.data.Home;
import ru.homes.plugin.security.HomeEncryptor;
import ru.homes.plugin.storage.SqliteStorage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class HomeExporterTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseYamlRecords() throws Exception {
        File yamlFile = tempDir.resolve("homes.yml").toFile();
        YamlConfiguration cfg = new YamlConfiguration();

        UUID uuid = UUID.randomUUID();
        cfg.set(uuid + ".home1.world", "world");
        cfg.set(uuid + ".home1.x", 10.0);
        cfg.set(uuid + ".home1.y", 64.0);
        cfg.set(uuid + ".home1.z", -20.0);
        cfg.set(uuid + ".home1.yaw", 0.0f);
        cfg.set(uuid + ".home1.pitch", 0.0f);

        cfg.set(uuid + ".home2.world", "world_nether");
        cfg.set(uuid + ".home2.x", 100.5);
        cfg.set(uuid + ".home2.y", 70.0);
        cfg.set(uuid + ".home2.z", 50.0);
        cfg.set(uuid + ".home2.yaw", 90.0f);
        cfg.set(uuid + ".home2.pitch", 45.0f);

        cfg.save(yamlFile);

        // Проверяем прямое чтение YAML
        YamlConfiguration loadedCfg = YamlConfiguration.loadConfiguration(yamlFile);
        assertEquals(1, loadedCfg.getKeys(false).size());

        org.bukkit.configuration.ConfigurationSection section = loadedCfg.getConfigurationSection(uuid.toString());
        assertNotNull(section);
        assertEquals(2, section.getKeys(false).size());

        Home h1 = new Home(
                section.getString("home1.world"),
                section.getDouble("home1.x"),
                section.getDouble("home1.y"),
                section.getDouble("home1.z"),
                (float) section.getDouble("home1.yaw"),
                (float) section.getDouble("home1.pitch")
        );
        assertEquals("world", h1.getWorld());
        assertEquals(10.0, h1.getX(), 0.001);
    }

    @Test
    public void testSqliteExportFlow() throws Exception {
        File dbFile = tempDir.resolve("homes.db").toFile();
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) key[i] = (byte) (i + 7);
        HomeEncryptor encryptor = new HomeEncryptor(true, key);

        SqliteStorage sqlite = new SqliteStorage(dbFile, encryptor, Logger.getLogger("ExportTest"));
        sqlite.init();

        UUID p1 = UUID.randomUUID();
        sqlite.saveHome(p1, "Player1", "main", new Home("world", 100, 64, -200, 0, 0));
        sqlite.saveHome(p1, "Player1", "base", new Home("world_the_end", 0, 50, 0, 0, 0));

        var loaded = sqlite.loadAllHomes();
        assertEquals(1, loaded.size());
        assertEquals(2, loaded.get(p1).size());

        sqlite.close();
    }
}
