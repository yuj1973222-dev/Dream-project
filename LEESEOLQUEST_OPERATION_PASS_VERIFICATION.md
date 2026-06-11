# LEESEOLQUEST_OPERATION_PASS_VERIFICATION.md

Verification notes for LeeSeolQuest operating pass 0.1.

## 2026-06-11 Deployment

Implemented and deployed `LeeSeolQuest` operating pass 0.1 to survival and lobby.

### Added

- Quest reset periods:
  - `once`
  - `daily`
  - `weekly`
- Reset timezone:
  - `settings.reset-time-zone: "Asia/Seoul"`
- Daily operating pass quests:
  - `daily_check_in`
  - `daily_jobs_income`
  - `daily_crafting_processing`
  - `daily_fishing`
- Weekly operating pass quest:
  - `weekly_survival_routine`
- Quest GUI now shows each quest's reset period.

### Deployed Files

- Survival jar:
  `/opt/minecraft/server/plugins/LeeSeolQuest-0.1.0.jar`
- Survival config:
  `/opt/minecraft/server/plugins/LeeSeolQuest/config.yml`
- Lobby jar:
  `/opt/minecraft/lobby/plugins/LeeSeolQuest-0.1.0.jar`
- Lobby config:
  `/opt/minecraft/lobby/plugins/LeeSeolQuest/config.yml`

### Backups

- `/opt/minecraft/backups/LeeSeolQuest-0.1.0-survival-2026-06-11_19-51-58.jar`
- `/opt/minecraft/backups/LeeSeolQuest-0.1.0-lobby-2026-06-11_19-51-58.jar`
- `/opt/minecraft/backups/LeeSeolQuest-config-survival-2026-06-11_19-51-58.yml`
- `/opt/minecraft/backups/LeeSeolQuest-config-lobby-2026-06-11_19-51-58.yml`

### Verification

- Local build:
  `powershell -ExecutionPolicy Bypass -File .\tools\build-plugin.ps1 LeeSeolQuest -SkipTests`
- Build result:
  `BUILD SUCCESS`
- Jar descriptor:
  `plugin.yml` present in `LeeSeolQuest-0.1.0.jar`
- Remote service status:
  `minecraft: active`, `lobby: active`
- Remote config check:
  `daily_check_in`, `daily_jobs_income`, `daily_crafting_processing`,
  `daily_fishing`, `weekly_survival_routine`, and `reset-time-zone` are present.
- Remote plugin logs:
  - `Loaded 6 quests.`
  - `LeeSeolQuest enabled.`
  - PlaceholderAPI expansion registered.

### Remaining Player Tests

- Open `/quest` GUI and confirm the 6 quest entries render.
- Confirm `daily_check_in` completes through Shift+F or `/servermenu`.
- Confirm `daily_jobs_income` progresses through Jobs payouts.
- Confirm `daily_crafting_processing` progresses through `IRON_INGOT` crafting or
  processing.
- Confirm `daily_fishing` progresses on successful fishing.
- Confirm `weekly_survival_routine` progresses across its three stages.
- Confirm daily reset after the next Asia/Seoul date change.
- Confirm weekly reset on the next ISO week.

### Known Unrelated Log

After restart, ItemsAdder still logs:

`Particle DRAGON_BREATH for action play_particle of item iasurvival:end_sword is currently not supported by the plugin.`

The same message appeared before the LeeSeolQuest deployment at `2026-06-11 19:32:59`,
so it is not caused by the operating pass change.
