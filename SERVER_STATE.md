# SERVER_STATE.md

Current expedition server state. Keep this file short and update it after
architecture changes.

## Server Brand

- Current server name/brand: `expedition`
- Korean display name: `익스페디션 서버`
- Use `expedition` for future server naming, UI labels, and resource-pack assets
  unless the user explicitly asks for a different name.

## Cloud VM

- Provider: Google Cloud
- VM name: `minecraft-server`
- Zone: `asia-northeast3-a`
- Machine type: `e2-standard-2`
- Public entry point: Velocity on port `25565`
- Google Cloud SDK path on PC:
  `C:\Users\user\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd`

## Live Services

- `velocity`
  - Path: `/opt/minecraft/velocity`
  - Public bind: `0.0.0.0:25565`
  - Status target during normal operation: active
- `resourcepack`
  - Dedicated static resource-pack host
  - Path served: `/opt/minecraft/lobby/plugins/ItemsAdder/output`
  - Public bind: `0.0.0.0:8163`
  - Status target during normal operation: active
- `minecraft`
  - Survival Paper server
  - Path: `/opt/minecraft/server`
  - Backend bind: `127.0.0.1:25566`
  - Status target during normal operation: active
- `lobby`
  - Lobby Paper server
  - Path: `/opt/minecraft/lobby`
  - Backend bind: `127.0.0.1:25567`
  - Status target during normal operation: active
- `newworld`
  - Future New World Paper server
  - Path: `/opt/minecraft/dungeon`
  - Backend bind: `127.0.0.1:25568`
  - Velocity backend name: `newworld`
  - Current status on 2026-06-05: active for New World datapack development
  - Default outside New World development: disabled/inactive until the user asks to
    develop New World
  - Active plugin folder is intentionally empty.
  - Old plugin copies are quarantined in `/opt/minecraft/dungeon/plugins.disabled/`.
  - Main world folder: `/opt/minecraft/dungeon/New_world`
  - Installed New World datapacks:
    `Terralith_26.1_v2.6.2.zip` and
    `Incendium_26.1_v5.4.12_UNSUPPORTED.zip` in
    `/opt/minecraft/dungeon/New_world/datapacks/`
  - The `New_world` folder was regenerated after datapacks were installed on
    2026-06-05, so newly generated overworld/nether chunks use Terralith and
    Incendium from first world creation.
  - Previous New World backup before regeneration:
    `/opt/minecraft/dungeon/backups/New_world-before-datapack-reset-20260605-195752`
  - Previous datapacks backup before regeneration:
    `/opt/minecraft/dungeon/backups/datapacks-before-datapack-reset-20260605-195752`
- `mariadb`
  - Shared LuckPerms storage

## World Model

- Lobby is a separate Paper backend.
- Survival is a separate Paper backend.
- Dungeon gameplay is not a Velocity backend.
- Dungeon gameplay world is the internal Paper world `dungeon` inside the survival
  service `/opt/minecraft/server`.
- New World uses the old `/opt/minecraft/dungeon` server folder, but its service name
  is now `newworld`.
- New World plugins must be selected explicitly before enabling the service.
- Do not recreate `dungeon.service`.
- Do not recreate Velocity backend `dungeon`.

## Current Velocity Backends

- `lobby`
- `survival`
- `newworld`

## Auth / Proxy Forwarding

- Velocity `online-mode` is enabled.
- Paper backends use `online-mode=false` because players enter through Velocity.
- Paper Velocity forwarding is enabled on survival and lobby.
- `/opt/minecraft/velocity/forwarding.secret` must match the Velocity secret in both
  Paper `config/paper-global.yml` files.

## Plugin Deployment Rules

- Deploy `LeeSeolDungeon` only to survival:
  `/opt/minecraft/server/plugins/`
- Do not deploy `LeeSeolDungeon` to newworld.
- Deploy lobby-only behavior to `/opt/minecraft/lobby/plugins/`.
- Deploy proxy commands to `/opt/minecraft/velocity/plugins/`.
- Newworld is normally off; do not restart it unless the user explicitly asks.

## Important Gameplay Rules

- LeeSeolCore suppresses default join/quit broadcasts on Paper backends.
- Lobby blocks all Nether/End dimension movement.
- Survival allows Nether movement only between `world` and `world_nether`.
- Survival blocks End access.
- Internal dungeon world `dungeon` blocks both Nether and End access.
- Dimension bypass permission:
  `leeseolcore.dimension.bypass`
