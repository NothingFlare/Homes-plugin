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

package ru.homes.plugin.language;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageManagerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExtractionAndDefaultLoading() {
        File dataFolder = tempDir.toFile();
        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("LangTest"));
        lm.load();

        File langDir = new File(dataFolder, "languages");
        assertTrue(langDir.exists(), "Папка languages должна быть создана");

        for (String lang : LanguageManager.SUPPORTED_LANGUAGES) {
            File f = new File(langDir, lang + ".yml");
            assertTrue(f.exists(), "Языковой файл " + lang + ".yml должен существовать");
            assertTrue(f.length() > 0, "Языковой файл " + lang + ".yml не должен быть пустым");
        }

        assertEquals("ru", lm.getCurrentLanguage());
        String sethomeUsage = lm.get("sethome.usage");
        assertTrue(sethomeUsage.contains("/sethome <название>"));

        String bannerHelloRu = lm.get("banner.hello");
        assertTrue(bannerHelloRu.contains("Привет, мир!"));

        String consoleInitRu = lm.get("console.storage-init-sqlite", "file", "homes.db");
        assertTrue(consoleInitRu.contains("Инициализация локального хранилища SQLite (homes.db)..."));
    }

    @Test
    public void testPlaceholdersAndColors() {
        File dataFolder = tempDir.toFile();
        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("LangTest"));
        lm.load();

        String created = lm.get("sethome.created", "name", "castle");
        assertTrue(created.contains("castle"));
        assertTrue(created.startsWith("§a"));

        String limitReached = lm.get("sethome.limit-reached", "count", 5, "limit", 10);
        assertTrue(limitReached.contains("5/10"));

        Component comp = lm.getComponent("gui.main-title");
        assertNotNull(comp);

        List<Component> lore = lm.getComponentList("gui.barrier-lore", "limit", 10);
        assertNotNull(lore);
        assertFalse(lore.isEmpty());
    }

    @Test
    public void testLanguageSwitching() {
        File dataFolder = tempDir.toFile();
        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("LangTest"));
        lm.load();

        lm.loadLanguage("en");
        assertEquals("en", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Home base set."));
        assertTrue(lm.get("banner.hello").contains("Hello World!"));
        assertTrue(lm.get("console.storage-init-sqlite", "file", "homes.db").contains("Initializing SQLite local storage (homes.db)..."));
        assertTrue(lm.get("console.homes-loaded", "count", 0).contains("Loaded 0 homes from database."));

        lm.loadLanguage("zh");
        assertEquals("zh", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("家园 base 已设置。"));

        lm.loadLanguage("de");
        assertEquals("de", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Home base wurde gesetzt."));

        lm.loadLanguage("fr");
        assertEquals("fr", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Le home base a été défini."));

        lm.loadLanguage("es");
        assertEquals("es", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Home base establecido."));

        lm.loadLanguage("pt");
        assertEquals("pt", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Home base definida"));

        lm.loadLanguage("ja");
        assertEquals("ja", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("ホーム base を設定しました。"));

        lm.loadLanguage("uk");
        assertEquals("uk", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Дім base встановлено."));

        lm.loadLanguage("it");
        assertEquals("it", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Home base impostato."));

        lm.loadLanguage("tr");
        assertEquals("tr", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("isimli ev ayarlandı."));

        lm.loadLanguage("pl");
        assertEquals("pl", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("Dom base został utworzony."));

        lm.loadLanguage("ko");
        assertEquals("ko", lm.getCurrentLanguage());
        assertTrue(lm.get("sethome.created", "name", "base").contains("홈 base 설정을 완료했습니다."));
    }

    @Test
    public void testLanguageCodeResolution() {
        File dataFolder = tempDir.toFile();
        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("LangTest"));
        lm.load();

        assertEquals("ru", lm.resolveLanguageCode("ru_RU"));
        assertEquals("en", lm.resolveLanguageCode("en-US"));
        assertEquals("zh", lm.resolveLanguageCode("ZH_CN"));
        assertEquals("pt", lm.resolveLanguageCode("pt-BR"));
    }

    @Test
    public void testFallbackToEnglish() throws Exception {
        File dataFolder = tempDir.toFile();
        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("LangTest"));
        lm.load();

        // Создаем неполный кастомный языковой файл
        File customFile = new File(dataFolder, "languages/custom.yml");
        YamlConfiguration customCfg = new YamlConfiguration();
        customCfg.set("sethome.created", "&aCustom home created: {name}");
        customCfg.save(customFile);

        lm.loadLanguage("custom");
        assertEquals("custom", lm.getCurrentLanguage());

        // Присутствующий ключ
        assertEquals("§aCustom home created: test", lm.get("sethome.created", "name", "test"));

        // Отсутствующий ключ должен браться из fallback (en.yml)
        String delhomeUsage = lm.get("delhome.usage");
        assertTrue(delhomeUsage.contains("/delhome <name>"), "Должен сработать fallback на en.yml");
    }

    @Test
    public void testSyncMissingKeysOnOutdatedDiskFile() throws Exception {
        File dataFolder = tempDir.resolve("outdated").toFile();
        dataFolder.mkdirs();
        File langDir = new File(dataFolder, "languages");
        langDir.mkdirs();

        // Симулируем старый файл ru.yml на диске, где нет секций console: и banner:
        File oldRuFile = new File(langDir, "ru.yml");
        YamlConfiguration oldRuCfg = new YamlConfiguration();
        oldRuCfg.set("sethome.created", "&aХом {name} установлен.");
        oldRuCfg.save(oldRuFile);

        LanguageManager lm = new LanguageManager(dataFolder, Logger.getLogger("OutdatedTest"));
        lm.load();

        // Проверяем, что console.* и banner.* НЕ возвращают сырые имена ключей
        String bannerHello = lm.get("banner.hello");
        assertNotEquals("banner.hello", bannerHello);
        assertTrue(bannerHello.contains("Привет, мир!"));

        String consoleInit = lm.get("console.storage-init-sqlite", "file", "homes.db");
        assertNotEquals("console.storage-init-sqlite", consoleInit);
        assertTrue(consoleInit.contains("Инициализация"));

        // Проверяем, что файл на диске был дополнен новыми ключами
        YamlConfiguration updatedRu = YamlConfiguration.loadConfiguration(oldRuFile);
        assertTrue(updatedRu.contains("console.storage-init-sqlite"));
        assertTrue(updatedRu.contains("banner.hello"));
    }
}
