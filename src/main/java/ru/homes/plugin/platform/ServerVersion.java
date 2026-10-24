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

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Распознает и сравнивает версию сервера (поддерживает 1.13 - 26.3+).
 */
public class ServerVersion {

    private static final Pattern MC_IN_VERSION_PATTERN = Pattern.compile("MC:\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    public ServerVersion(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw != null ? raw : "";
    }

    public static ServerVersion detect() {
        String bukkitVer = null;
        try {
            bukkitVer = Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {}

        String serverVer = null;
        try {
            serverVer = Bukkit.getVersion();
        } catch (Throwable ignored) {}

        return parse(bukkitVer, serverVer);
    }

    public static ServerVersion parse(String bukkitVer, String serverVer) {
        if (serverVer != null) {
            Matcher mcMatcher = MC_IN_VERSION_PATTERN.matcher(serverVer);
            if (mcMatcher.find()) {
                int maj = Integer.parseInt(mcMatcher.group(1));
                int min = Integer.parseInt(mcMatcher.group(2));
                int pat = mcMatcher.group(3) != null ? Integer.parseInt(mcMatcher.group(3)) : 0;
                return new ServerVersion(maj, min, pat, serverVer);
            }
        }

        String candidate = (bukkitVer != null && !bukkitVer.isEmpty()) ? bukkitVer : serverVer;
        if (candidate != null) {
            Matcher matcher = VERSION_PATTERN.matcher(candidate);
            if (matcher.find()) {
                int maj = Integer.parseInt(matcher.group(1));
                int min = Integer.parseInt(matcher.group(2));
                int pat = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                return new ServerVersion(maj, min, pat, candidate);
            }
        }

        return new ServerVersion(1, 20, 0, candidate != null ? candidate : "unknown");
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getRaw() {
        return raw;
    }

    public boolean isAtLeast(int reqMajor, int reqMinor) {
        if (this.major > reqMajor) return true;
        if (this.major == reqMajor) return this.minor >= reqMinor;
        return false;
    }

    public boolean isAtLeast(int reqMajor, int reqMinor, int reqPatch) {
        if (this.major > reqMajor) return true;
        if (this.major == reqMajor) {
            if (this.minor > reqMinor) return true;
            if (this.minor == reqMinor) return this.patch >= reqPatch;
        }
        return false;
    }

    public String getFriendlyName() {
        if (patch > 0) {
            return major + "." + minor + "." + patch;
        }
        return major + "." + minor;
    }

    @Override
    public String toString() {
        return getFriendlyName();
    }
}
