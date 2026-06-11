# TODO.md

Deferred work. Keep this list short and move completed items out.

## Current Operating Direction

This main operations chat should keep the server direction aligned in this order:

1. Server features / plugins: make the operating pass guide the player's next action.
2. Additional items / content: define item sources, uses, sinks, and tiers.
3. Balance: tune economy, progression, PvP risk, rank requirements, item value, and
   reward loops after the core pass exists.
4. Design / GUI / resource pack: polish after the gameplay direction is stable, unless
   a visual issue blocks testing.

Current activity-system decision:

- Do not turn `LeeSeolJobs` into a fixed class/job-selection system.
- Treat it as an open-world activity reward layer: mining, farming, fishing, and
  exploration should reward what players naturally do in the world.
- Future progression should prefer collections, discoveries, region goals, and
  optional mastery over permanent job identity.

Highest future gameplay priority:

- When this chat returns to gameplay planning after the current visual/ItemsAdder
  issue, continue the activity/open-world direction first.
- Next step should be player-online verification and tuning of `/activity`, biome
  exploration rewards, Quest `earn-money` progress, and the operating pass flow before
  adding unrelated large systems.

## Plugin Pass First

Current priority: make `LeeSeolQuest` the operating pass that ties server systems
together.

- `LEESEOLQUEST_OPERATION_PASS_VERIFICATION.md` exists.
- Operating pass 0.1 was implemented and deployed on 2026-06-11.
- Current deployed pass quests:
  - `daily_check_in`
  - `daily_jobs_income`
  - `daily_crafting_processing`
  - `daily_fishing`
  - `weekly_survival_routine`
- Next plugin-pass work:
  - Verify the pass with an online player.
  - Add Dungeon and Combat pass quests after those systems are player-tested.
  - Add pass categories or a richer GUI if the current 6-entry quest GUI becomes too
    small.

## Balance Baseline

Keep the balance baseline as a guardrail while the plugin and item passes are built.

- Baseline reference: `BALANCE_BASELINE.md`.
- Economy loop:
  - Measure `LeeSeolJobs` mining, farming, fishing, and exploration income with a
    real player.
  - Review daily limits, cooldowns, and shop/NPC shop prices against expected play
    time.
  - Confirm auction flow does not create money or bypass item-value assumptions.
- Progression loop:
  - Review `LeeSeolRanks` rank-up requirements against Jobs income, combat rewards,
    and expected playtime.
  - Review `LeeSeolCrafting` costs, processing value, disassembly value, and repair
    costs against vanilla anvil repair.
  - Decide which Quest rewards should be progression helpers instead of major money
    sources.
- PvP / combat loop:
  - First AdvancedEnchantments PvP safety patch was applied on 2026-06-11 before
    opening serious PvP.
  - Verify `LeeSeolCombat` PVP points and trophy drops with two players.
  - Keep same-affiliation reward suppression enabled unless nation-war rules require
    a different design.
- Item and loot value:
  - Do not add major new custom items until the base economy and rank progression are
    measured.
  - Dungeon loot tables should be configured after item value tiers are decided.
  - AdvancedEnchantments EXP-book access should be treated as part of item/value
    inflation, not only as a combat feature.

Next balance decisions:

1. Define target income ranges for early, mid, and late survival play.
2. Define rank-up cost/time targets for the current rank ladder.
3. Review whether the first AdvancedEnchantments PvP safety patch is enough after
   player testing.
4. Decide whether Jobs/Crafting/Ranks should be tuned first or whether PvP-risk
   enchants should be restricted first.

## Infrastructure

- Back up VM server files to the PC D drive while preserving server folder structure.
- Set up a more visual remote editing workflow, preferably VS Code Remote SSH.
- Later: domain setup for a custom server address.
- Later: dynamic MOTD by time, maintenance status, server state, or events.

## New World

- New World is paused.
- `newworld.service` should remain disabled/inactive until the user asks to resume New
  World development.
- Later: add a custom map and datapack to New World.
- Later: design New World rules separately from survival and dungeon.

## Dungeon

