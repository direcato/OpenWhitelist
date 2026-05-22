# OpenWhitelist

Minecraft whitelist plugin for Paper 1.21.4+. Supports Java and Bedrock (Geyser/Floodgate).
This Plugins is not version locked so It works on All server version (Some old version might not work due to not testing)

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
/openw update          - download and apply latest version
```

Bedrock players are detected automatically via Floodgate on join.
