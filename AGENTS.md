# AGENTS.md

This repository contains the LeeSeol Network Minecraft server project. Follow these
rules when working in this workspace.

## Communication

- The user speaks Korean and is learning server development. Explain work in Korean,
  keep commands copyable, and avoid assuming deep coding knowledge.
- Be proactive: inspect, edit, build, deploy, verify, and summarize. Do not stop at a
  plan unless the user explicitly asks for one.
- Keep progress visible. For longer work, report the current step, what changed, and
  how it was verified.
- If the user sends the Korean resume keyword U+C7AC U+AC1C, pronounced "jaegae",
  restart the development environment and Minecraft services, then verify services,
  ports, and recent logs before continuing.

## Project Layout

- `LeeSeolCore/`: Paper plugin for core gameplay tools.
  - Current responsibilities include `/leeseolcore`, server info, launch pads,
    portal triggers, dimension portal restrictions, and Velocity server movement
    through plugin messaging.
- `LeeSeolAuction/`: Paper plugin for admin-opened item auctions, user-submitted
  auction lots, GUI bidding, and Vault economy settlement.
- `LeeSeolEconomy/`: Paper plugin for won-based economy, Vault integration, shop UI,
  NPC shop helpers, and Shift+F server menu.
- `LeeSeolHologram/`: Paper plugin for in-game configurable RGB hologram displays.
- `LeeSeolCombat/`: Paper survival plugin for PvP combat tags, combat logout death,
  and Citizens-based logout combat clones.
- `LeeSeolCleanup/`: Paper survival plugin for periodic dropped-item cleanup in
  survival and internal dungeon worlds.
- `LeeSeolRanks/`: Paper plugin for shared lobby/survival rank data, rank
  permissions, manual rank-up, and rank PlaceholderAPI display.
- `LeeSeolDungeon/`: Paper plugin for the survival server's internal multi-world
  dungeon: dungeon world loading, survival-to-dungeon portals, dungeon block
  protection, and GUI-configurable random loot chests.
- `LeeSeolLobby/`: Paper lobby protection plugin.
- `LeeSeolTown/`: Paper plugin for villages, nations, chunk claims, affiliation
  prefixes, and village/nation chat.
- `LeeSeolProxy/`: Velocity plugin for proxy commands such as `/lobby`, `/survival`,
  and `/servers`.
- Root `deploy-*.sh` files are active deployment helpers. `setup-network-backup.sh`
  and `stop-newworld-service.sh` are active maintenance helpers. Old one-off
  apply/fix/migration scripts were removed to reduce accidental re-runs.

## Context Files

To reduce token usage, check these short files before reading broad code or old logs:

- `SERVER_STATE.md`: current live server architecture, service names, ports, and major
  decisions.
- `PLUGIN_INDEX.md`: plugin ownership, minimal files to read, deploy targets, and
  verification shortcuts.
- `RESOURCEPACK_IMAGE_HANDOFF.md`: resource-pack image generation, TAB logo, hosted
  pack SHA workflow, and repeated pitfalls for image/resource-pack tasks.
- `ITEMSADDER_BLOCK_RESOURCEPACK_RULES.md`: mandatory checklist before adding
  ItemsAdder custom blocks/furniture-like placeable models such as Crates and Stuff.
  Read this before touching ItemsAdder block contents or rebuilding `generated.zip`.
- `SERVER_ANALYSIS_CURRENT.md`: clean Korean current-state server analysis for
  handoff between image/resource-pack work and plugin development work.
- `TOKEN_WORKFLOW.md`: token-saving workflow for one-plugin development and remote
  verification.
- `TODO.md`: deferred work and future feature backlog.
- `REQUEST_TEMPLATE.md`: preferred one-feature request format for new work.

Start from these files, then inspect only the plugin/module directly related to the
current request.

## Token Budget Rules

- Default plugin-task context is `AGENTS.md`, `SERVER_STATE.md`, `PLUGIN_INDEX.md`,
  and the target plugin's minimal files from `PLUGIN_INDEX.md`.
- Do not read every plugin, every deploy script, or broad historical logs for normal
  feature work.
- Treat one request as one primary plugin/feature whenever possible.
- If a task touches multiple plugins, name the exact order before editing and keep the
  read set limited to those plugins.
- Use targeted `rg` inside the target plugin before opening files. Avoid whole-repo
  searches unless the dependency is unknown.
- Use one main chat/session for live VM deployment. Forked chats may analyze or draft,
  but should not deploy unless they first refresh `SERVER_STATE.md` and
  `PLUGIN_INDEX.md`.
