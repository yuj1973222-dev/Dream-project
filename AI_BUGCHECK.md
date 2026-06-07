# AI Bugcheck Workflow

Use this checklist before asking AI to inspect bugs or regressions. It keeps context
small and makes code review more accurate.

## Quick State

```powershell
& 'C:\Program Files\Git\cmd\git.exe' status --short
& 'C:\Program Files\Git\cmd\git.exe' diff --stat
& 'C:\Program Files\Git\cmd\git.exe' diff
```

Send the target plugin name, the symptom, the commands already run, and the relevant
`git diff`. Do not paste secrets, RCON passwords, database passwords, private keys,
or Velocity forwarding secrets.

## Normal Review Order

1. Read `AGENTS.md`, `SERVER_STATE.md`, `PLUGIN_INDEX.md`, and the target plugin's
   minimal files from `PLUGIN_INDEX.md`.
2. Inspect `git diff` first. Bugs are easiest to find by comparing changed behavior
   against the last committed baseline.
3. Build only the changed plugin unless the diff touches shared contracts or plugin
   APIs.
4. After deployment, verify recent logs with a narrow time window, not old full logs.

## Useful Commands

```powershell
git log --oneline -10
git diff -- LeeSeolQuest
git diff -- LeeSeolEconomy/src/main/java
.\tools\local-env-check.ps1
.\tools\build-plugin.ps1 LeeSeolQuest -SkipTests
.\tools\build-plugin.ps1 LeeSeolEconomy -SkipTests
rg -n "ERROR|Exception|Could not load|LeeSeolQuest|LeeSeolEconomy" SERVER_STATE.md PLUGIN_INDEX.md TODO.md
```

## Commit Rule

Commit after a verified unit of work:

```powershell
& 'C:\Program Files\Git\cmd\git.exe' add .
& 'C:\Program Files\Git\cmd\git.exe' commit -m "Describe verified change"
```

Keep generated resource packs, previews, temporary archives, and deployment backups
out of Git unless a future task explicitly needs them as source material.
