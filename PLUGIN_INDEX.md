# PLUGIN_INDEX.md

Use this file as the current plugin structure and contract map. It exists to
prevent broad code reads, cross-domain edits, and accidental contract drift. Keep
history, failed attempts, and date-based notes out of this file.

## Work Scope Template

State scope before feature work:

```text
Work:
Primary:
Allowed consumers:
Bridge/config:
Affected contracts:
Not included:
Verify:
```

- Primary owns the data or behavior.
- Allowed consumers may only display or integrate that same domain.
- Bridge/config covers external settings such as TAB, PlaceholderAPI, LuckPerms,
  Vault, BetterHUD, BetterRanks, ItemsAdder, WorldGuard, WorldEdit, Citizens, or
  BlueMap.
- If a contract changes, verify the affected consumers/bridges too.

## Contract Rules

- Treat `Integration Map` as the fixed shared-contract owner map. The `Primary`
  plugin owns the behavior/data; consumers may only display or integrate it.
- Contract changes include commands, permissions, placeholders, APIs/events,
  plugin-message channels, shared config/data keys, GUI/display surfaces, deploy
  targets, and external bridge expectations.
- If a shared surface changes, update both the owning plugin row and the matching
  integration/smoke-test row before finishing the task.
- Prefer plugin APIs, Bukkit events, placeholders, commands, or documented shared
  files over reading another plugin's private storage shape directly.
- Run only the matching `Contract Smoke Tests` row for the changed surface. If no
  row exists, add one before treating the new surface as stable.
- Handle `LeeSeolCore`, `LeeSeolTown`, and `LeeSeolProxy` contract changes
  sequentially because they own high-risk integration surfaces.

## Integration Map

These rows are the current contract ownership map, not a roadmap. Add planned
features only after code/config implements the shared surface.

| Domain | Primary | Allowed consumers | Bridge/config |
| --- | --- | --- | --- |
| Rank | `LeeSeolRanks` | `LeeSeolTown` scoreboard/chat, `LeeSeolQuest` rank-up progress, TAB/BetterRanks displays | PlaceholderAPI `%leeseolranks_*%`, LuckPerms command sync, Vault, BetterRanks `%img_*%` |
| Economy | `LeeSeolEconomy` | `LeeSeolTown` claims/upkeep/treasury, `LeeSeolAuction`, `LeeSeolQuest` money rewards via `/won give`, `LeeSeolJobs`/`LeeSeolCrafting` via Vault, survival menus | Vault provider, BungeeCord channel, shared balances/market/ledger files |
| Content locations / neutral zones | `LeeSeolCore` | `LeeSeolTown` neutral-zone source, WorldGuard/BlueMap content displays | `/content`, `contents.yml`, WorldEdit, WorldGuard, BlueMap |
| Party / nation / claim / war | `LeeSeolTown` | scoreboard/chat prefixes, territory actionbars, BlueMap claim markers, `LeeSeolCombat` same-affiliation PVP filter, TAB/PAPI displays | Vault, PlaceholderAPI `%leeseoltown_*%`, WorldGuard, BlueMap, ItemsAdder, WorldEdit, shared Town data |
| Quest progress | `LeeSeolQuest` | `LeeSeolRanks`, `LeeSeolJobs`, `LeeSeolCrafting`, `LeeSeolEconomy` server menu | `LeeSeolQuestApi`, quest Bukkit events, PlaceholderAPI `%leeseolquest_*%`, `/won give` reward path |
| Lobby / queue movement | `LeeSeolProxy` | `LeeSeolLobby` limbo handling, Paper menu move commands | Velocity `network.properties`/`queue.properties`, `leeseol:queue`, BungeeCord `Connect` |
| Resource pack | `LeeSeolProxy` | ItemsAdder output and visual placeholder assets | Velocity pack offer, `resourcepack.properties`, `resourcepack` service |
| Dungeon | `LeeSeolDungeon` | `LeeSeolQuest` `dungeon-enter`, `LeeSeolEconomy` menu bridge | survival-internal `dungeon` world, shared dungeon inventory/return files |
| Combat / PVP | `LeeSeolCombat` | `LeeSeolQuest` `kill-player`, `LeeSeolTown` placeholders for same-affiliation reward checks | Citizens, PlaceholderAPI, `/opt/minecraft/shared/combat/pvp.yml` |
| Cleanup alerts | `LeeSeolCleanup` | TAB/actionbar/external displays when they consume placeholders | PlaceholderAPI `%leeseolcleanup_*%`, direct actionbar countdown |
| Jobs / activity | `LeeSeolJobs` | `LeeSeolQuest` `earn-money`, economy payout, rank multipliers | Vault, `LeeSeolQuestApi`, shared jobs data |
| Crafting / repair | `LeeSeolCrafting` | `LeeSeolQuest` `craft-item`, economy fees, rank requirements | Vault, `LeeSeolQuestApi`, recipe PDC `recipe_id` |
| Custom enchanting | `LeeSeolEnchanting` | item lore/display consumers only when explicitly scoped | AdvancedEnchantments |
| Hologram | `LeeSeolHologram` | display-only consumers only when explicitly scoped | display entities, local hologram data |

