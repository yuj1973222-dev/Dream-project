# TOKEN_WORKFLOW.md

`AGENTS.md` is the canonical low-token workflow. Keep this file as a tiny reminder,
not a second rulebook.

## Default Read Set

- Always read `AGENTS.md`.
- Read `SERVER_STATE.md` only for architecture, deploy, or live service state.
- For plugin work, search `PLUGIN_INDEX.md` for the target plugin and open only the
  listed files.
- Read `TODO.md` only for planning or deferred work.
- Read handoff files only when the user provides or explicitly names one.

## Work Shape

- One request should target one feature and one primary plugin.
- If multiple plugins are necessary, state the order before editing.
- Use recent logs only for live verification.
- Do not check Chunky, BlueMap, old GUI/resource-pack experiments, or deleted reports
  unless explicitly requested.

## After Work

Update only the shortest relevant active file:

- `SERVER_STATE.md`: deployed architecture or service state
- `PLUGIN_INDEX.md`: plugin ownership, read set, deploy target, verification
- `TODO.md`: backlog or current development order
- `AGENTS.md`: durable operating rule or repeated mistake