- Server menu is blocked inside internal dungeon world unless explicitly bypassed.
- Server menu dungeon bypass permission:
  `leeseoldungeon.menu-bypass`
- Auction is blocked inside internal dungeon world.
- Auction world bypass permission:
  `leeseolauction.world-bypass`

## Normal Start/Stop

Normal active services:

```bash
sudo systemctl start mariadb velocity minecraft lobby
```

Resource-pack host:

```bash
sudo systemctl start resourcepack
```

Normal stop:

```bash
sudo systemctl stop lobby minecraft velocity resourcepack
```

Newworld only when explicitly requested:

```bash
sudo systemctl enable newworld
sudo systemctl start newworld
```

Expected normal state when newworld is paused:

```text
velocity: active
resourcepack: active
minecraft: active
lobby: active
newworld: inactive
```

## Verification Commands

```bash
systemctl is-active velocity minecraft lobby newworld
sudo journalctl -u minecraft -u lobby -u velocity --since "5 minutes ago" -p err --no-pager
sudo ss -ltnp | grep -E '25565|25566|25567|25568|8163'
```

## Current Custom Plugins

- `LeeSeolCore`
- `LeeSeolProxy`
- `LeeSeolEconomy`
  - Deployed to survival and lobby.
  - Shift+F/`/servermenu` opens are bridged to the PixieStudios DeluxeMenus menu when
    configured.
  - Since 2026-06-08, successful server menu opens soft-progress
    `LeeSeolQuestApi#progress(player, "open-gui", "server-menu", 1)` when
    LeeSeolQuest is present. Server-side build/deploy/RCON verification passed;
    real player objective progress still needs in-game verification.
- `LeeSeolAuction`
- `LeeSeolDungeon`
- `LeeSeolLobby`
- `LeeSeolTown`
- `LeeSeolHologram`
- `LeeSeolCombat`
  - Deployed to survival only.
  - Combat-tag logout is immediate death punishment and does not create a corpse NPC.
  - Normal survival logout creates a Citizens corpse clone when `logout-clone.enabled`
    and the world is in `logout-clone.enabled-worlds`.
  - Logout corpse hitboxes are non-persistent.
  - Stale combat hitboxes are removed on plugin enable and on chunk load.
  - PVP rewards are enabled in survival worlds:
    PVP points, player-head trophy drops, same-target cooldown, and optional same
    town/nation reward suppression through PlaceholderAPI/LeeSeolTown.
  - PVP reward data file: `/opt/minecraft/shared/combat/pvp.yml`
- `LeeSeolCleanup`
- `LeeSeolRanks`
  - Deployed to both survival and lobby.
  - Shared rank data file: `/opt/minecraft/shared/ranks/ranks.yml`
  - Provides PlaceholderAPI placeholders such as `%leeseolranks_prefix%` for TAB and
    chat rank display.
  - Rankup requirements now support kills, Vault balance, and playtime minutes via
    `rank-up.requirements`.
  - Provides `/rank progress` and `/rank requirements`.
  - Soft-integrates with `LeeSeolQuestApi#progress(...)` for `rank-up` objective
    progress when a player successfully ranks up.
  - ADMIN and DEV both receive the configured staff admin permission list from
    `plugins/LeeSeolRanks/config.yml` at `staff.admin-permissions`; current default
    is LuckPerms wildcard `*`.
  - Current rank records are intentionally limited to:
    `lee_seol=ADMIN`, `YamiyongO_o=DEV`.
- `LeeSeolQuest`
  - Deployed to both survival and lobby.
  - Shared quest data file: `/opt/minecraft/shared/quests/data.yml`
  - Provides `/quest`, `/tutorial`, `/lsquest` and PlaceholderAPI placeholders under
    `%leeseolquest_*%`.
  - Current baseline quest config contains one tutorial quest, `tutorial_start`.
  - Current objective support includes `open-gui`, `mine-block`, `fish`,
    `kill-player`, `harvest-crop`, `dungeon-enter`, and Citizens NPC click based
    `npc-dialogue`.
  - Exposes Bukkit quest events and `LeeSeolQuestApi#progress(...)` for future
    Crafting/Jobs/Ranks integration.
- `LeeSeolJobs`
  - Deployed to survival only.
  - Shared jobs data file: `/opt/minecraft/shared/jobs/data.yml`
  - Provides `/jobs` and `/lsjobs`.
  - Current support: mining, farming, fishing rewards, Vault payout through
    LeeSeolEconomy, daily limits, cooldowns, and placed-ore abuse guard.
  - Soft-integrates with `LeeSeolQuestApi#progress(...)` for `earn-money` objective
    progress when LeeSeolQuest is present.
