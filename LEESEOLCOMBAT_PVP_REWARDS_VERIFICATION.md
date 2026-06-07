# LeeSeolCombat PVP Rewards Verification

작성일: 2026-06-04 KST

## Scope

`LeeSeolCombat` 0.1.0 now includes the first PVP reward layer.

Implemented features:

- Player kill PVP point records
- Player trophy item drop using the victim's player head
- Repeated same-target reward cooldown
- Optional same town/nation reward suppression through PlaceholderAPI and LeeSeolTown placeholders
- Admin command for PVP point adjustment
- User/admin command for PVP point lookup

This change stays inside `LeeSeolCombat`. It does not modify `LeeSeolRanks`,
`LeeSeolTown`, TAB, ItemsAdder, resource packs, or LuckPerms data.

## Modified Files

- `LeeSeolCombat/src/main/java/me/leeseol/combat/LeeSeolCombatPlugin.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/command/CombatCommand.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/config/CombatConfig.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/listener/PvpRewardListener.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/model/PvpRecord.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/service/PvpRewardService.java`
- `LeeSeolCombat/src/main/java/me/leeseol/combat/storage/PvpPointStore.java`
- `LeeSeolCombat/src/main/resources/config.yml`
- `LeeSeolCombat/src/main/resources/plugin.yml`

## Config

New section:

```yaml
pvp-rewards:
  enabled: true
  data-file: "/opt/minecraft/shared/combat/pvp.yml"
  enabled-worlds:
    - world
    - world_nether
  points-per-kill: 1
  repeat-kill-cooldown-seconds: 600
  ignore-same-town-or-nation: true
  trophy:
    enabled: true
    name: "&c%victim%의 증표"
    lore:
      - "&7처치자: &f%killer%"
      - "&7희생자: &f%victim%"
      - "&7시간: &f%time%"
```

## Commands

```text
/combat pvp [player]
/combat pvppoints <set|add|take> <player> <amount>
```

`pvppoints` requires `leeseolcombat.admin`.

## Deployment

Deployed to:

```text
/opt/minecraft/server/plugins/LeeSeolCombat-0.1.0.jar
```

Service restarted:

```text
minecraft
```

`newworld` remained inactive.

## Server-Side Verification

Passed checks:

- `minecraft=active`
- `newworld=inactive`
- recent logs include `LeeSeolCombat enabled. combatWorlds=3, cloneWorlds=2`
- recent logs include `Done`
- deployed config contains `pvp-rewards`
- no new `ERROR`, `Exception`, or `Could not load` lines for `LeeSeolCombat`

RCON responses:

```text
version LeeSeolCombat
LeeSeolCombat version 0.1.0

combat status
[전투] 전투 태그: 0 / NPC: 0 / 관전 NPC: ON / PVP 기록: 0

combat pvp
사용법: /combat pvp <player>

combat pvppoints
사용법: /combat pvppoints <set|add|take> <player> <amount>
```

## Player-Online Verification Still Needed

These require at least two online players:

- Kill another player in `world` and confirm one PVP point is added.
- Confirm the victim's player-head trophy is added to death drops.
- Kill the same target again within 600 seconds and confirm no second reward is given.
- Confirm same town/nation kills do not give rewards when LeeSeolTown placeholders are present.
- Confirm `/combat pvp` shows the player's current points and kill count.
- Confirm `/combat pvppoints add|take|set` works for an admin.

## Notes

- Existing combat-tag logout punishment and normal logout corpse clone behavior were not changed.
- The repeated-kill cooldown is runtime-only in this first version.
