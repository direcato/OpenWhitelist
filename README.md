# OpenWhitelist

Minecraft whitelist plugin for Paper 1.21.4+. Supports Java and Bedrock (Geyser/Floodgate).

## Back to Life
Im abit casual to reborn the project abit to update some stuff and use workflow so I don't kill my computer for the long gradle build

## Install

1. Drop `OpenWhitelist.jar` into your `plugins/` folder
2. Restart your server

## Commands

```
/openw add <name>        - whitelist a player
/openw remove <name>     - unwhitelist
/openw list              - see all whitelisted players
/openw requests          - view pending whitelist requests
/openw accept <name>     - accept a request
/openw timeout <name>    - temporarily whitelist a player for 5 minutes
/openw set               - whitelist all online players
/openw lang <code>       - switch language (en, tl, es, fr, de, pt, ru, zh, ja, ko, it, nl, tr, vi, pl, ar, id)
/openw reload            - reload config and whitelist
/openw on                - enable whitelist
/openw off               - disable whitelist
```

- **Any player** can use any command (no op needed — all permissions default: `true`)
- Denied players trigger a broadcast: `[OpenWhitelist] PlayerX requested whitelist access — /openw accept PlayerX`
- Bedrock players are detected automatically via Floodgate on join
- Timed entries are cleaned up automatically every 30 seconds

See [CHANGELOG.md](CHANGELOG.md) for version history.
