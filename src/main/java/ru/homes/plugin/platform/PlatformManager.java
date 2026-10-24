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

import org.bukkit.plugin.Plugin;
import ru.homes.plugin.platform.folia.FoliaPlatformAdapter;
import ru.homes.plugin.platform.paper.PaperPlatformAdapter;
import ru.homes.plugin.platform.spigot.SpigotPlatformAdapter;

import java.util.logging.Logger;

/**
 * Менеджер платформы: определяет ядро (Folia, Purpur, Paper, Spigot, Bukkit),
 * версию Minecraft (1.13 - 26.3+) и подключает соответствующий модульный адаптер.
 */
public class PlatformManager {

    private final Plugin plugin;
    private final Logger logger;
    private final PlatformType platformType;
    private final ServerVersion serverVersion;
    private final PlatformAdapter adapter;

    public PlatformManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.platformType = detectPlatformType();
        this.serverVersion = ServerVersion.detect();
        this.adapter = createAdapter();
    }

    private PlatformType detectPlatformType() {
        if (hasFolia()) return PlatformType.FOLIA;
        if (hasPurpur()) return PlatformType.PURPUR;
        if (hasPaper()) return PlatformType.PAPER;
        if (hasSpigot()) return PlatformType.SPIGOT;
        return PlatformType.BUKKIT;
    }

    private boolean hasFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasPurpur() {
        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean hasPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (Throwable t1) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (Throwable t2) {
                return false;
            }
        }
    }

    private boolean hasSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private PlatformAdapter createAdapter() {
        try {
            if (platformType == PlatformType.FOLIA) {
                return new FoliaPlatformAdapter(plugin, serverVersion);
            }
            if (platformType == PlatformType.PURPUR || platformType == PlatformType.PAPER) {
                return new PaperPlatformAdapter(plugin, platformType, serverVersion);
            }
        } catch (Throwable t) {
            logger.warning("[Homes] Could not initialize native " + platformType.getFriendlyName() + " adapter, falling back to Spigot adapter: " + t.getMessage());
        }
        return new SpigotPlatformAdapter(plugin, platformType, serverVersion);
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    public PlatformAdapter getAdapter() {
        return adapter;
    }
}
