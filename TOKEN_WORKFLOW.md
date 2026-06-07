# TOKEN_WORKFLOW.md

This workflow reduces token use during plugin development and server maintenance.

## Default Context Budget

Before starting a normal plugin task, read only:

1. `AGENTS.md`
2. `SERVER_STATE.md`
3. `PLUGIN_INDEX.md`
4. `TODO.md` only if the request mentions deferred work
5. The target plugin's minimal files from `PLUGIN_INDEX.md`

Do not read every plugin, every deploy script, or broad server logs unless the task
requires cross-plugin debugging.

## One Plugin Rule

- Treat one request as one feature in one primary plugin.
- If a request touches multiple plugins, name the order before editing.
- Deploy through one main chat/session only. Other chats may draft or analyze, but
  should not deploy to the live VM.

## Search Rules

- Use `rg --files` to locate files.
- Use targeted `rg "symbol"` inside the target plugin.
- Avoid broad text searches across the whole workspace unless the dependency is
  unknown.
- Prefer reading one class at a time over loading entire modules.

## Remote Log Rules

- Do not paste or read long historical logs.
- Prefer:

```bash
sudo journalctl -u minecraft -u lobby -u velocity --since "5 minutes ago" -p err --no-pager
```

- If old logs are needed, grep only the exact plugin name or error phrase.

## Deploy Rules

- Build/deploy only the target plugin.
- Restart only affected services.
- If a config is owned by a running plugin and the plugin saves on disable, stop the
  service before editing that config.
- Always clean temporary scripts created for the current task.

## Summary Cache Rule

After a meaningful change, update only the shortest relevant state file:

- Architecture or service changes: `SERVER_STATE.md`
- Plugin responsibility or deploy target changes: `PLUGIN_INDEX.md`
- Deferred work: `TODO.md`
- New repeated mistake: `AGENTS.md`

Avoid writing long narrative summaries into the chat when a state file can hold the
stable fact.

## User Request Shortcut

For lowest token use, send requests in this shape:

```text
Target: LeeSeolRanks
Goal: Change only the default PLAYER rank behavior.
Scope: Touch LeeSeolRanks only. Do not edit TAB or ItemsAdder.
Verify: Build succeeds and recent server logs have no errors.
Deploy: Apply live. Restart minecraft/lobby is allowed.
```