- After meaningful changes, update the shortest stable context file instead of
  relying on long chat history:
  - architecture/service state: `SERVER_STATE.md`
  - plugin ownership/deploy target: `PLUGIN_INDEX.md`
  - deferred work: `TODO.md`
  - repeated mistakes: `AGENTS.md`

## Server Architecture

- Current server name/brand is `expedition`; Korean display name is
  `익스페디션 서버`. Use this name for future server UI, docs, and resource-pack
  assets unless the user explicitly changes it.
- Google Cloud VM: `minecraft-server`
- Zone: `asia-northeast3-a`
- VM type: `e2-standard-2`
- Public entry point: Velocity on `0.0.0.0:25565`
- Survival Paper: `/opt/minecraft/server`, service `minecraft`, bound to
  `127.0.0.1:25566`
- Lobby Paper: `/opt/minecraft/lobby`, service `lobby`, bound to
  `127.0.0.1:25567`
- Resource pack host: `/opt/minecraft/lobby/plugins/ItemsAdder/output`, service
  `resourcepack`, bound to `0.0.0.0:8163`
- Newworld Paper: `/opt/minecraft/dungeon`, service `newworld`, bound to
  `127.0.0.1:25568`, registered in Velocity as backend `newworld`.
- Dungeon gameplay world: internal world `dungeon` inside the survival Paper service
  `/opt/minecraft/server`, not a separate Velocity backend.
- Velocity: `/opt/minecraft/velocity`, service `velocity`
- Database: `mariadb`, used for shared LuckPerms data
- Start order: `mariadb`, `resourcepack`, `velocity`, `minecraft`, `lobby`. Start
  `newworld` only when the user explicitly resumes New World development.
- Stop order: `newworld`, `lobby`, `minecraft`, `velocity`, `resourcepack`, `mariadb`
- Stopping services is different from stopping the VM. Only stop the VM when the user
  explicitly asks to prevent cloud costs.

## Remote Work Rules

- Use Google Cloud SDK from Windows when needed:
  `C:\Users\user\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd`
- Prefer uploading scripts to `/tmp` and executing them over making the user paste many
  commands manually.
- Clean up temporary remote files in `/tmp` and temporary local ZIP/script artifacts
  after the task succeeds.
- Always make a timestamped backup before changing deployed configs or jars.
- Do not print secrets such as RCON passwords, database passwords, tokens, or private
  keys.
- PowerShell treats `<# ... #>` as comments. Do not inline TAB RGB strings like
  `<#FFFFFF>` directly in PowerShell commands. Upload a script or use a safe encoded
  command instead.
- `pscp` may not expand `~`; prefer explicit paths such as `/tmp/file.sh`.
- When uploading Java plugin source trees from Windows to the Linux VM, prefer
  `tar -czf <plugin>.tar.gz -C <plugin> .` and extract with `tar -xzf`. Avoid
  PowerShell `Compress-Archive` ZIPs for source trees because they can preserve
  backslash path separators and produce incomplete Maven builds on Linux.

## Repeated Mistakes To Avoid

These issues happened repeatedly on 2026-05-30. Treat them as hard-learned project
rules:

- Do not put complex Bash in `gcloud compute ssh --command` from PowerShell. Loops,
  heredocs, nested quotes, `$variables`, and `|| true` are easy to mangle. Upload a
  small script to `/tmp`, run `bash /tmp/script.sh`, then delete it.
- Do not pass PuTTY/Plink options such as `-- -hostkey ...` to `gcloud compute scp`
  unless the exact gcloud syntax has already been verified. If the host key is already
  cached, use the plain `gcloud compute scp ... --zone ...` form.
- When a command fails due to file ownership under `/opt/minecraft`, do not rerun the
  same non-sudo Python/edit command. Use `sudo python3` for the edit, then reset
  ownership to `yuj1973222:yuj1973222` for the changed config.
- Do not sync survival plugins into `/opt/minecraft/dungeon` as a dungeon backend
  anymore. That backend is now player-facing `newworld`. The actual dungeon is the
  internal `dungeon` world inside `/opt/minecraft/server`.
- `LeeSeolDungeon` should be deployed to `/opt/minecraft/server` only. Remove it from
  `/opt/minecraft/dungeon` if it appears there again.
- Do not recreate Velocity backend `dungeon`. Velocity backend names are currently
  `lobby`, `survival`, and `newworld`.
