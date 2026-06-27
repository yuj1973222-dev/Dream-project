# SERVER_STATE.md

Compact live-state summary. Update only when architecture, services, deployed
responsibilities, or major operational decisions change.

## Brand

- Brand: `expedition`
- Korean display name: `익스페디션 서버`

## VM And Services

| Service | Role | Path | Bind | Normal |
| --- | --- | --- | --- | --- |
| `velocity` | public proxy | `/opt/minecraft/velocity` | `0.0.0.0:25565` | active |
| `resourcepack` | static pack host | `/opt/minecraft/lobby/plugins/ItemsAdder/output` | `0.0.0.0:8163` | active |
| `minecraft` | survival Paper | `/opt/minecraft/server` | `127.0.0.1:25566` | active |
| `lobby` | lobby Paper | `/opt/minecraft/lobby` | `127.0.0.1:25567` | active |
| `newworld` | future backend | `/opt/minecraft/newworld` | `127.0.0.1:25568` | inactive unless requested |
| `mariadb` | LuckPerms DB | system service | local | active |

VM: `minecraft-server`, zone `asia-northeast3-a`, type `e2-standard-2`.

Normal start:

```bash
sudo systemctl start mariadb resourcepack velocity minecraft lobby
```

Normal stop:

```bash
sudo systemctl stop lobby minecraft velocity resourcepack
```

Do not stop the VM unless the user explicitly asks to reduce cloud cost.

## Network Model

- Velocity backend names: `lobby`, `survival`, `newworld`.
- Velocity initial backend: lobby.
- Survival/newworld disconnect fallback is handled by `LeeSeolProxy`.
- If lobby maintenance is enabled in
  `/opt/minecraft/velocity/plugins/leeseolproxy/network.properties`, login is denied
  instead of redirecting.
- Velocity online mode is true. Paper backends use online-mode false with Velocity
  forwarding enabled. Forwarding secret must match across Velocity, survival, and
  lobby.

## Worlds

- Lobby:
  - Current map: `IC-Lobby20`.
  - Role: movement-only gate to future PVP, survival/RPG, and nation-war servers.
  - Survival-linked lobby plugins are disabled.
  - `LeeSeolCore` handles Shift+F lobby menu and Citizens movement NPCs.
  - `LeeSeolLobby` protects spawn behavior, item drop blocking, and void return.
  - WorldGuard `__global__` protects the whole lobby world.
- Survival:
  - Main world regenerated on 2026-06-24 with Terralith, Incendium, and Trek
    datapacks. Geophilic is intentionally excluded. Player data under
    `world/players` was preserved.
  - World border: center `0,0`, square diameter `30000`, radius `15000`.
  - Chunky pregeneration was started on 2026-06-24 for `world`, square,
    spawn/center `0,0`, radius `7500`.
  - Chunky pregeneration and BlueMap rendering are operator-owned; do not check or
    continue them unless explicitly requested.
  - BlueMap is public at `http://34.64.126.179:8100/`.
  - BlueMap data is on attached disk `minecraft-bluemap-data` mounted at
    `/mnt/bluemap-data`; `/opt/minecraft/server/bluemap` is a symlink to
    `/mnt/bluemap-data/bluemap`.
  - BlueMap is configured for low-storage ground-focused rendering: overworld map
    only, flat view enabled, perspective/free-flight disabled, hires disabled,
    no coordinate render-mask, `min-inhabited-time: 0`, and caves removed with
    `remove-caves-below-y: 10000`.
  - BlueMap `player-render-limit` is `0`, so rendering does not pause when players
    are online.
  - BlueMap `overworld` was purged and updated for currently generated chunks on
    2026-06-24 after the world regeneration. Start further render work only when
    the user asks or is ready to preload the map.
  - BlueMap render threads were started on 2026-06-24 for `overworld` updates while
    Chunky radius `7500` pregeneration is running.
  - A transient watcher service `bluemap-render-after-chunky.service` was started on
    2026-06-24. It waits for Chunky to stop and at least 850 overworld region files,
    then runs `bluemap start`, `bluemap purge overworld`, and `bluemap tasks`.
    Watcher script/log: `/opt/minecraft/tools/bluemap-render-after-chunky.sh`,
    `/opt/minecraft/tools/bluemap-render-after-chunky.log`.
  - Chunky is available for world-targeted chunk generation work. LeeSeolCore has
    world separation settings such as `enabled-worlds`, `survival-respawn.worlds`,
    `survival-spawn-return.worlds`, and `dimension-gate.survival.*`.
  - `dungeon` is created by `LeeSeolDungeon`; long-term test/tutorial worlds should
    be added to the LeeSeolCore world lists after their live world folders are
    created/loaded.
