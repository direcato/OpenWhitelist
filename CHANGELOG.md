# Changelog

## 1.7.0
- Version now auto-derived from git tags via `git describe`
- New build workflow — verifies compilation on every push and PR
- Release workflow now creates versioned GitHub releases from `v*` tags
- Removed hardcoded version from `build.gradle.kts`
- Removed `.env` file with stale token

## 1.6.0
- Request system: kicked players create a pending request, `/openw requests` and `/openw accept <name>` to approve
- Any player can use any command (no op needed — all permissions default: true)
- Broadcast alerts when a player is denied and when a request is accepted
- Removed `[java|bedrock]` type from `/openw add` — auto-detected via Floodgate
- Removed `/openw update` and all auto-update code
- Single release on GitHub — only updates when the jar changes
- Language system with 17 languages: en, tl, es, fr, de, pt, ru, zh, ja, ko, it, nl, tr, vi, pl, ar, id
- `/openw lang <code>` to switch language in-game (no op needed)
- Config option `language:` to set default language
- All messages loaded from `plugins/OpenWhitelist/lang/lang_<code>.yml` — editable
- Simplified config (removed `update` section)

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
