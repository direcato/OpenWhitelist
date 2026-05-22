# Changelog

## 1.6.0
- Request system: kicked players create a pending request, `/openw requests` and `/openw accept <name>` to approve
- Any player can accept requests (no op required)
- Broadcast alerts when a player is denied (`[OpenWhitelist] PlayerX requested whitelist access — /openw accept PlayerX`)
- Broadcast alerts when a request is accepted
- Removed `[java|bedrock]` type from `/openw add` — type is auto-detected via Floodgate on join
- Removed hot-reload — updates download on startup/command, apply on restart
- Single release on GitHub — only updates when the jar actually changes
- Simplified config (removed `check-interval-hours`)

## 1.5.2
- Fixed hot-reload on Paper 26.1+ (provider storage reflection)
- Backup/restore safety on hot-reload failure

## 1.5.1
- Added `/openw on` and `/openw off` commands
- Console logging for denied players with `[OpenWhitelist]` prefix
- All command actions logged with `[OpenWhitelist]` prefix

## 1.5.0
- Floodgate support with automatic Bedrock detection
- `.` prefix stripping for Bedrock players
- XUID and stripped UUID matching at login
- Auto-update with direct URL + GitHub API fallback
- SHA-256 hash comparison to avoid redundant updates
- YAML storage (`config.yml` + `whitelist.yml`)

## 1.0.0
- Initial release
- Basic whitelist management: `/openw add`, `remove`, `list`, `reload`
- Kick non-whitelisted players on join