- `LeeSeolCrafting`
  - Deployed to survival only.
  - Provides `/craftmenu`, `/forge`, `/process`, `/disassemble`, `/repair`, and
    `/lscrafting`.
  - Current support: config-driven recipes, crafting/processing/disassembly GUI,
    money-based repair confirmation GUI, Vault costs, rank requirements, world
    limits, and soft Quest `craft-item` objective progress.
- `LeeSeolHUD`
  - Deployed to survival only.
  - Provides `/hud` and `/lshud`.
  - Current support: image-based BossBar compass HUD and TAB below-name player
    health placeholder in `❤ current/max` style, with short-lived heal amount
    suffixes such as `+16`.
  - Target-health BossBar is disabled by default because player health is now shown
    through TAB below-name display.
  - Provides PlaceholderAPI placeholders under `%leeseolhud_*%`.
  - Uses TAB below-name objective for `%leeseolhud_healthbar%`.
  - Uses fixed resource-pack font glyphs `U+E340` to `U+E7BF` for 5-degree compass
    images. Each heading is rendered as 16 glyph segments to avoid oversized
    single-glyph rendering failures.

## Resource / Model Sources

- Crates and Stuff Model Pack Update 4 source models are staged at:
  `/opt/minecraft/model-sources/crates_and_stuff/current`
- Original source pack contains Blockbench `.bbmodel` sources:
  `common_crate`, `cosmetic_crate`, `legendary_crate`, `rare_crate`, `vote_crate`,
  `shield1`, and `straw_hat`.
- Crates and Stuff Model Pack Update 4 is currently rolled back and not live.
- A 2026-06-06 safe apply of `crates_and_stuff:common_crate` passed server-side ZIP,
  SHA, and provider checks, but the user reported the same broken texture/model
  symptom in-game. The apply was immediately rolled back.
- ItemsAdder content files for `crates_and_stuff` are intentionally absent from lobby
  and survival.
- `contents/expedition` and Crates-related source content from the failed apply are
  absent after rollback.
- Paper `CustomModelData` mapping uses material `PAPER`, but no Crates mappings are
  live.
- Paper `CustomModelData` mapping uses material `PAPER`:
  - No Crates and Stuff mappings are live.
  - Failed common_crate official-block testing used `12000`:
    `crates_and_stuff:block/common_crate`.
- This was a direct static conversion. Original `.bbmodel` animations were not
  converted into animated ModelEngine/Oraxen entities.
- Rollback backup of the failed Crates state:
  `/opt/minecraft/backups/rollback-before-removing-crates-20260606-011107`
- Last good pre-Crates apply backup used for rollback:
  `/opt/minecraft/backups/itemsadder-official-crates-v2-20260606-003001`
- Current known-good resource-pack restore backup before the latest correction:
  `/opt/minecraft/backups/restore-known-good-resourcepack-20260606-033037`
- Current successful common_crate furniture merge backup:
  `/opt/minecraft/backups/common-crate-furniture-merged-20260606-040258`
- Rollback backup after texture break report:
  `/opt/minecraft/backups/rollback-common-crate-texture-break-20260606-041136`
- Failed official-block common_crate backup used for rollback:
  `/opt/minecraft/backups/itemsadder-common-crate-block-v3-20260606-054931`
- Current successful safe common_crate apply backup:
  `/opt/minecraft/backups/itemsadder-common-crate-safe-20260606-064054`
- Rollback from the failed safe common_crate apply restored this same backup on
  2026-06-06.

## Resource Pack

- A dedicated `resourcepack` systemd service serves the generated pack on port `8163`.
- Public resource pack URL:
  `http://34.64.126.179:8163/generated.zip`
- `LeeSeolProxy` offers the resource pack once from Velocity when a player joins the
  network.
- Lobby and survival Paper `server.properties` resource-pack fields are intentionally
  empty so backend servers do not send separate packs.
- Lobby and survival ItemsAdder `auto_apply.enabled` and self-host are disabled.
- Current resource-pack SHA1:
  `16c2f19d8a7e75a2bce5bdea96eec8affbb24d8a`
- The current generated pack contains BetterRanks images:
  `admin.png`, `dev.png`, `player.png`, `rank_s.png`, `rank_a.png`, `rank_b.png`,
  `rank_c.png`, and `rank_d.png`.
