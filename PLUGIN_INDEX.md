# PLUGIN_INDEX.md

Read this before opening broad code. It maps each custom plugin to the smallest useful
context for normal work.

## How To Use

- For a plugin task, read only this file, `SERVER_STATE.md`, and the target plugin.
- Do not inspect unrelated plugins unless the dependency column says to.
- Do not read old logs first. Verify with recent logs after the change.
- Update this file only when plugin ownership, deployment target, or shared state
  changes.
- This plugin-development chat must not edit ItemsAdder, `generated.zip`, TAB logo
  glyphs, BetterRanks images, or Velocity resource-pack SHA values. Those belong to
  the separate GUI/resource-pack part. The 2026-06-04 LeeSeolHUD compass task was a
  user-approved one-time exception and should not be treated as a general rule.
- `LeeSeolStations` and custom furniture click integrations are currently assigned to
  the GUI/resource-pack part. Plugin work may expose command/API endpoints for that
  part to call, but should not directly implement ItemsAdder furniture handling here.

## Custom Plugins

| Plugin | Target | Role | Minimal Files To Read | Depends On / Touches | Deploy / Verify |
| --- | --- | --- | --- | --- | --- |
| `LeeSeolCore` | survival, lobby | Core commands, launch pads, portal triggers, dimension gates, Velocity movement | `src/main/java/.../LeeSeolCorePlugin.java`, relevant `command/`, `listener/`, `manager/`, `src/main/resources/config.yml`, `plugin.yml` | Velocity channel, Paper worlds | `deploy-leeseolcore.sh`; restart affected Paper server |
| `LeeSeolProxy` | Velocity | `/lobby`, `/survival`, `/servers` proxy movement, one-time network resource-pack offer | `src/main/java/.../LeeSeolProxyPlugin.java`, `command/`, `velocity-plugin.json` | `velocity.toml` backend names, `resourcepack` service URL/SHA1 | `deploy-leeseolproxy.sh`; restart `velocity` |
| `LeeSeolEconomy` | survival, lobby | Won economy, Vault, shop UI, NPC helpers, Shift+F menu, DeluxeMenus bridge `/leeseolmenu`, Quest `open-gui` soft hook for `server-menu` opens | `LeeSeolEconomyPlugin.java`, `command/`, `listener/`, `gui/`, `servermenu/`, `config.yml`, `plugin.yml` | Vault, LeeSeolAuction, LeeSeolDungeon world blocks, optional DeluxeMenus command calls, soft LeeSeolQuest API reflection | `deploy-leeseoleconomy.sh`; restart affected Paper server |
| `LeeSeolAuction` | survival, lobby | Admin-opened auction, user submissions, bidding GUI | `LeeSeolAuctionPlugin.java`, `command/`, `listener/`, `gui/`, `service/`, `config.yml`, `plugin.yml` | Vault, LeeSeolEconomy, dungeon-world restrictions | `deploy-leeseolauction.sh`; restart affected Paper server |
| `LeeSeolDungeon` | survival only | Internal `dungeon` world, dungeon portals, block protection, loot chests | `LeeSeolDungeonPlugin.java`, `command/`, `listener/`, `service/`, `config.yml`, `plugin.yml` | LeeSeolEconomy server menu, LeeSeolAuction world block | `deploy-leeseoldungeon.sh`; restart `minecraft` only |
| `LeeSeolLobby` | lobby only | Lobby spawn/protection rules | `LeeSeolLobbyPlugin.java`, listeners, `config.yml`, `plugin.yml` | Lobby world config | `deploy-leeseollobby.sh`; restart `lobby` |
| `LeeSeolTown` | survival, lobby | Villages, nations, claims, affiliation prefixes, chat formatting, `%leeseoltown_has_party%` TAB routing placeholder | `LeeSeolTownPlugin.java`, `service/TownService.java`, `command/`, `listener/`, `hook/`, `config.yml`, `plugin.yml` | TAB, PlaceholderAPI, LeeSeolRanks rank prefix | Prefer jar-only deploy for chat/TAB fixes; restart affected Paper server |
| `LeeSeolHologram` | survival, lobby | In-game RGB hologram displays | `LeeSeolHologramPlugin.java`, `command/`, `listener/`, `service/`, `config.yml`, `plugin.yml` | Display entities | `deploy-leeseolhologram.sh`; restart affected Paper server |
| `LeeSeolCombat` | survival only | Combat tags, combat-logout death punishment, normal logout Citizens corpse clone, corpse drops, PVP points, player-head trophy rewards | `LeeSeolCombatPlugin.java`, `listener/SessionListener.java`, `listener/PvpRewardListener.java`, `manager/CombatCloneManager.java`, `service/PvpRewardService.java`, `storage/PvpPointStore.java`, `config.yml`, `plugin.yml` | Citizens, ProtocolLib, PlaceholderAPI/LeeSeolTown optional same-affiliation checks, survival inventory | `deploy-leeseolcombat.sh`; restart `minecraft` only; verify `version LeeSeolCombat`, `version Citizens`, `combat status` |
| `LeeSeolCleanup` | survival only | Dropped item cleanup timer | `LeeSeolCleanupPlugin.java`, `listener/`, `service/`, `config.yml`, `plugin.yml` | TAB footer timer expectations | `deploy-leeseolcleanup.sh`; restart `minecraft` only |
| `LeeSeolRanks` | survival, lobby | Shared rank data, one-rank permissions, rankup requirements, PlaceholderAPI rank display, ADMIN/DEV staff permission sync | `LeeSeolRanksPlugin.java`, `model/Rank.java`, `service/RankRequirementService.java`, `storage/`, `command/`, `hook/`, `config.yml`, `plugin.yml` | LuckPerms, Vault/LeeSeolEconomy balance checks, LeeSeolQuest `rank-up` hook, TAB, BetterRanks font images, LeeSeolTown chat | restart `minecraft` and `lobby`; verify `LeeSeolRanks enabled`, `rank requirements`, `leeseolrank status` |
| `LeeSeolQuest` | survival, lobby | Tutorial and quest progression, quest GUI, shared quest data, PlaceholderAPI quest tracker placeholders, Bukkit quest events, lightweight external progress API | `LeeSeolQuestPlugin.java`, `service/QuestService.java`, `api/LeeSeolQuestApi.java`, `event/`, `storage/QuestStore.java`, `command/`, `listener/`, `gui/`, `config.yml`, `plugin.yml` | PlaceholderAPI, Vault command rewards through `won give`, Citizens NPC click metadata, future Crafting/Jobs/Ranks hooks | restart `minecraft` and `lobby`; verify `LeeSeolQuest enabled`, `Done`, and `lsquest reload` |
| `LeeSeolJobs` | survival only | Mining, farming, and fishing income loop with Vault payouts, daily limits, cooldowns, and placed-ore abuse guard | `LeeSeolJobsPlugin.java`, `service/`, `storage/JobsStore.java`, `listener/`, `command/`, `config.yml`, `plugin.yml` | Vault/LeeSeolEconomy, soft LeeSeolQuest API reflection, LeeSeolRanks rank permissions | restart `minecraft`; verify `LeeSeolJobs enabled`, `Done`, `version LeeSeolJobs`, `lsjobs reload` |
| `LeeSeolCrafting` | survival only | Config-driven crafting, processing, disassembly, money-based repair GUI, and Quest `craft-item` hook | `LeeSeolCraftingPlugin.java`, `service/`, `gui/`, `command/`, `config.yml`, `plugin.yml` | Vault/LeeSeolEconomy, soft LeeSeolQuest API reflection, LeeSeolRanks rank permissions | restart `minecraft`; verify `LeeSeolCrafting enabled`, `Done`, and `/lscrafting reload` when RCON/in-game access is available |
| `LeeSeolHUD` | survival only | Image-based BossBar compass, HUD toggles, direct `/compasshud <on\|off>` command, and TAB below-name heart health display with fading heal suffixes | `LeeSeolHudPlugin.java`, `service/`, `listener/`, `hook/HudPlaceholderExpansion.java`, `command/`, `config.yml`, `plugin.yml` | PlaceholderAPI, TAB below-name objective, one-time ItemsAdder/resource-pack compass glyphs `U+E340`-`U+E7BF` and transparent WHITE BossBar sprites | restart `minecraft`; verify `LeeSeolHUD enabled`, `TAB enabled`, `Done`, `version LeeSeolHUD`, `lshud status`, resource-pack SHA, and in-game compass/health/heal display |

