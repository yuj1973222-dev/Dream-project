# LeeSeolEconomy

Shared won economy, shop GUI, shop NPCs, and Shift+F server movement menu for the LeeSeol Minecraft network.

## Commands

- `/won`
- `/won balance [player]`
- `/won pay <player> <amount>`
- `/won give <player> <amount>`
- `/won take <player> <amount>`
- `/won set <player> <amount>`
- `/won reload`
- `/shop [shop]`
- `/wonnpc create <id> <shopId> [skin:<playerName>] [displayName]`
- `/wonnpc skin <id> <playerName|none>`
- `/wonnpc remove <id>`
- `/wonnpc list`
- `/servermenu`

## Shortcut

Sneak + swap hand key opens the server movement menu. By default this is `Shift + F`.

## Skin NPC

`/wonnpc create skin_shop general skin:lee_seol &e스킨 상점`

When a skin is set, the plugin spawns an invisible armor stand with the player's skull skin as the NPC head.
Full-body player NPCs require a packet/NMS or Citizens-based implementation and are intentionally left for a later chapter.
