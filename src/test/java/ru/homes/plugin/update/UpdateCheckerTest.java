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

package ru.homes.plugin.update;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateCheckerTest {

    @Test
    public void testVersionComparison() {
        // v1 > v2 -> > 0
        assertTrue(UpdateChecker.compareVersions("2.4", "2.3") > 0);
        assertTrue(UpdateChecker.compareVersions("2.4", "2.2") > 0);
        assertTrue(UpdateChecker.compareVersions("2.2", "2.1") > 0);
        assertTrue(UpdateChecker.compareVersions("2.2", "1.4") > 0);
        assertTrue(UpdateChecker.compareVersions("2.2.1", "2.2") > 0);
        assertTrue(UpdateChecker.compareVersions("2.1", "2.0") > 0);
        assertTrue(UpdateChecker.compareVersions("1.4.1", "1.4") > 0);

        // v1 < v2 -> < 0
        assertTrue(UpdateChecker.compareVersions("2.3", "2.4") < 0);
        assertTrue(UpdateChecker.compareVersions("1.4", "2.2") < 0);
        assertTrue(UpdateChecker.compareVersions("2.1", "2.2") < 0);
        assertTrue(UpdateChecker.compareVersions("1.3.2", "1.4") < 0);

        // v1 == v2 -> == 0
        assertEquals(0, UpdateChecker.compareVersions("2.4", "2.4"));
        assertEquals(0, UpdateChecker.compareVersions("v2.4", "2.4"));
        assertEquals(0, UpdateChecker.compareVersions("2.2", "2.2"));
        assertEquals(0, UpdateChecker.compareVersions("v2.2", "2.2"));
        assertEquals(0, UpdateChecker.compareVersions("2.2", "v2.2"));
        assertEquals(0, UpdateChecker.compareVersions("2.1", "2.1"));
        assertEquals(0, UpdateChecker.compareVersions("1.4.0", "1.4"));
    }

    @Test
    public void testLiveApiFetch() {
        UpdateChecker checker = new UpdateChecker("2.4", Logger.getLogger("UpdateCheckerTest"));
        String version = checker.fetchLatestVersion();

        if (version != null) {
            assertFalse(version.isEmpty());
            assertTrue(UpdateChecker.compareVersions("2.4", version) >= 0,
                    "Версия 2.4 должна быть новее или равна текущей версии на Modrinth/GitHub (" + version + ")");
        }
    }
}