## External Config Hotspots

| Area | Server Path | Notes |
| --- | --- | --- |
| TAB survival | `/opt/minecraft/server/plugins/TAB/` | Uses `%leeseolranks_prefix%`, `%leeseoltown_affiliation%`, and `%leeseoltown_has_party%`; party players route to column 4 slots `62-80`. |
| TAB lobby | `/opt/minecraft/lobby/plugins/TAB/` | Mirrors survival party routing where requested. |
| ItemsAdder lobby | `/opt/minecraft/lobby/plugins/ItemsAdder/` | Generates pack content only; auto-apply/self-host should stay disabled. |
| ItemsAdder survival | `/opt/minecraft/server/plugins/ItemsAdder/` | Uses the already-offered shared pack; auto-apply/self-host should stay disabled. |
| Resource pack host | `/opt/minecraft/lobby/plugins/ItemsAdder/output/` | Served by `resourcepack.service` on port `8163`; Velocity offers this URL once. |
| DeluxeMenus survival/lobby | `/opt/minecraft/*/plugins/DeluxeMenus/` | Current Shift+F visual menu is PixieStudios `gui_menus/pixiestudios_gamemenu1.yml` plus the related 9 Pixie menus; patched clicks call `/leeseolmenu` for auction, shop, dungeon, and server movement. Survival menu titles intentionally use direct glyphs `U+F806 + U+E02F..U+E037` instead of `:pixiestudios_*:` tags because survival-side tag conversion failed to render the GUI image. |
| Shared ranks | `/opt/minecraft/shared/ranks/ranks.yml` | Current expected records: `lee_seol=ADMIN`, `YamiyongO_o=DEV`. |
| Shared quests | `/opt/minecraft/shared/quests/data.yml` | Shared lobby/survival quest progress for `LeeSeolQuest`. |
| Shared jobs | `/opt/minecraft/shared/jobs/data.yml` | Survival-only Jobs statistics and daily reward totals. |
| LuckPerms | MariaDB-backed | Avoid direct DB edits; prefer console commands or plugin sync. |