- BetterRanks `player.png` was replaced with the new gray metal `PLAYER` glyph on
  2026-06-07. Remote backup:
  `/opt/minecraft/backups/betterranks-player-20260607-212327`
- The current generated pack contains an expedition TAB title glyph:
  - texture: `assets/expedition/textures/gui/expedition_title.png`
  - texture dimensions: `220x42`; keep single font-glyph logo textures small enough
    for Minecraft font rendering
  - explicit font file: `assets/expedition/font/tab.json`
  - explicit font key: `expedition:tab`
  - default font glyph: `\ue301`
  - current font metrics in `assets/minecraft/font/default.json`:
    `ascent: 36`, `height: 42`
  - current local source image:
    `assets/generated/expedition_logo_sleek_220x42_left32.png`
- The current generated pack replaces vanilla TAB ping icons with transparent
  textures under `assets/minecraft/textures/gui/sprites/icon/`. This hides latency
  bars globally in the client's TAB UI while the pack is applied.
- The current generated pack contains LeeSeolHUD compass glyphs:
  - textures:
    `assets/leeseolhud/textures/gui/compass/compass_000.png` through
    `compass_355.png`
  - default font glyphs are mapped in `assets/minecraft/font/default.json`:
    `U+E340` through `U+E7BF`
  - glyph layout: 72 headings x 16 segments = 1152 chars; source sample glyph cell
    width is `120px`, rendered sample glyph cell width is `30px`, and sample image
    size is `1920x136`
  - compass style: transparent background, clear white/gray coordinate line, 5-degree
    ticks aligned to one baseline, 15-degree labels, intercardinal labels, labels
    without shadow, labels placed with more vertical breathing room, stronger
    edge-only fade, fixed-width segment anchors, a slightly right-shifted center
    marker, and lowered font ascent to reduce top clipping
  - player commands:
    `/hud compass on`, `/hud compass off`, `/compasshud on`, `/compasshud off`
  - WHITE BossBar sprites are transparent in the resource pack to hide the compass
    BossBar fill/background while keeping the title glyph visible:
    `assets/minecraft/textures/gui/sprites/boss_bar/white_background.png`,
    `white_progress.png`
  - generated from local source:
    `assets/generated/leeseolhud_compass/`
- The current generated pack contains PixieStudios GUI assets for the active Shift+F
  DeluxeMenus menu:
  `assets/pixiestudios_gamemenu/textures/pixiestudios_gamemenu1.png` and related
  `pixiestudios_gamemenu` textures.
- The current generated pack contains ItemsAdder DefaultPack 2.0.13 namespaces:
  `iaalchemy`, `iafestivities`, `iageneric`, `iasurvival`, `iawearables`,
  `mcemojis`, `mcicons`, and `twitteremojis`.
- DefaultPack was applied on 2026-06-06 with ZIP protection disabled and a post-build
  texture injection pass. ItemsAdder generated model JSON with `_b_*` and `_*`
  namespace texture references but did not include those textures in `generated.zip`.
  The final pack injects those missing textures from the corresponding
  `plugins/ItemsAdder/contents/**/resourcepack` sources and rewrites `_b_minecraft:*`
  model texture references back to vanilla `minecraft:*`.
- If `/iazip` is run again, the DefaultPack injection pass must be repeated before
  updating Velocity SHA; otherwise many DefaultPack models can render as
  missing-texture purple/black assets.
- A 2026-06-07 attempt to fix held-block, GUI icon, and shield missing-texture
  symptoms by merging private namespace sprites into the item atlas was rolled
  back because it broke already placed block rendering and did not fix held items.
  Do not repeat that atlas-merge approach on the live pack.
- A second 2026-06-07 attempt to split atlas usage by model role was also rolled
  back. It did not fix held items and made shield rendering worse. Do not patch the
  live pack by rewriting generated model JSON/atlas files until a fix is proven in
  an isolated test pack first.
- A 2026-06-07 `iafix` candidate pack was validated and deployed for DefaultPack
  held item/icon/shield atlas errors. It does not rewrite original placed-block
  models. Instead it duplicates only `minecraft:paper` and `minecraft:shield`
  display models into namespace `iafix`, copies required vanilla/custom textures
  into the item atlas, rewrites only the active item definitions to those duplicate
  display models, verifies that `paper` and `shield` no longer mix block/item atlases,
  then updates Velocity SHA. Backup before deployment:
  `/opt/minecraft/backups/itemsadder-iafix-candidate-20260607-062154`.