- Create in-game dungeon entry portal in survival using `LeeSeolDungeon`.
- Create in-game dungeon exit portal using target `return`.
- Configure dungeon loot chest spots and loot tables through GUI.
- Add future dungeon restrictions as explicit permissions/config entries.

## Economy And Shops

- Polish shop UI.
- Add more advanced in-game shop editing.
- Continue NPC shop work.
- Later: full-body/player-skinned NPC implementation.

## Auction

- Continue GUI polish only when requested.
- Confirm auction workflow with real player testing:
  item submit, admin selection, bidding, admin close, payout.

## Combat

- `LeeSeolCombat` corpse behavior has been restored to the intended split:
  combat-tag logout is immediate death punishment, normal survival logout creates a
  Citizens corpse clone.
- `LEESEOLCOMBAT_CORPSE_DIAGNOSIS.md` and
  `LEESEOLCOMBAT_RESTORE_VERIFICATION.md` document the current behavior.
- Still needs player-online verification: combat-tag logout punishment, normal logout
  corpse spawn, corpse kill pending death, and owner return cleanup.
- Decide whether logout corpses should survive plugin/server restarts. Current
  Citizens registry is in-memory and `onDisable` removes all active combat clones.
- PVP reward layer is implemented and deployed in `LeeSeolCombat`.
- `LEESEOLCOMBAT_PVP_REWARDS_VERIFICATION.md` exists. Server-side build/log/RCON
  verification passed.
- Current support: PVP points, player-head trophy drops, same-target reward cooldown,
  same town/nation reward suppression, `/combat pvp`, and admin `/combat pvppoints`.
- Still needs player-online verification with two players: real kill reward, trophy
  drop, repeat cooldown, same-affiliation suppression, and admin point adjustment.

## TAB / Visuals

- Later: per-user TAB themes.
- Later: event-limited nickname/TAB themes.
- Later: time/event-specific lobby display themes.

## Quest

- Expand `LeeSeolQuest` beyond the baseline tutorial:
  richer quest tracker UI and additional quest content beyond the baseline tutorial.
- `LEESEOLQUEST_VERIFICATION.md` exists. Current verified baseline: survival/lobby
  service status, plugin jar presence, shared quest data file, `version
  LeeSeolQuest`, `lsquest reload`, and lobby PlaceholderAPI parse output for
  `lee_seol`.
- `LeeSeolQuest` now has detailed `/quest progress`, `harvest-crop`,
  `dungeon-enter`, `npc-dialogue`, Bukkit quest events, and
  `LeeSeolQuestApi#progress(...)`.
- `LeeSeolEconomy` now soft-hooks Shift+F/`/servermenu` opens into Quest
  `open-gui` target `server-menu`; server-side build/deploy/RCON verification passed
  on 2026-06-08.
- Still needs player-online verification: `/quest` GUI, tutorial commands,
  survival PlaceholderAPI parse output, shared progress sync, restart persistence,
  real objective events, and `server-menu` open objective progress.
- `earn-money` now has a source hook from `LeeSeolJobs`; real player verification is
  still needed.
- `craft-item` now has a source hook from `LeeSeolCrafting`; real player
  verification is still needed.
- `rank-up` now has a source hook from `LeeSeolRanks`; real player verification is
  still needed.

## Plugin Design Queue

- `LEESEOLCRAFTING_DESIGN.md` exists and 0.1.0 has been implemented/deployed.
- `ANVIL_REPAIR_IMPACT_REVIEW.md` exists. Current recommendation: do not block
  vanilla anvil repair in LeeSeolCrafting 1.0.
- `ADVANCED_ENCHANTMENTS_BALANCE_REVIEW.md` exists. 2026-06-08 follow-up reviewed
  live `enchantments.yml`, group prices, enchantment-table access, and Jobs/Crafting
  overlap. On 2026-06-11, the first PvP safety patch was applied live:
  `explosive`, `reflect`, and `greatsword` chances are `0`; `forcefield` moved to
  `ELITE` with lower chances and `20s` cooldowns. Next decision: verify with players,
  then decide whether `blind`, `paralyze`, `shockwave`, or automation enchants need a
  second pass.