- Dungeon:
  - Internal survival world named `dungeon`.
  - Not a Velocity backend and not a separate service.
- Newworld:
  - Separate future Paper backend.
  - Normally disabled/inactive.
  - Active plugin folder intentionally empty.

## Current Key Systems

- `LeeSeolCore`: dimension gates, survival death respawn, `/survivalspawn`, Shift+F
  vanilla menus, lobby Citizens movement NPCs.
- `LeeSeolProxy`: proxy movement commands, resource-pack offer, maintenance gate,
  backend fallback.
- `LeeSeolEconomy`: won economy, `/pay`, `/market`, `/won report`, survival menu,
  Vault integration.
- `LeeSeolTown`: parties/nations/claims, dynamic land costs, nation upkeep,
  neutral zones, BlueMap markers, WorldGuard neutral-zone regions, survival sidebar
  scoreboard, neutral-zone entry/exit actionbar notices, and ItemsAdder/WorldEdit
  territory structure placement with in-memory `/party structure undo`. The sidebar
  uses fixed scoreboard teams, hides score numbers, refreshes slowly, and updates
  neutral-zone display from join/entry/exit state instead of live zone polling.
- `LeeSeolEnchanting`: controlled custom-enchant table route and one-line lore.
- `LeeSeolHUD`: disabled legacy. BetterHUD is the active HUD platform on survival.

## Neutral Zones

Admin flow:

```mcfunction
//wand
/content add neutral <id> [displayName]
/content remove neutral <id>
/content list neutral
/content tp neutral <id>
```

LeeSeolCore owns neutral-zone location registration. The add command uses the
WorldEdit selection and creates all of these together:

- LeeSeolCore content data in `/opt/minecraft/server/plugins/LeeSeolCore/contents.yml`
- WorldGuard region `leeseol_content_neutral_<id>` with the common content
  protection preset
- claim blocking over the zone's chunk footprint plus buffer chunks
- actionbar entry and exit notices
- BlueMap marker in set `leeseol-neutral-zones`

LeeSeolTown consumes LeeSeolCore neutral contents for protection and claim blocking.
For compatibility, it falls back to legacy
`/opt/minecraft/shared/town/neutral-zones.yml` only while no Core neutral content is
registered.

Current deployment checked on 2026-06-25: Core neutral contents contain `central`
and `ne1`; LeeSeolTown loads neutral zones from LeeSeolCore. The legacy file is
kept only as a rollback reference.

## External Plugins

- WorldEdit/WorldGuard active on survival and lobby.
- BlueMap active on survival, webserver port `8100`.
- BlueMap disk monitor is active via `bluemap-disk-monitor.timer`; it checks
  `/mnt/bluemap-data` every 30 minutes and logs WARN at 80%, CRITICAL at 90%.
- Chunky active on survival.
- BetterHUD 2.0.0 active on survival.
- Network resource-pack SHA after leeseolwar `_b_leeseolwar` texture namespace fix:
  `fd89a45188463d0bd67e92c6c57c0bf473a1a443`.
- MythicMobs was removed from survival on 2026-06-13. The jar and data folder are
  archived in the backup path below instead of being hard-deleted.
- CoreProtect 23.2 failed on current Paper `26.1.2` and is disabled.
- DeluxeMenus disabled on lobby and survival; Shift+F uses plain inventory GUIs.
- ItemsAdder learning/default custom content is disabled.
- ItemsAdder `leeseolwar` namespace is active on survival and lobby for
  `capital_core`, `border_outpost_core`, and `supply_depot_core`.

## Important Backups