## Contract Smoke Tests

These are the required smoke gates for changed shared surfaces. Run only the rows
touched by the change.

| Contract | Shared surface | Smoke test |
| --- | --- | --- |
| Rank | `%leeseolranks_rank%`, `%leeseolranks_rank_lower%`, `%leeseolranks_image%`, `%leeseolranks_prefix%`, `%leeseolranks_kills%`, `%leeseolranks_next%`; `leeseolranks.rank.*`; BetterRanks image placeholders | `/leeseolrank status`, `/rank requirements`, `/rankup`, PAPI parse, LuckPerms permission check, TAB/BetterRanks/Town display |
| Economy | Vault provider, `/won give`, shared balances/market/ledger files, survival menu moves | `/won`, `/pay`, `/won report`, `/market status`, `/servermenu`, dependent fee/reward path |
| Content locations / neutral zones | `/content` registry, `contents.yml`, WorldGuard content regions, BlueMap markers, Town neutral-zone consumer | `/content list`, `/content tp`, `/party diag`, neutral-zone protection/claim behavior |
| Party / nation / claim / war | Town data, nation colors, `%leeseoltown_affiliation%`, `%leeseoltown_rank%`, `%leeseoltown_has_party%`, `%leeseoltown_has_town%`, `%leeseoltown_town%`, `%leeseoltown_party%`, `%leeseoltown_nation%`, `%leeseoltown_nation_color%`, `%leeseoltown_nation_color_hex%`, `%leeseoltown_nation_type%`, `beacon-claim`, treasury/upkeep/debt/function-suspension fields, invasion/total `WarMode`, territory actionbar, scoreboard/chat | `/party me`, `/party claimprice`, `/party nation claimprice`, `/party nation deposit`, `/party nation upkeep`, `/party war declare <nation> invasion`, `/party diag`, PAPI parse, scoreboard/chat/territory display |
| Town structures | ItemsAdder block ids, PDC `structure_core`/`structure_id`, `structures/*.schem`, nation core first-claim flow, WorldEdit paste/undo | structure core select/place, `nation_core`/`outpost`/`supply_depot`, `/party structure undo`, claim protection around core chunk |
| Quest progress | `LeeSeolQuestApi`, objective strings, daily/weekly reset keys, quest events, `%leeseolquest_*%`, `/won give` rewards | `/quest progress`, `/lsquest objective`, PAPI parse, rank/jobs/crafting/server-menu progress hooks |
| Dungeon | portal triggers, return locations, inventory sync, loot spots, dungeon protection | `/dungeon enter`, `/dungeon exit`, `/dungeon portal list`, `/dungeon chest list`, portal return, loot roll, protection |
| Queue / lobby movement | backend names, `leeseol:queue` actions, `queue.properties`, `network.properties` | `/survival`, queue/limbo enter, `/lobby`, queue leave, kicked fallback |
| Resource pack | Velocity offer URL/hash, hosted pack file | Velocity plugin load, pack offer on join, resourcepack service response |
| Combat / PVP | combat tag, Citizens logout clone, PVP points, Town same-affiliation placeholders | `/combat status`, `/combat diag`, logout clone death, same town/nation reward block |
| Cleanup alerts | cleanup timer, `%leeseolcleanup_next%`, `%leeseolcleanup_seconds%`, actionbar warning | `/cleanup status`, `/cleanup run`, PAPI parse, countdown/actionbar warning |
| Jobs / crafting / enchanting | Vault payout/fee, quest progress hooks, rank requirements, AdvancedEnchantments bridge | `/activity`, `/lsjobs status`, `/craftmenu`, `/lscrafting status`, `/lsenchanting status` |