- Do not recreate systemd service `dungeon.service`. New World uses
  `newworld.service`; the old service name was removed to avoid confusion.
- Keep `LeeSeolDungeon` portal triggers enabled on survival, but default
  `portal-triggers.portals` should stay empty until an in-game admin defines new
  multi-world portals.
- When checking logs after a restart, avoid grepping a large old log window and
  treating historical errors as current failures. Prefer `journalctl --since
  '5 minutes ago'` or a timestamp captured before the restart.
- `systemctl is-active` exits nonzero for expected stopped services. If the goal is to
  stop servers, `inactive` is success. If `failed` appears after stopping, run
  `systemctl reset-failed` and verify `inactive`.
- Service status alone is not enough. Verify the relevant config values, plugin enabled
  lines, `Done`, and recent `ERROR`/`Exception` absence before declaring success.
- If a script intentionally cleans up `/tmp`, run the final verification before cleanup
  or keep a local copy of the script. Cleaning remote helpers too early makes small
  follow-up fixes slower.
- If players get repeated `Invalid session` or proxy login failures, check the whole
  Velocity auth chain before changing unrelated plugins: Velocity `online-mode=true`,
  Paper backend `online-mode=false`, Paper Velocity forwarding enabled, and
  `/opt/minecraft/velocity/forwarding.secret` matching both Paper
  `config/paper-global.yml` files. Compare the secret without printing it.
- For resource-pack edits, do not keep patching a ZIP that fails integrity checks.
  First run `unzip -t` on the hosted `generated.zip`; if it is bad, rebuild from a
  known-good backup or regenerate cleanly. After changing the pack, verify both
  internal and public downloads, confirm the SHA1 in Velocity matches the public file,
  and inspect that required files exist in the downloaded ZIP.
- Do not rely on Paper backend `server.properties` or ItemsAdder auto-apply to
  distribute the network resource pack. This repeatedly caused lobby/survival pack
  drift and missed downloads. The network pack must be offered once from Velocity via
  `LeeSeolProxy`, using one URL and one SHA1, and served by the dedicated
  `resourcepack` service. Verify with an actual `GET` download and SHA1 comparison,
  not only by checking that a port is listening.
- Do not edit ItemsAdder `config.yml` with broad indentation/string-rewrite scripts.
  This broke nested YAML under `resource-pack.hosting.self-host.protection.rate_limit`
  and disabled `/ia`. For ItemsAdder config changes, either restore a known-good
  backup or patch only an exact, validated block; then restart and verify no
  `while parsing a block mapping`, `plugin is disabled`, or `Cannot execute command
  'ia'` log lines appear.
- Do not use a very large PNG as one Minecraft font glyph for TAB logos. A
  `1024x352` expedition logo existed in the pack and downloaded correctly, but the
  client did not render it in TAB while small rank glyphs rendered normally. Keep a
  single TAB logo glyph texture small, currently `220x42`, or split a large logo into
  multiple glyphs and verify it through the hosted ZIP URL.
- If the TAB logo disappears after `/iazip`, ItemsAdder regeneration, or applying a
  third-party GUI pack, first inspect the public `generated.zip`. The usual cause is
  that the regenerated pack omitted the manually injected `assets/expedition/...`
  logo PNG/font provider, not a DeluxeMenus/TAB conflict. Rebuild a clean pack or
  re-inject the expedition assets, then verify public SHA1 equals Velocity SHA1.
- Before adding ItemsAdder custom blocks or placeable model packs, read
  `ITEMSADDER_BLOCK_RESOURCEPACK_RULES.md`. Do not treat placeable blocks as plain
  custom model items, do not use one-off ZIP file copying as the merge strategy, and
  do not apply Crates and Stuff directly to live. Start with one block, use the
  official `specific_properties.block.placed_model` structure, and verify existing
  TAB/logo/HUD/rank resources before testing the new block.

## Build And Deploy

- Use Maven for Java plugin builds unless a module already has a working Gradle flow.
- Respect each module's existing Java release, package names, and manifest files.
- Paper plugin packages use the `me.leeseol...` namespace.
- Velocity plugin packages use the `me.leeseol.proxy...` namespace.
- `LeeSeolEconomy` may reference Vault through the deployed server path
  `/opt/minecraft/server/plugins/Vault.jar`; local Windows builds can fail if that jar
  is unavailable. Prefer server-side build verification for that module.
- After building, verify the jar contains the correct descriptor:
  - Paper: `plugin.yml` or `paper-plugin.yml`
  - Velocity: `velocity-plugin.json`
