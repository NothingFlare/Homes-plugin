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

package ru.homes.plugin.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServerVersionTest {

    @Test
    public void testParseStandardVersions() {
        ServerVersion v113 = ServerVersion.parse("1.13.2-R0.1-SNAPSHOT", "git-Paper-500 (MC: 1.13.2)");
        assertEquals(1, v113.getMajor());
        assertEquals(13, v113.getMinor());
        assertEquals(2, v113.getPatch());
        assertEquals("1.13.2", v113.getFriendlyName());
        assertTrue(v113.isAtLeast(1, 13));
        assertFalse(v113.isAtLeast(1, 14));

        ServerVersion v120 = ServerVersion.parse("1.20.4-R0.1-SNAPSHOT", "git-Paper-496 (MC: 1.20.4)");
        assertEquals(1, v120.getMajor());
        assertEquals(20, v120.getMinor());
        assertEquals(4, v120.getPatch());
        assertTrue(v120.isAtLeast(1, 13));
        assertTrue(v120.isAtLeast(1, 20));
        assertTrue(v120.isAtLeast(1, 20, 4));
        assertFalse(v120.isAtLeast(1, 21));

        ServerVersion v26_3 = ServerVersion.parse("26.3-R0.1-SNAPSHOT", "git-Folia-10 (MC: 26.3.0)");
        assertEquals(26, v26_3.getMajor());
        assertEquals(3, v26_3.getMinor());
        assertTrue(v26_3.isAtLeast(1, 13));
        assertTrue(v26_3.isAtLeast(1, 20));
        assertTrue(v26_3.isAtLeast(26, 3));
    }

    @Test
    public void testVersionComparisons() {
        ServerVersion v = new ServerVersion(1, 20, 2, "1.20.2");
        assertTrue(v.isAtLeast(1, 13));
        assertTrue(v.isAtLeast(1, 19));
        assertTrue(v.isAtLeast(1, 20));
        assertTrue(v.isAtLeast(1, 20, 2));
        assertFalse(v.isAtLeast(1, 20, 3));
        assertFalse(v.isAtLeast(1, 21));
    }
}