## Custom Plugins

| Plugin | Target | Current responsibility / commands | Key files and contracts | Minimal verify |
| --- | --- | --- | --- | --- |
| `LeeSeolCore` | survival + lobby | Common Paper core: server info, survival spawn/return, portal triggers, launch pads, Shift+F server menu, Citizens server NPCs, `/content`; commands `/serverinfo`, `/survivalspawn`, `/lscore`, `/leeseolcore`, `/content` | `LeeSeolCorePlugin.java`, `command/`, `content/`, `portal/`, `launchpad/`, `menu/`, `servernpc/`, `spawn/`, `listener/`, `config.yml`, `plugin.yml`; contracts: `contents.yml`, BungeeCord channel, WorldGuard/BlueMap content regions | `version LeeSeolCore`, `/content list`, `/survivalspawn`, `/leeseolcore servernpc list`; verify portal/launchpad/menu only if touched |
| `LeeSeolTown` | survival | Party/town membership, nation colors, nation core-gated claims, treasury/upkeep/debt suspension, invasion/total war, chat channels, territory actionbar/scoreboard, neutral zones, structure core placement/undo; commands `/town`, `/party`, `/village`, `/towny`, `/tc`, `/pc`, `/nc` | `LeeSeolTownPlugin.java`, `command/`, `command/NationClaimCommand.java`, `service/TownService.java`, `service/TerritoryTransition.java`, `storage/TownStore.java`, `listener/`, `hook/`, `diagnostic/`, `scoreboard/`, `structure/`, `config.yml`, `plugin.yml`; contracts: `%leeseoltown_*%`, shared town data keys `towns`/`nations`/`wars`, `beacon-claim`, `upkeep.*`, `debt.*`, `functions-suspended`, Core `contents.yml`, ItemsAdder structure ids, WorldGuard/BlueMap, Vault | `version LeeSeolTown`, `/party me`, `/party claimprice`, `/party nation claimprice`, `/party nation claim`, `/party nation deposit`, `/party nation upkeep`, `/party war declare <nation> invasion`, `/party diag`, PAPI parse, scoreboard/chat/territory/claim smoke |
| `LeeSeolQuest` | survival | Quests/tutorial, daily/weekly reset progress, GUI objectives, money reward dispatch; commands `/quest`, `/quests`, `/tutorial`, `/lsquest` | `LeeSeolQuestPlugin.java`, `api/`, `command/`, `listener/`, `gui/`, `service/`, `storage/`, `hook/`, `config.yml`, `plugin.yml`; contracts: `LeeSeolQuestApi`, quest events, `%leeseolquest_*%`, `/won give` | `version LeeSeolQuest`, `/quest`, `/quest progress`, `/lsquest reload`, PAPI parse, API consumer smoke |
| `LeeSeolDungeon` | survival | Internal survival dungeon world, inventory sync, return locations, portal triggers, loot chests, dungeon protection; command `/dungeon` | `LeeSeolDungeonPlugin.java`, `command/`, `portal/`, `world/`, `loot/`, `protection/`, `inventory/`, `returnloc/`, `config.yml`, `plugin.yml`; contracts: shared dungeon inventory/return files, `dungeon` world | `version LeeSeolDungeon`, `/dungeon enter`, `/dungeon exit`, `/dungeon portal list`, `/dungeon chest list`, portal/loot/protection smoke |
| `LeeSeolCombat` | survival | Combat tag, combat logout death/clone, pending deaths, Citizens clone hitbox, PVP reward points/trophies; command `/leeseolcombat` alias `/combat` | `LeeSeolCombatPlugin.java`, `command/`, `listener/`, `tag/`, `clone/`, `reward/`, `storage/`, `config.yml`, `plugin.yml`; contracts: Citizens, `%leeseoltown_town%`, `%leeseoltown_nation%`, shared `pvp.yml` | `version LeeSeolCombat`, `/combat status`, `/combat diag`, tag/logout clone, PVP reward filter |
| `LeeSeolProxy` | velocity | Velocity server list, lobby/survival movement, maintenance/fallback, queue/limbo, resource-pack offer; commands `/servers`, `/lobby`, `/survival` | `LeeSeolProxyPlugin.java`, `command/`, `queue/`, `resourcepack/`, `velocity-plugin.json`; contracts: `leeseol:queue`, `network.properties`, `queue.properties`, `resourcepack.properties` | Velocity plugin load, `/servers`, `/survival`, `/lobby`, resource-pack offer, queue/limbo roundtrip |
| `LeeSeolEconomy` | survival | Won economy, Vault provider, shops, market, NPC shops, server menu, ledger; commands `/won`, `/pay`, `/shop`, `/wonnpc`, `/market`, `/servermenu`, `/leeseolmenu` | `LeeSeolEconomyPlugin.java`, `command/`, `storage/`, `shop/`, `market/`, `npc/`, `servermenu/`, `vault/`, `ledger/`, `config.yml`, `plugin.yml`; contracts: Vault, shared balances/market/ledger, `/won give`, BungeeCord channel | `version LeeSeolEconomy`, `/won`, `/pay`, `/won report`, `/market status`, `/shop`, `/servermenu`, Vault provider check |
| `LeeSeolAuction` | survival | Player auction GUI, bid/increment/end flow, blocked worlds; command `/auction` aliases `/ah`, `/auc` | `LeeSeolAuctionPlugin.java`, `command/`, `gui/`, `service/`, `storage/`, `vault/`, `config.yml`, `plugin.yml`; contracts: Vault economy, auction storage | `version LeeSeolAuction`, `/auction`, `/auction submit`, admin open/end flow, bid economy smoke |
| `LeeSeolCleanup` | survival | Dropped-item cleanup timer, broadcasts, actionbar countdown, cleanup placeholders; command `/leeseolcleanup` aliases `/cleanup`, `/itemcleanup` | `LeeSeolCleanupPlugin.java`, `command/`, `service/`, `hook/`, `config.yml`, `plugin.yml`; contracts: `%leeseolcleanup_next%`, `%leeseolcleanup_seconds%` | `version LeeSeolCleanup`, `/cleanup status`, `/cleanup run`, PAPI parse, warning/actionbar smoke |
| `LeeSeolRanks` | survival | Rank model, rank-up requirements, kill/playtime tracking, Vault money requirement, LuckPerms sync, PAPI/BetterRanks visual placeholders; commands `/rank`, `/ranks`, `/rankup`, `/leeseolrank`, `/lsrank` | `LeeSeolRanksPlugin.java`, `command/`, `hook/`, `listener/`, `model/`, `permission/`, `requirement/`, `storage/`, `config.yml`, `plugin.yml`; contracts: `%leeseolranks_*%`, `leeseolranks.rank.*`, LuckPerms, Vault, Quest rank-up hook | `version LeeSeolRanks`, `/rank`, `/rank requirements`, `/rankup`, `/leeseolrank status`, PAPI parse, LuckPerms/TAB display |
| `LeeSeolJobs` | survival | Mining/farming/fishing/exploration activity rewards, cooldowns, block history, daily limits; commands `/jobs`, `/activity`, `/activities`, `/expedition`, `/explore`, `/lsjobs` | `LeeSeolJobsPlugin.java`, `command/`, `listener/`, `service/`, `storage/`, `config.yml`, `plugin.yml`; contracts: Vault payout, `LeeSeolQuestApi` `earn-money`, shared jobs data | `version LeeSeolJobs`, `/activity`, `/lsjobs status`, one reward source, Vault payout, quest progress hook |
| `LeeSeolCrafting` | survival | Custom recipe menus, forge/process/disassemble, repair, failure/cost/rank checks; commands `/craftmenu`, `/forge`, `/process`, `/disassemble`, `/repair`, `/lscrafting` | `LeeSeolCraftingPlugin.java`, `command/`, `gui/`, `service/`, `model/`, `config.yml`, `plugin.yml`; contracts: Vault fee, `LeeSeolQuestApi` `craft-item`, recipe PDC `recipe_id` | `version LeeSeolCrafting`, `/craftmenu`, `/forge`, `/process`, `/disassemble`, `/repair`, `/lscrafting status` |
| `LeeSeolEnchanting` | survival | Custom enchanting-table rolls, bookshelf scaling, AdvancedEnchantments bridge, lore descriptions; command `/lsenchanting` aliases `/leeseolenchanting`, `/customenchanting` | `LeeSeolEnchantingPlugin.java`, `command/`, `config/`, `listener/`, `service/`, `config.yml`, `plugin.yml`; contracts: AdvancedEnchantments reflective API, item lore | `version LeeSeolEnchanting`, `/lsenchanting status`, `/lsenchanting bookshelves`, table roll, lore rewrite |
| `LeeSeolLobby` | lobby | Movement-only lobby rules, spawn/void return, tab header, limbo world and queue plugin messages; command `/lobbysetspawn` | `LeeSeolLobbyPlugin.java`, `config.yml`, `plugin.yml`; contracts: `leeseol:queue`, lobby spawn/rules/limbo config | `version LeeSeolLobby`, `/lobbysetspawn`, spawn/void/rules, tab header, queue limbo roundtrip |
| `LeeSeolHologram` | survival + lobby | Display-entity holograms with editable lines and spacing; command `/holo` aliases `/hologram`, `/lholo` | `LeeSeolHologramPlugin.java`, `command/`, `model/`, `service/`, `storage/`, `config.yml`, `plugin.yml`; contracts: local hologram data file only | `version LeeSeolHologram`, `/holo list`, create/addline/move/delete display smoke |
| `LeeSeolHUD` | disabled | Legacy HUD remains inactive. Descriptor still declares `/hud`, `/compasshud`, `/lshud`, but it is not an active deploy target | `LeeSeolHUD/`, `plugin.yml`, `config.yml`; old BossBar/PAPI HUD code only | Verify inactive/absent only; do not deploy unless explicitly reactivated |

