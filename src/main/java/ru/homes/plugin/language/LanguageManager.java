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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.homes.plugin.HomesPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Управляет локализацией сообщений, названий предметов и интерфейсов (GUI) плагина.
 * Поддерживает автоизвлечение языковых файлов в plugins/HomesPlugin/languages/,
 * переключение языков на лету через /home reload и fallback на английский/встроенный ресурс при отсутствии ключей.
 */
public class LanguageManager {

    public static final String[] SUPPORTED_LANGUAGES = {
            "ru", "en", "zh", "de", "fr", "es", "pt", "ja", "uk", "it", "tr", "pl", "ko"
    };

    public static final String DEFAULT_LANGUAGE = "ru";
    public static final String FALLBACK_LANGUAGE = "en";

    private final HomesPlugin plugin;
    private final File dataFolder;
    private final Logger logger;

    private FileConfiguration langConfig;
    private FileConfiguration fallbackConfig;
    private String currentLanguage = DEFAULT_LANGUAGE;

    public LanguageManager(HomesPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.logger = plugin.getLogger();
    }

    public LanguageManager(File dataFolder, Logger logger) {
        this.plugin = null;
        this.dataFolder = dataFolder;
        this.logger = logger != null ? logger : Logger.getLogger("HomesLanguage");
    }

    /**
     * Инициализирует языковые файлы и загружает активный язык из конфигурации.
     */
    public synchronized void load() {
        extractDefaultLanguages();

        String configuredLang = DEFAULT_LANGUAGE;
        if (plugin != null) {
            configuredLang = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        }

        loadLanguage(configuredLang);
    }

    /**
     * Перезагружает активный языковой файл (вызывается при /home reload).
     */
    public synchronized void reload() {
        load();
    }

    /**
     * Распаковывает встроенные файлы языков в папку languages/, если они отсутствуют,
     * или дополняет существующие файлы новыми ключами при обновлении плагина.
     */
    public void extractDefaultLanguages() {
        File langDir = getLanguagesFolder();
        if (!langDir.exists() && !langDir.mkdirs()) {
            logger.warning("[Homes] Could not create languages directory at: " + langDir.getAbsolutePath());
        }

        for (String code : SUPPORTED_LANGUAGES) {
            File targetFile = new File(langDir, code + ".yml");
            String resourcePath = "languages/" + code + ".yml";
            if (!targetFile.exists()) {
                try (InputStream in = getResourceStream(resourcePath)) {
                    if (in != null) {
                        try (OutputStream out = new FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        }
                    } else if (plugin != null) {
                        try {
                            plugin.saveResource(resourcePath, false);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[Homes] Could not extract default language file: " + resourcePath, e);
                }
            } else {
                // Если файл уже есть на диске (от предыдущей версии), синхронизируем новые ключи
                syncMissingKeys(targetFile, resourcePath);
            }
        }
    }

    /**
     * Дополняет существующий файл на диске новыми ключами из ресурсов JAR, не перезаписывая существующие настройки.
     */
    public void syncMissingKeys(File diskFile, String resourcePath) {
        try (InputStream in = getResourceStream(resourcePath)) {
            if (in == null) return;
            YamlConfiguration jarConfig = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                jarConfig.load(reader);
            }

            YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(diskFile);
            boolean modified = false;
            for (String key : jarConfig.getKeys(true)) {
                if (!diskConfig.contains(key) && !jarConfig.isConfigurationSection(key)) {
                    diskConfig.set(key, jarConfig.get(key));
                    modified = true;
                }
            }
            if (modified) {
                diskConfig.save(diskFile);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "[Homes] Could not sync missing keys into " + diskFile.getName(), e);
        }
    }

    /**
     * Загружает конкретный язык по коду с автоматическим fallback на en и встроенные JAR-ресурсы.
     */
    public synchronized void loadLanguage(String languageCode) {
        String resolvedCode = resolveLanguageCode(languageCode);
        this.currentLanguage = resolvedCode;

        // 1. Встроенный английский fallback из ресурсов JAR
        YamlConfiguration enJarConfig = loadJarConfig("languages/" + FALLBACK_LANGUAGE + ".yml");

        // 2. Английский fallback файл на диске (если есть)
        File fallbackFile = new File(getLanguagesFolder(), FALLBACK_LANGUAGE + ".yml");
        if (fallbackFile.exists()) {
            this.fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile);
            this.fallbackConfig.setDefaults(enJarConfig);
        } else {
            this.fallbackConfig = enJarConfig;
        }

        // 3. Встроенный активный язык из ресурсов JAR (например, languages/ru.yml)
        YamlConfiguration activeJarConfig = loadJarConfig("languages/" + resolvedCode + ".yml");
        activeJarConfig.setDefaults(this.fallbackConfig);

        // 4. Активный язык на диске
        File langFile = new File(getLanguagesFolder(), resolvedCode + ".yml");
        if (langFile.exists()) {
            this.langConfig = YamlConfiguration.loadConfiguration(langFile);
            this.langConfig.setDefaults(activeJarConfig);
        } else {
            logger.warning("[Homes] Language file " + langFile.getName() + " not found! Falling back to " + FALLBACK_LANGUAGE);
            this.langConfig = this.fallbackConfig;
            this.currentLanguage = FALLBACK_LANGUAGE;
        }
    }