- `LEESEOLJOBS_DESIGN.md` exists and 0.1.0 has been implemented/deployed.
- `LEESEOLRANKS_RANKUP_EXPANSION_DESIGN.md` exists and 0.1.0 requirements expansion
  has been implemented/deployed.
- `LEESEOLHUD_DESIGN.md` exists and BossBar HUD 0.1.0 has been
  implemented/deployed.

## Jobs

- `LeeSeolJobs` is implemented and deployed to survival.
- `LEESEOLJOBS_VERIFICATION.md` exists. Console/RCON verification passed.
- 2026-06-11: Jobs was reworked into the first activity-system pass. `/activity`,
  `/activities`, `/expedition`, and `/explore` now alias `/jobs`; exploration rewards
  the first entry into each biome per day.
- Still needs player-online verification: mining reward, placed-ore abuse guard,
  farming reward, fishing reward, exploration biome reward, daily limit,
  `/activity` stats, and Quest `earn-money` progress.

## Crafting

- `LeeSeolCrafting` is implemented and deployed to survival.
- `LEESEOLCRAFTING_VERIFICATION.md` exists. Server-side build/log/RCON verification
  passed.
- Still needs player-online verification: `/craftmenu`, `/process`, `/disassemble`,
  recipe confirmation, money/material consumption, `/repair`, `/lscrafting reload`,
  and Quest `craft-item` progress.

## Ranks

- `LeeSeolRanks` rankup requirements expansion is implemented and deployed to
  survival/lobby.
- `LEESEOLRANKS_REQUIREMENTS_VERIFICATION.md` exists. Server-side build/log/RCON
  verification passed.
- Current rankup requirements check kills, Vault balance, and playtime minutes.
- Still needs player-online verification: `/rank progress`, `/rank requirements`,
  `/rankup`, LuckPerms sync after rankup, and Quest `rank-up` progress.

## HUD

- `LeeSeolHUD` is implemented and deployed to survival.
- `LEESEOLHUD_VERIFICATION.md` exists. Server-side build/log/RCON/resource-pack
  verification passed.
- Current support: image-based BossBar compass, `/hud`, `/lshud reload`, and
  PlaceholderAPI below-name health output for TAB.
- Target-health BossBar is intentionally disabled by default.
- Still needs player-online verification: client resource-pack redownload, compass
  image rendering, 5-degree compass motion with 15-degree labels, TAB below-name
  heart health bar, fading heal amount suffix, old target-health BossBar staying
  hidden, world-change cleanup, and permission behavior.

## Data Issues

- `LeeSeolTown` YAML parse error was rechecked on 2026-06-04 after the survival
  restart. It was not reproduced. Current `/opt/minecraft/shared/town/data.yml` is
  empty, parses successfully, and LeeSeolTown loaded `towns=0, nations=0, claims=0`.
  Keep monitoring this file after future town/nation tests.

## GUI / Resource-Pack Hand-Off

- `LeeSeolStations`, custom furniture click GUI, ItemsAdder custom item/furniture
  handling, generated resource packs, TAB logo visuals, and BetterRanks images belong
  to the GUI/resource-pack part, not the plugin-development part.
- Shift+F currently uses the PixieStudios DeluxeMenus GUI on survival/lobby through
  LeeSeolEconomy's external menu bridge. Future polish: Korean image/text
  translation, replacing remaining demo placeholders with expedition systems, and
  deciding whether to keep DeluxeMenus long-term or migrate the final behavior back
  into LeeSeolEconomy.
- `PIXIESTUDIOS_GUI_APPLY_FRAMEWORK.md` documents the current applied structure and
  the remaining GUI/resource-pack cautions.
- Deferred Shift+F GUI buttons after the 2026-06-06 slot pass:
  attendance reward, rankup GUI, donation PET GUI, nation member list, skill/rankup
  GUI, standalone skill GUI, survival spawn expansion buttons, map page 2/3 travel
  buttons, HOME integration, nation balance GUI, custom enchant GUI, donation system,
  player asset view, and final anarchy menu behavior.

## Translation

- Later: AdvancedEnchantments full Korean translation pass.
- Prefer editing/uploading config files directly instead of doing large translation
  work in `nano`.
