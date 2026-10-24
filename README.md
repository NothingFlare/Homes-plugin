# Homes (by NothingFlare)

**Homes** is a fast, secure, and lightweight home management plugin for Minecraft servers[cite: 4]. Featuring a clean compass GUI[cite: 5], multi-backend storage (**SQLite, MySQL, and Legacy YAML**)[cite: 4], end-to-end **AES-256-GCM coordinate encryption** for complete player privacy[cite: 4], and built-in localization in **13 languages**[cite: 4].

---

## Commands

### Player & Admin Commands

| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/sethome <name>` | `/sethomes` | Sets a home at your current location[cite: 4, 5] |
| `/delhome <name>` | `/rehome`, `/deletehome` | Deletes an existing home[cite: 4, 5] |
| `/home <name>` | `/h` | Teleports you to the specified home[cite: 4, 5] |
| `/homes` | `/homelist` | Opens the homes GUI (compasses, click to teleport)[cite: 4, 5] |
| `/home reload` | — | Reloads configuration, language files, and hot-swaps storage backends[cite: 4] |

### Console-Only Commands
*(Strictly restricted to the server console — in-game players, even OPs, cannot execute these)*[cite: 4]

| Command | Description |
| :--- | :--- |
| `/home clear <player\|all>` | Purges all homes of a specific player or all players across the server[cite: 4] |
| `/homes add <homes.yml\|sqlite\|legacy> to mysql <user:password>` | Migrates and encrypts all homes from YAML or SQLite directly into MySQL with zero server lag[cite: 4] |

---

## Permissions

| Node | Description | Default |
| :--- | :--- | :--- |
| `homes.sethome` | Allows setting homes[cite: 5] | Everyone (`true`)[cite: 5] |
| `homes.delhome` | Allows deleting homes[cite: 5] | Everyone (`true`)[cite: 5] |
| `homes.home` | Allows teleporting to a home[cite: 5] | Everyone (`true`)[cite: 5] |
| `homes.homes` | Allows opening the homes GUI[cite: 5] | Everyone (`true`)[cite: 5] |
| `homes.bypass` | Bypasses the maximum home limit (`home-limit`)[cite: 5] | `op`[cite: 5] |

---

## Key Features

### Storage Backends & Hot-Swapping

| Backend | Description |
| :--- | :--- |
| **SQLite** *(Default)* | Local `homes.db` running in **WAL mode** (*Write-Ahead Logging*) with an isolated connection pool to prevent file locking[cite: 4] |
| **MySQL / MariaDB** | Remote database support powered by an integrated, shaded, and relocated **HikariCP** connection pool[cite: 4] |
| **Legacy YAML** | Uses `homes.yml` with atomic file saving via `.tmp` files (`ATOMIC_MOVE`) to prevent corruption during crashes[cite: 4] |
| **Hot-Swap Support** | Switch between `sqlite`, `mysql`, and `legacy` in `config.yml` on the fly using `/home reload` without restarting the server[cite: 4] |

### Admin-Proof Privacy (AES-256-GCM Encryption)
* **End-to-End Coordinate Encryption:** All coordinates ($X, Y, Z$), worlds, and viewing angles ($Yaw, Pitch$) are encrypted with **AES-256-GCM** before being written to SQLite, MySQL, or `homes.yml`[cite: 4].
* **Zero Admin Snooping:** Server admins inspecting databases via phpMyAdmin, DBeaver, HeidiSQL, or text editors cannot view player coordinates (stored as encrypted `ENC:v1:...` strings)[cite: 4].
* **Per-Player Keys:** Unique keys are derived via `HMAC-SHA256(MasterKey, PlayerUUID)` with a unique 12-byte IV per record[cite: 4]. Automatically generates a 256-bit key in `.encryption_key` if left blank[cite: 4].

### Multi-Language Support (13 Languages)
* **Built-in Locales:** Includes `en`, `ru`, `zh`, `de`, `fr`, `es`, `pt`, `ja`, `uk`, `it`, `pl`, `tr`, and `ko` inside the `languages/` folder[cite: 4].
* **Full Localization:** In-game messages, console logs, startup banners, and update notifications follow the selected language[cite: 4].
* **Auto-Sync:** Existing language files automatically receive new keys on update without overwriting custom edits[cite: 4].

---

## Configuration (`config.yml`)

```yaml
# Language selection (en, ru, zh, de, fr, es, pt, ja, uk, it, pl, tr, ko)
language: "en"

storage:
  type: "sqlite" # "sqlite", "mysql", or "legacy"
  sqlite:
    file: "homes.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "homes"
    username: "root"
    password: ""
    table-prefix: "homes_"
    ssl: false
    pool-size: 10

encryption:
  enabled: true
  secret-key: "" # Leave blank to auto-generate a 256-bit key in .encryption_key

check-updates: true
home-limit: 10
teleport-timeout: 5