- Deploy only the needed jar to the correct backend:
  - survival plugins: `/opt/minecraft/server/plugins/`
  - lobby plugins: `/opt/minecraft/lobby/plugins/`
  - newworld plugins: `/opt/minecraft/dungeon/plugins/`
  - Velocity plugins: `/opt/minecraft/velocity/plugins/`
- Newworld's active plugin folder is intentionally empty while the service is paused.
  Do not copy survival/lobby plugin sets into it automatically.
- Do not deploy `LeeSeolDungeon` to newworld; it belongs on survival only.
- Deploy `LeeSeolCombat` only to survival. It depends on Citizens and should not run
  on lobby or newworld unless a later task explicitly changes that.
- Deploy `LeeSeolCleanup` only to survival. Its default cleanup worlds are `world`,
  `world_nether`, and internal `dungeon`.
- Deploy `LeeSeolRanks` to both survival and lobby. It uses shared data at
  `/opt/minecraft/shared/ranks/ranks.yml` and exposes `%leeseolranks_prefix%` for
  TAB/chat rank display.
- Restart only the services affected by the change.

## Verification

- Check service status with `systemctl status <service> --no-pager`.
- Check ports with `ss -ltnp`, especially `25565`, `25566`, `25567`, and `25568`.
- Check logs with `journalctl -u <service> -n <lines> --no-pager`.
- For plugin work, confirm:
  - no new `ERROR`, `Exception`, or `Could not load` lines
  - plugin enabled message appears
  - server reaches `Done`
  - commands or config reloads work in-game when applicable
- When testing Velocity movement, confirm backend server names match `velocity.toml`
  exactly. Current intended names are `lobby`, `survival`, and `newworld`.

## Java Plugin Patterns

- Keep features separated by responsibility:
  - commands in `command`
  - listeners in `listener`
  - managers/services in `manager`, `service`, or feature-specific packages
  - small helpers in `util`
- Prefer config-driven behavior with safe defaults.
- Reload commands should reload all affected managers without requiring a full restart
  when possible.
- Use defensive parsing for worlds, coordinates, materials, sounds, particles, and
  optional config values. Bad config should warn and skip the bad entry, not crash the
  server.
- For `PlayerMoveEvent`, return early unless the player's block coordinates changed.
- For cooldowns, key by player UUID and feature id.
- For Paper-to-Velocity movement, use the BungeeCord plugin messaging channel:
  register outgoing channel `BungeeCord`, send subchannel `Connect`, then the target
  server name. Keep a comment that Velocity behavior needs live-server verification.
- For dungeon movement, do not use Velocity. Use Paper world teleport inside the
  survival service from `world` to the internal `dungeon` world.
- Combat logout clones use Citizens temporary in-memory NPCs. Paper cannot distinguish
  Velocity lobby transfer from a normal survival backend quit unless another plugin
  marks the transfer first, so do not claim that distinction exists without adding an
  explicit marker.
- Dimension restrictions are handled by `LeeSeolCore` `dimension-gate` config:
  lobby blocks all nether/end portal causes, survival allows only `world` <=>
  `world_nether`, and internal `dungeon` blocks both nether and end. The bypass
  permission is `leeseolcore.dimension.bypass` and should stay `default: false`
  unless explicitly granted.
- Keep log output concise: summaries on load, warnings for bad config, no noisy loops.

## Config And Text

- Keep YAML readable and comment important user-facing sections.
- Use UTF-8 for Korean text. If terminal output shows mojibake, fix encoding before
  continuing.
- TAB, MOTD, and Minecraft color formats can be version/plugin-specific. Verify on the
  live server after changing them.
- Preserve user-facing Korean phrasing unless the user asks for a new tone.

## Backups And Cleanup

- Before replacing worlds, plugin configs, or jars, create a timestamped backup.
- Remove obsolete backup candidates only when the user explicitly asks or when a fresh
  verified backup exists and the user already approved cleanup.
- Remove generated temp files created during the current task.
- Never delete user-provided source files, uploaded paid plugin jars, or map archives
  unless the user explicitly asks.

## Deferred Tasks

These are intentionally postponed unless the user asks to resume them:

- Shop UI polish and more advanced shop editing.
- Full-body player-skinned NPC implementation.
- Domain setup for a custom server address.
- Dynamic MOTD by time, maintenance status, server state, or event.
- Per-user TAB themes and event-limited nickname/TAB themes.
- Advanced Korean translation pass for paid plugin configs.

# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