## Deploy Targets

- Survival Paper `/opt/minecraft/server/plugins`: `LeeSeolCore`,
  `LeeSeolHologram`, `LeeSeolEconomy`, `LeeSeolAuction`, `LeeSeolDungeon`,
  `LeeSeolCombat`, `LeeSeolCleanup`, `LeeSeolRanks`, `LeeSeolQuest`,
  `LeeSeolJobs`, `LeeSeolCrafting`, `LeeSeolEnchanting`, `LeeSeolTown`.
- Lobby Paper `/opt/minecraft/lobby/plugins`: `LeeSeolCore`, `LeeSeolLobby`,
  `LeeSeolHologram`.
- Velocity `/opt/minecraft/velocity/plugins`: `LeeSeolProxy`.
- Disabled legacy: `LeeSeolHUD`.
- Do not deploy survival/lobby plugin sets into `newworld`.

## External Hotspots

- MariaDB-backed LuckPerms: rank sync and permissions display.
- PlaceholderAPI: ranks, town, quest, cleanup, HUD legacy.
- TAB/BetterRanks/BetterHUD configs: consume visual placeholders and resource-pack
  glyphs; edit only when the display contract changes.
- ItemsAdder: generated resource pack and Town structure block ids.
- WorldGuard/WorldEdit: Core content regions and Town structures/neutral zones.
- BlueMap: Core content markers, Town neutral/nation claim markers.
- Citizens: Core server NPCs, Town integrations, Combat logout clones.
- AdvancedEnchantments: `LeeSeolEnchanting` custom enchant bridge.
- Shared files under `/opt/minecraft/shared`: economy, ranks, quests, jobs,
  dungeon inventory/returns, town data/neutral zones, combat PVP data.
- Resource pack host: service `resourcepack`, port `8163`; network pack offer is
  owned by Velocity/`LeeSeolProxy`.

## Notes

- `TODO.md` is for agreed current-goal deferrals only. It is not a source of truth
  for this structure map.
- Do not add development logs, old reports, or planned features here unless code or
  active config already implements them.
