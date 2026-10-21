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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.homes.plugin.data.Home;

import java.security.GeneralSecurityException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HomeEncryptorTest {

    private HomeEncryptor encryptor;
    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    public void setup() {
        byte[] masterKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            masterKey[i] = (byte) (i + 1);
        }
        encryptor = new HomeEncryptor(true, masterKey);
    }

    @Test
    public void testEncryptionAndDecryption() throws GeneralSecurityException {
        Home original = new Home("world_nether", 1234.56, 64.0, -987.65, 180.0f, -45.0f);

        String encrypted = encryptor.encrypt(playerUuid, original);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC:v1:"));
        // Проверяем, что в зашифрованной строке нет открытых координат и названия мира
        assertFalse(encrypted.contains("world_nether"));
        assertFalse(encrypted.contains("1234.56"));
        assertFalse(encrypted.contains("-987.65"));

        Home decrypted = encryptor.decrypt(playerUuid, encrypted);

        assertNotNull(decrypted);
        assertEquals(original.getWorld(), decrypted.getWorld());
        assertEquals(original.getX(), decrypted.getX(), 0.0001);
        assertEquals(original.getY(), decrypted.getY(), 0.0001);
        assertEquals(original.getZ(), decrypted.getZ(), 0.0001);
        assertEquals(original.getYaw(), decrypted.getYaw(), 0.0001f);
        assertEquals(original.getPitch(), decrypted.getPitch(), 0.0001f);
    }

    @Test
    public void testRandomIvProducesDifferentCiphertext() throws GeneralSecurityException {
        Home home = new Home("world", 100.0, 70.0, 100.0, 0.0f, 0.0f);

        String cipher1 = encryptor.encrypt(playerUuid, home);
        String cipher2 = encryptor.encrypt(playerUuid, home);

        assertNotEquals(cipher1, cipher2, "Два шифрования одной локации должны давать разные шифротексты из-за случайного IV");
    }

    @Test
    public void testPlayerKeyIsolation() throws GeneralSecurityException {
        Home home = new Home("world", 500.0, 60.0, 500.0, 90.0f, 0.0f);
        String encrypted = encryptor.encrypt(playerUuid, home);

        UUID anotherPlayer = UUID.randomUUID();
        assertThrows(GeneralSecurityException.class, () -> {
            encryptor.decrypt(anotherPlayer, encrypted);
        }, "Попытка расшифровать чужой хом другим UUID должна вызывать ошибку аутентификации GCM");
    }

    @Test
    public void testUnencryptedJsonCompatibility() throws GeneralSecurityException {
        Home home = new Home("world", 10.0, 20.0, 30.0, 0.0f, 0.0f);
        String plainJson = HomeEncryptor.serializeJson(home);

        Home decrypted = encryptor.decrypt(playerUuid, plainJson);
        assertNotNull(decrypted);
        assertEquals("world", decrypted.getWorld());
        assertEquals(10.0, decrypted.getX(), 0.0001);
    }
}
