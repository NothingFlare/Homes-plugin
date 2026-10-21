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

package ru.homes.plugin.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.homes.plugin.data.Home;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обеспечивает шифрование данных хомов (AES-256-GCM), чтобы администраторы
 * сервера или посторонние лица не могли прочесть координаты, названия миров
 * и углы поворота в базе данных (MySQL / SQLite).
 *
 * Для каждого игрока генерируется уникальный производный ключ шифрования
 * на основе мастер-ключа и UUID игрока (HMAC-SHA256).
 * Каждая запись шифруется с новым случайным вектором инициализации (IV),
 * поэтому даже одинаковые координаты выглядят в базе совершенно по-разному.
 */
public class HomeEncryptor {

    public static final String PREFIX = "ENC:v1:";
    private static final int GCM_IV_LENGTH = 12; // 96 бит
    private static final int GCM_TAG_LENGTH = 128; // 128 бит
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256";

    private final boolean enabled;
    private final byte[] masterKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Logger logger;

    public HomeEncryptor(boolean enabled, String configSecret, File dataFolder, Logger logger) {
        this.enabled = enabled;
        this.logger = logger;
        this.masterKey = resolveMasterKey(configSecret, dataFolder);
    }

    /**
     * Конструктор для прямого задания мастер-ключа (удобно для тестов).
     */
    public HomeEncryptor(boolean enabled, byte[] masterKey) {
        this.enabled = enabled;
        this.masterKey = masterKey != null ? masterKey.clone() : new byte[32];
        this.logger = Logger.getLogger(HomeEncryptor.class.getName());
    }

    private byte[] resolveMasterKey(String configSecret, File dataFolder) {
        if (configSecret != null && !configSecret.trim().isEmpty() && !configSecret.trim().equalsIgnoreCase("auto")) {
            String trimmed = configSecret.trim();
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed);
                if (decoded.length == 32) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // Если не Base64, хешируем строку через SHA-256
            }
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                return sha256.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }

        // Автогенерация постоянного мастер-ключа в файл
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File keyFile = new File(dataFolder, ".encryption_key");
        if (keyFile.exists()) {
            try {
                String line = Files.readString(keyFile.toPath()).trim();
                byte[] decoded = Base64.getDecoder().decode(line);
                if (decoded.length == 32) {
                    return decoded;
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.log(Level.WARNING, "Не удалось прочитать .encryption_key, генерирую новый ключ", e);
                }
            }
        }

        byte[] generated = new byte[32];
        secureRandom.nextBytes(generated);
        try {
            Files.writeString(keyFile.toPath(), Base64.getEncoder().encodeToString(generated));
            if (logger != null) {
                logger.info("[Homes] Создан новый секретный ключ шифрования в " + keyFile.getName());
            }
        } catch (IOException e) {
            if (logger != null) {
                logger.log(Level.SEVERE, "Не удалось сохранить файл .encryption_key!", e);
            }
        }
        return generated;
    }

    /**
     * Создаёт уникальный ключ AES-256 для конкретного игрока на основе мастер-ключа и UUID.
     */
    private SecretKey derivePlayerKey(UUID playerUuid) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(masterKey, HMAC_ALGO));
        byte[] derivedBytes = mac.doFinal(playerUuid.toString().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(derivedBytes, "AES");
    }

    /**
     * Зашифровывает данные хома в строку для сохранения в БД.
     */
    public String encrypt(UUID playerUuid, Home home) throws GeneralSecurityException {
        if (home == null) return null;
        String json = serializeJson(home);

        if (!enabled) {
            return json;
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, derivePlayerKey(playerUuid), spec);

        byte[] cipherText = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Расшифровывает строку из БД в объект Home.
     * Поддерживает как зашифрованные записи (ENC:v1:...), так и незашифрованный JSON.
     */
    public Home decrypt(UUID playerUuid, String data) throws GeneralSecurityException {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        if (data.startsWith(PREFIX)) {
            String base64 = data.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64);

            if (combined.length < GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
                throw new IllegalArgumentException("Зашифрованные данные хома повреждены (длина < IV + Tag)");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            int cipherLength = combined.length - GCM_IV_LENGTH;
            byte[] cipherText = new byte[cipherLength];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherLength);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, derivePlayerKey(playerUuid), spec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            String json = new String(plainBytes, StandardCharsets.UTF_8);
            return deserializeJson(json);
        }

        // Если данные не зашифрованы (например, перенесены из старой БД или отключено шифрование)
        return deserializeJson(data);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static String serializeJson(Home home) {
        JsonObject obj = new JsonObject();
        obj.addProperty("world", home.getWorld());
        obj.addProperty("x", home.getX());
        obj.addProperty("y", home.getY());
        obj.addProperty("z", home.getZ());
        obj.addProperty("yaw", home.getYaw());
        obj.addProperty("pitch", home.getPitch());
        return obj.toString();
    }

    public static Home deserializeJson(String jsonString) {
        JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
        String world = obj.get("world").getAsString();
        double x = obj.get("x").getAsDouble();
        double y = obj.get("y").getAsDouble();
        double z = obj.get("z").getAsDouble();
        float yaw = obj.get("yaw").getAsFloat();
        float pitch = obj.get("pitch").getAsFloat();
        return new Home(world, x, y, z, yaw, pitch);
    }
}
