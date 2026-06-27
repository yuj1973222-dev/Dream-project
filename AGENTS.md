# AGENTS.md

LeeSeol Network Minecraft server workspace. Keep this file short. Long history
belongs in git, not active context.

## Communication

- Reply in Korean. The user is learning server development, so explain the practical
  reason for changes without over-explaining code internals.
- Be proactive, but keep scope tiny: inspect/edit/build/deploy/verify only the
  requested feature.
- Keep work visible with short progress updates during long tasks.
- If the user sends the Korean resume keyword U+C7AC U+AC1C, restart the development
  environment and Minecraft services, then verify services, ports, and recent logs
  before continuing.

## Current Direction

- This chat is the main server-operations chat.
- Build server features first, then item/content planning, then balance, then final
  GUI/resource-pack polish.
- Lobby and survival are separate products:
  - lobby: movement-only network gate
  - survival: main semi-vanilla/RPG server
  - newworld: paused future backend
- One request = one domain. Pick one primary source plugin, then touch only the
  consumers/bridges required to display or integrate that same domain.
- Do not load or edit old GUI/resource-pack experiments unless the user explicitly
  asks for that part.
- Do not check, continue, render, or manage Chunky/BlueMap unless the user explicitly
  asks. The user monitors them.

## Scope Discipline

Before feature work, state the scope in this shape:

```text
Work:
Primary:
Allowed consumers:
Bridge/config:
Affected contracts:
Not included:
Verify:
```

- Primary owns the data or behavior for the domain.
- Consumers may be changed only for display or integration of that same domain.
- Bridge/config covers external settings such as TAB, PlaceholderAPI, LuckPerms,
  Vault, BetterHUD, BetterRanks, ItemsAdder, WorldGuard, or BlueMap.
- Affected contracts lists shared APIs, placeholders, commands, config/data keys,
  permissions, GUI/display surfaces, or server routes that other plugins depend on.
- If a contract changes, include the affected consumers/bridges in verification or
  stop and ask before expanding the work.
- Prefer plugin APIs, services, events, or placeholders over reading another
  plugin's internal config/data shape directly.
- If a second domain is needed, stop and ask before expanding scope.
- Superpowers outputs are chat-first by default; see Superpowers Hygiene for when a
  persistent artifact is worth saving.

## Parallel Work Gate

- Block parallel work if it touches the same primary plugin, shared data/config,
  live service, deploy target, command namespace, permission, placeholder, or GUI
  surface.
- Treat `LeeSeolTown`, `LeeSeolCore`, and `LeeSeolProxy` as high-risk integration
  owners. Default to sequential work when they are involved.
- A worktree isolates files, not live server state, shared APIs, shared database
  rows, or resource-pack/config side effects.

## Context Budget

Read only what the task needs:

- Always: this file.
- Deployment/architecture task: `SERVER_STATE.md`.
- Plugin/code task: use `rg` in `PLUGIN_INDEX.md` for the target plugin row and
  relevant integration-map row, then open only those files.
- After every plugin/code task, check whether `PLUGIN_INDEX.md` still matches the
  current code. Update it before the final response if responsibilities, commands,
  permissions, APIs/events/placeholders, shared config/data, integrations, deploy
  targets, minimal files, or verification steps changed. If no update is needed,
  say that it was checked and left unchanged.
- Deferred/planning task: create/read `TODO.md` only after the user and agent agree
  on the current goal.
- Economy, custom enchanting, BetterHUD, or ItemsAdder learning task: read only
  current code/config unless the user explicitly provides a handoff file.

Do not read all plugins, all markdown, deleted reports, or broad logs for normal
work.

## Superpowers Hygiene

- Superpowers skills may be used for reasoning, debugging, planning, and verification.
- Keep Superpowers outputs in chat by default.
- Create persistent Superpowers artifacts only when they are useful for handoff,
  long-running implementation, code review, or user-requested documentation.
- Do not create `docs/superpowers/*` for small TODO, prompt-writing, AGENTS,
  PLUGIN_INDEX, or one-file documentation updates.
- If a Superpowers artifact is created, keep it short, name it clearly, and mention
  why it was worth saving.
- Before the final response, check whether unexpected Superpowers files were created.
  If they were created by the current task and are not useful, remove them or ask
  before keeping them.

## Server Architecture

- VM: `minecraft-server`, zone `asia-northeast3-a`
- gcloud path:
  `C:\Users\user\AppData\Local\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd`