- The 2026-06-06 Pixie/expedition pack was clean-rebuilt instead of patching the
  protected ItemsAdder ZIP in place. If `/iazip` is run again, verify that the
  expedition logo files still exist in the public `generated.zip` before testing TAB.

## External Plugins In Use

- Paper
- Velocity
- EssentialsX
- EssentialsXChat
- EssentialsXSpawn
- LuckPerms
- Vault
- TAB
- AdvancedEnchantments 9.23.0
- Citizens 2.0.42 build 4186 on survival
- WorldEdit on lobby
- DeluxeMenus 1.14.1-Release on survival and lobby

## UI And Notifications

- Survival TAB layout uses columns 1-3 for `접속 유저 목록` and column 4 for
  `마을원 목록`. Players with `%leeseoltown_has_party%=true` are routed to column
  4 slots `62-80`; players without a party remain in columns 1-3.
- Lobby TAB currently uses the same 4-column survival layout, but TAB's own
  `header-footer` is disabled on lobby. `LeeSeolLobby` sends the lobby TAB
  header/footer directly. The expedition logo glyph `\ue301` is registered in
  `minecraft:default`, matching the BetterRanks image path that is already known to
  render on clients. Lobby TAB currently adds `4` leading blank lines before the logo
  and `2` gap lines after the logo to give the title more vertical room. The lobby
  TAB ping label is `PING : ` in sky blue, and the online footer is beige with a
  `500` player display cap.
- TAB empty-slot latency icons are hidden through the resource pack's transparent
  vanilla ping icon textures, not through TAB config alone.
- LuckPerms `broadcast-received-log-entries` and `log-notify` are disabled on
  survival and lobby to avoid rank-sync messages being sent to admins in-game.
- Shift+F currently opens LeeSeolEconomy's external menu bridge:
  `dm open pixiestudios_gamemenu1 %player%`.
  The active DeluxeMenus files are the 9 PixieStudios `pixiestudios_*.yml` menus on
  both survival and lobby. Their example click commands were patched to real server
  commands such as `/leeseolmenu auction`, `/leeseolmenu shop`, `/leeseolmenu dungeon`,
  `/leeseolmenu move lobby`, `/leeseolmenu move survival`, and
  `/leeseolmenu move newworld`. Buttons for features not implemented yet should show
  a message instead of calling placeholder demo commands.
- Survival PixieStudios `menu_title` values were changed on 2026-06-08 from
  ItemsAdder tag syntax such as `:pixiestudios_gamemenu1:` to direct font glyphs
  `U+F806 + U+E02F..U+E037`. The hosted resource pack already contains these glyphs;
  this avoids survival-side ItemsAdder tag conversion failures where the GUI image is
  not rendered. Do not revert survival menu titles back to tag syntax unless the
  conversion path is verified in-game.
- On 2026-06-06, PixieStudios DeluxeMenus `display_name` and `lore` tooltip text was
  translated from English to Korean on both survival and lobby. The original English
  menu files are backed up at
  `/opt/minecraft/backups/deluxemenus-tooltips-english-20260606-215512`. Mouse/icon
  glyphs and color/format codes were intentionally preserved. `slot-playerhead`
  materials were changed from `head-%player_name%` to `PLAYER_HEAD` to avoid
  DeluxeMenus sending the literal placeholder to Mojang profile lookup.
- On 2026-06-06 the Pixie menu slots were aligned to the expedition plan:
  - Main menu keeps quest slot `38`, shop slot `42`, map slots
    `0,1,9,10,18,19,27,28,36,37`, left-page movement slots `45,46,47`, and
    right-page movement slots `51,52,53`.
  - Main menu placeholder slots for attendance, rankup, pets, nation list, skills,
    and future New World buttons now show "준비 중" messages only.
  - Left menu keeps auction slots `0,1,2,3,9,10,11,12,18,19,20,21,27,28,29,30`;
    home, nation balance, custom enchant, donation, pet, and unused sections are
    disabled with messages.
  - Map menu 1 slots `39,40,41` now move to survival. Map menu future spawn slots
    `4,20,24`, map menu 2 slots `37,31,20,14`, and map menu 3 slots `29,23,3` are
    disabled with messages.
  - Right menu now routes its active panel to `pixiestudios_anarchymenu1`; the
    previous donation/store demo path is no longer opened from Shift+F.
