# OpenWhitelist

A Minecraft Paper/Purpur whitelist plugin with support for **Java Edition** and **Bedrock Edition** (via Geyser/Floodgate).

## Features

- `/openw add <name> [java|bedrock]` — Add a player to the whitelist
- `/openw remove <name>` — Remove a player from the whitelist
- `/openw list [page]` — List all whitelisted players
- `/openw reload` — Reload config and whitelist from disk
- `/openw update` — Trigger auto-update check and hot reload
- Bedrock support via Floodgate (XUID/stripped UUID matching, `.` prefix stripping)
- Auto-update with configurable URL and hash verification
- YAML-based storage (`config.yml` + `whitelist.yml`)

## Requirements

- Paper 1.21.4+ or Purpur (version-agnostic)
- Java 21+
- Floodgate (optional — for Bedrock support)

## Commands

| Command | Permission | Description |
|---|---|---|
| `/openw add <name> [type]` | `openwhitelist.add` | Add player (`java` or `bedrock`) |
| `/openw remove <name>` | `openwhitelist.remove` | Remove player |
| `/openw list [page]` | `openwhitelist.list` | Paginated whitelist |
| `/openw reload` | `openwhitelist.reload` | Reload config + data |
| `/openw update` | `openwhitelist.update` | Check & apply updates |

## Building

```bash
./gradlew build
```

The jar will be in `build/libs/`. Paste it into your server's `plugins/` folder.

## License

GNU General Public License v3.0