- Survival datapack reset: `/opt/minecraft/backups/survival-datapack-reset-2026-06-12_23-18-16`
- Lobby `IC-Lobby20` install: `/opt/minecraft/backups/lobby-ic-lobby20-2026-06-12_20-55-15`
- Lobby protection: `/opt/minecraft/backups/lobby-protection-2026-06-12_23-00-53`
- BlueMap download acceptance: `/opt/minecraft/backups/bluemap-accept-download-2026-06-12_23-30-28`
- BlueMap 50GB disk mount:
  `/opt/minecraft/backups/bluemap-disk-mount-20260618-222636`
- BlueMap low-storage enable:
  `/opt/minecraft/backups/bluemap-low-storage-enable-20260618-222833`
- BlueMap player render pause disabled:
  `/opt/minecraft/backups/bluemap-disable-player-pause-20260618-224635`
- BlueMap full generated-chunk render:
  `/opt/minecraft/backups/bluemap-full-render-20260619-193507`
- Survival world regeneration:
  `/opt/minecraft/backups/world-regen-20260624-031539`
- LeeSeolTown neutral WorldGuard integration:
  `/opt/minecraft/backups/leeseoltown-neutral-worldguard-2026-06-13_22-05-02`
- LeeSeolTown scoreboard deploy and MythicMobs removal:
  `/opt/minecraft/backups/town-scoreboard-mythicmobs-remove-2026-06-13_22-29-55`
- LeeSeolTown scoreboard text fix:
  `/opt/minecraft/backups/leeseoltown-scoreboard-textfix-2026-06-13_22-44-46`
- LeeSeolTown stable scoreboard update:
  `/opt/minecraft/backups/leeseoltown-scoreboard-stable-2026-06-13_22-53-23`
- LeeSeolTown scoreboard slot fix:
  `/opt/minecraft/backups/leeseoltown-scoreboard-slotfix-2026-06-13_22-57-45`
- LeeSeolTown territory structure placement:
  `/opt/minecraft/backups/leeseoltown-structures-20260619-195641`
- LeeSeolTown custom core GUI and territory notice fix:
  `/opt/minecraft/backups/leeseoltown-core-gui-territory-20260619-201343`
- LeeSeolTown claim-boundary movement throttle:
  `/opt/minecraft/backups/leeseoltown-claimmove-throttle-20260619-201928`
- LeeSeolTown structure undo:
  `/opt/minecraft/backups/leeseoltown-structure-undo-20260619-203336`
- LeeSeolTown nation claim command aliases:
  `/opt/minecraft/backups/leeseoltown-nation-claim-command-20260619-205025`
- Feature diagnostics deploy:
  `/opt/minecraft/backups/feature-diagnostics-20260616-004832`
- LeeSeolTown diagnostics config repair:
  `/opt/minecraft/backups/feature-diagnostics-config-20260616-005239`
- ItemsAdder leeseolwar core blocks:
  `/opt/minecraft/backups/itemsadder-leeseolwar-20260615-175503`,
  `/opt/minecraft/backups/itemsadder-leeseolwar-finalize-20260615-180019`,
  `/opt/minecraft/backups/resourcepack-restore-betterhud-leeseolwar-20260615-180848`,
  `/opt/minecraft/backups/resourcepack-fix-leeseolwar-cmd-20260615-181030`,
  `/opt/minecraft/backups/leeseolwar-core-textures-20260615-181613`,
  `/opt/minecraft/backups/leeseolwar-align-survival-pack-20260615-182403`,
  `/opt/minecraft/backups/leeseolwar-fix-block-texture-namespace-20260615-182848`

## Quick Verify

```bash
sudo python3 /opt/minecraft/tools/remote-rcon.py survival "version LeeSeolTown"
sudo /opt/minecraft/tools/leeseol-featurecheck.sh
sudo /opt/minecraft/tools/bluemap-disk-check.sh /mnt/bluemap-data
bash /opt/minecraft/tools/remote-service-check.sh "5 minutes ago"
```

Reusable remote helpers installed on 2026-06-13:

- `/opt/minecraft/tools/remote-rcon.py`
- `/opt/minecraft/tools/remote-service-check.sh`
- `/opt/minecraft/tools/leeseol-featurecheck.sh` (`--fix` runs safe feature reloads)
- `/opt/minecraft/tools/bluemap-disk-check.sh`
