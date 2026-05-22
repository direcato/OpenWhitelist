# OpenWhitelist

Minecraft whitelist plugin for Paper 1.21.4+. Supports Java and Bedrock (Geyser/Floodgate).

## Install

1. Drop `OpenWhitelist.jar` into your `plugins/` folder
2. Restart your server

## Commands

```
/openw add <name>      - whitelist a player
/openw remove <name>   - unwhitelist
/openw list            - see all whitelisted players
/openw requests        - view pending whitelist requests
/openw accept <name>   - accept a request
/openw reload          - reload config and whitelist
/openw on              - enable whitelist
/openw off             - disable whitelist
```

- **Any player** can accept requests and view the pending list (no op needed)
- Denied players trigger a broadcast: `[OpenWhitelist] PlayerX requested whitelist access — /openw accept PlayerX`
- Bedrock players are detected automatically via Floodgate on join

See [CHANGELOG.md](CHANGELOG.md) for version history.