## Minimal Verification By Task

- Jar change: build, descriptor check, deploy target jar, restart affected service,
  check recent errors.
- Config change: backup, edit exact config, restart or plugin reload, check recent
  errors and the exact changed value.
- Resource pack change: regenerate/copy `generated.zip`, update
  `/opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties` SHA1,
  restart `resourcepack` and `velocity`, GET-download it, verify required assets
  exist in zip, then join server.
- Rank/TAB change: verify shared rank file, LuckPerms sync log, TAB placeholder output
  in-game.

## Current Plugin-Part Priority

1. Balance pass first: use `LeeSeolEconomy`, `LeeSeolJobs`, `LeeSeolCrafting`,
   `LeeSeolRanks`, `LeeSeolCombat`, and AdvancedEnchantments data to establish a
   survival economy/progression/PvP baseline before broad feature or design work.
2. Measure live player loops needed for balance: Jobs mining/farming/fishing income,
   Crafting material and money consumption, Rank progress requirements, Quest reward
   impact, and Combat PVP rewards.
3. Decide the first AdvancedEnchantments safety patch from
   `ADVANCED_ENCHANTMENTS_BALANCE_REVIEW.md` before opening serious PvP or adding new
   high-value loot sources.
4. Tune economy/progression in this order unless the user changes direction:
   Jobs income and limits, Crafting costs and repair, Rank requirements, Quest
   rewards, then auction/shop value assumptions.
5. Keep player-online verification active, but treat it as balance measurement rather
   than the main goal: Quest GUI/hooks, Jobs payouts, Crafting flows, Ranks rank-up,
   HUD display, and Combat rewards still need real-player checks.