    private YamlConfiguration loadJarConfig(String path) {
        YamlConfiguration cfg = new YamlConfiguration();
        try (InputStream in = getResourceStream(path)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    cfg.load(reader);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "[Homes] Could not load JAR language resource: " + path, e);
        }
        return cfg;
    }

    /**
     * Разрешает код языка (например, ru-RU -> ru, ZH_CN -> zh, или произвольный пользовательский файл custom -> custom).
     */
    public String resolveLanguageCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String clean = rawCode.trim().toLowerCase(Locale.ROOT);

        // Если файл прямо существует с таким именем (например custom.yml или ru_RU.yml)
        File directFile = new File(getLanguagesFolder(), clean + ".yml");
        if (directFile.exists()) {
            return clean;
        }

        // Проверяем разделители локали '-' или '_' (например ru-RU -> ru)
        if (clean.contains("_") || clean.contains("-")) {
            String shortCode = clean.split("[_-]")[0];
            File shortFile = new File(getLanguagesFolder(), shortCode + ".yml");
            if (shortFile.exists()) {
                return shortCode;
            }
        }

        for (String supported : SUPPORTED_LANGUAGES) {
            if (supported.equalsIgnoreCase(clean)) {
                return supported;
            }
        }

        return clean;
    }

    /**
     * Возвращает форматированную строку сообщения с подстановкой плейсхолдеров и цветами (&).
     *
     * @param key          ключ сообщения
     * @param placeholders пары "ключ", значение, "ключ2", значение2
     */
    public String get(String key, Object... placeholders) {
        String message = null;
        if (langConfig != null) {
            message = langConfig.getString(key);
        }
        if (message == null && fallbackConfig != null) {
            message = fallbackConfig.getString(key);
        }
        if (message == null) {
            return key;
        }

        if (placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String ph = "{" + placeholders[i] + "}";
                String val = String.valueOf(placeholders[i + 1]);
                message = message.replace(ph, val);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Возвращает Kyori Adventure Component для отображения в GUI или компонентах Paper/Folia.
     */
    public Component getComponent(String key, Object... placeholders) {
        String msg = get(key, placeholders);
        return LegacyComponentSerializer.legacySection().deserialize(msg)
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Возвращает список форматированных строк (для lore предметов).
     */
    public List<String> getStringList(String key, Object... placeholders) {
        List<String> rawList = null;
        if (langConfig != null) {
            rawList = langConfig.getStringList(key);
        }
        if ((rawList == null || rawList.isEmpty()) && fallbackConfig != null) {
            rawList = fallbackConfig.getStringList(key);
        }
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<>(rawList.size());
        for (String line : rawList) {
            if (placeholders.length > 0 && placeholders.length % 2 == 0) {
                for (int i = 0; i < placeholders.length; i += 2) {
                    String ph = "{" + placeholders[i] + "}";
                    String val = String.valueOf(placeholders[i + 1]);
                    line = line.replace(ph, val);
                }
            }
            list.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return list;
    }

    /**
     * Возвращает список компонентов (например, для lore предметов в инвентаре).
     */
    public List<Component> getComponentList(String key, Object... placeholders) {
        List<String> rawList = null;
        if (langConfig != null) {
            rawList = langConfig.getStringList(key);
        }
        if ((rawList == null || rawList.isEmpty()) && fallbackConfig != null) {
            rawList = fallbackConfig.getStringList(key);
        }
        if (rawList == null || rawList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Component> list = new ArrayList<>(rawList.size());
        for (String line : rawList) {
            if (placeholders.length > 0 && placeholders.length % 2 == 0) {
                for (int i = 0; i < placeholders.length; i += 2) {
                    String ph = "{" + placeholders[i] + "}";
                    String val = String.valueOf(placeholders[i + 1]);
                    line = line.replace(ph, val);
                }
            }
            String colored = ChatColor.translateAlternateColorCodes('&', line);
            list.add(LegacyComponentSerializer.legacySection().deserialize(colored)
                    .decoration(TextDecoration.ITALIC, false));
        }
        return list;
    }

    /**
     * Отправляет локализованное сообщение отправителю команды.
     */
    public void sendMessage(CommandSender sender, String key, Object... placeholders) {
        if (sender == null) return;
        sender.sendMessage(get(key, placeholders));
    }

    public File getLanguagesFolder() {
        return new File(dataFolder, "languages");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    private InputStream getResourceStream(String path) {
        if (plugin != null) {
            InputStream in = plugin.getResource(path);
            if (in != null) return in;
        }
        ClassLoader cl = getClass().getClassLoader();
        return cl != null ? cl.getResourceAsStream(path) : null;
    }
}