- Velocity public entry: `0.0.0.0:25565`
- Survival Paper: `/opt/minecraft/server`, service `minecraft`, bind `127.0.0.1:25566`
- Lobby Paper: `/opt/minecraft/lobby`, service `lobby`, bind `127.0.0.1:25567`
- Resource pack host: service `resourcepack`, bind `0.0.0.0:8163`
- Newworld Paper: `/opt/minecraft/newworld`, service `newworld`, bind `127.0.0.1:25568`, normally disabled
- Velocity path/service: `/opt/minecraft/velocity`, service `velocity`
- MariaDB is used for shared LuckPerms data.

Normal active services: `mariadb`, `resourcepack`, `velocity`, `minecraft`, `lobby`.
Start `newworld` only when the user resumes New World work.

## Plugin Targets

- Survival only: `LeeSeolEconomy`, `LeeSeolAuction`, `LeeSeolDungeon`,
  `LeeSeolCombat`, `LeeSeolCleanup`, `LeeSeolRanks`, `LeeSeolQuest`,
  `LeeSeolJobs`, `LeeSeolCrafting`, `LeeSeolEnchanting`, `LeeSeolTown`
- Lobby only: `LeeSeolLobby`
- Survival + lobby: `LeeSeolCore`, `LeeSeolHologram`
- Velocity: `LeeSeolProxy`
- Disabled legacy: `LeeSeolHUD`

Do not deploy survival/lobby plugin sets into `newworld`. Do not recreate old
`dungeon` server folders/services; the dungeon is an internal survival world.

## Remote Work

- Prefer uploading a short script to `/tmp` and running it over putting complex bash
  inside a PowerShell `gcloud --command`.
- Always make a timestamped backup before replacing deployed jars, configs, worlds,
  or shared data.
- Do not print secrets, RCON passwords, DB passwords, tokens, or private keys.
- Clean `/tmp` helpers created for the current task after success.
- If editing files owned by `/opt/minecraft`, use sudo as needed and restore
  ownership to `yuj1973222:yuj1973222`.
- Do not add Chunky/BlueMap status checks to deploy scripts unless explicitly asked.

## Build And Deploy

- Use Maven for Java plugin builds unless the module already has another working
  build flow.
- Build only the target plugin.
- Deploy only the affected jar to the correct backend.
- Restart only affected services.
- Verify jar descriptor exists:
  - Paper: `plugin.yml` or `paper-plugin.yml`
  - Velocity: `velocity-plugin.json`

## Verification

For plugin/config deploys, verify only the changed service/plugin:

- changed service active and reached `Done`
- plugin enabled/version command works when possible
- no recent new `ERROR`, `Exception`, or `Could not load` lines for the changed part

Use recent logs, not broad historical logs:

```bash
sudo journalctl -u minecraft -u lobby -u velocity --since "5 minutes ago" --no-pager
```

## Git And Files

- The worktree may already be dirty. Never revert or delete unrelated user changes.
- Use `apply_patch` for manual edits.
- Do not use destructive git commands unless the user explicitly asks.
- Use worktrees as short-lived feature sandboxes, not long-term storage:
  - create one worktree per agreed feature/domain;
  - do not create more than one worktree in the same chat/session;
  - keep the main checkout for coordination and server operations;
  - do not mix unrelated plugin domains in one worktree;
  - keep the worktree through implementation, deploy verification, and immediate
    bug-fix follow-up instead of removing it after the first successful build;
  - when the feature and bug-fix pass look complete, report the worktree path,
    branch, and status, then ask the user before removing it;
  - remove the worktree only after the user approves removal.
- Do not create a worktree for small documentation-only updates.
- If deleting obsolete docs, rely on git history for recovery and update active
  context files so future agents do not chase deleted references.

## Repeated Mistakes To Avoid

- Do not inline complex bash through PowerShell.
- Do not inspect huge old logs and treat historical errors as current.
- Do not resume or report Chunky/BlueMap as a side effect of unrelated work.
- Do not deploy `LeeSeolDungeon` to `newworld`.
- Do not recreate Velocity backend `dungeon`, `/opt/minecraft/dungeon`, or
  `dungeon.service`.
- Do not use Paper backend resource-pack settings as the network pack source.
  Velocity/`LeeSeolProxy` owns the one network pack offer.
- Do not broadly rewrite ItemsAdder configs. Patch exact blocks only.
- Do not touch GUI/resource-pack assets in this operations chat unless explicitly
  requested.
