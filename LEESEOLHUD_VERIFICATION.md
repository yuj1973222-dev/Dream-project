# LeeSeolHUD Verification

작성일: 2026-06-04 KST

## Scope

`LeeSeolHUD` 0.1.0 was implemented as a survival-only Paper plugin.

Implemented features:

- BossBar compass HUD
- Target health BossBar after damaging a living entity
- Player preference toggles through `/hud`
- Admin reload/status through `/lshud`
- Safe world filtering for `world`, `world_nether`, and `dungeon`

The implementation intentionally does not touch TAB, ItemsAdder, generated resource
packs, BetterRanks images, or Velocity resource-pack settings.

## Modified Files

- `LeeSeolHUD/pom.xml`
- `LeeSeolHUD/src/main/resources/plugin.yml`
- `LeeSeolHUD/src/main/resources/config.yml`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/LeeSeolHudPlugin.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/command/HudCommand.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/command/HudAdminCommand.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/listener/HudPlayerListener.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/listener/TargetDamageListener.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/service/CompassHudService.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/service/TargetHealthService.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/service/PlayerHudStateService.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/storage/HudStore.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/model/CompassDirection.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/model/HudPreference.java`
- `LeeSeolHUD/src/main/java/me/leeseol/hud/util/Text.java`
- `deploy-leeseolhud.sh`

## Deployment

Deployed to:

```text
/opt/minecraft/server/plugins/LeeSeolHUD-0.1.0.jar
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
- survival backend still listening on `127.0.0.1:25566`
- Velocity still listening on public `25565`
- resource-pack service still listening on `8163`
- recent logs include `LeeSeolHUD v0.1.0`
- recent logs include `LeeSeolHUD enabled.`
- recent logs include `Done preparing level "world"`
- no new `ERROR`, `Exception`, or `Could not load` lines for `LeeSeolHUD`

RCON responses:

```text
version LeeSeolHUD
LeeSeolHUD version 0.1.0

lshud status
[HUD] online=0 settings.enabled=true

lshud reload
[HUD] LeeSeolHUD 설정을 다시 불러왔습니다.
```

## Player-Online Verification Still Needed

These require an in-game player:

- Join survival and confirm the compass BossBar appears by default.
- Run `/hud` and confirm current HUD state text.
- Run `/hud compass off` and confirm the compass BossBar disappears.
- Run `/hud compass on` and confirm it returns.
- Run `/hud target off` and confirm target health display is disabled.
- Run `/hud target on`, hit a living entity, and confirm target health BossBar appears.
- Change worlds between `world`, `world_nether`, and `dungeon` and confirm BossBars clean up or appear according to config.
- Confirm players without `leeseolhud.compass` or `leeseolhud.target-health` do not see those HUD elements.

## Notes

- Local PC did not have Maven installed, so build verification was performed on the VM.
- Temporary RCON verification read `server.properties` internally and did not print the RCON password.

## 2026-06-04 Revision: Below-Name Health And Image Compass

The earlier target-health BossBar was disabled because the intended requirement was
player health under the nickname, not a target HUD.

Changes made:

- `target-health.enabled: false`
- Added PlaceholderAPI expansion `leeseolhud`
- Added `%leeseolhud_healthbar%`, `%leeseolhud_health%`,
  `%leeseolhud_max_health%`, `%leeseolhud_compass_glyph%`, and
  `%leeseolhud_compass_degrees%`
- Survival TAB `belowname-objective` now uses `%leeseolhud_healthbar%`
- Survival TAB YAML indentation error in the disabled scoreboard block was fixed so
  TAB can load normally
- BossBar compass now uses four ItemsAdder/resource-pack font glyph segments per
  5-degree heading from `0` to `355`
- Compass glyph base codepoint: `U+E340`
- Compass glyph range: `U+E340` through `U+E7BF`
- Compass glyph count: `1152` chars for `72` headings, `16` segments per heading
- Compass glyph textures were generated under
  `assets/generated/leeseolhud_compass/`
- Hosted resource pack SHA1:
  `aa260c65b346d990e4420be277eeff5d6ccbb0db`
- Compass image style: transparent background, `1920x136` source texture rendered
  at roughly `480x34`, clearer white/gray coordinate line, intercardinal labels
  such as `NE`, labels without shadow, labels placed with more vertical breathing
  room, stronger edge-only fade, fixed-width segment anchors to prevent
  heading-dependent center drift, and a slightly right-shifted center marker.
- Compass update interval: `1` tick.
- Compass labels are shown every `15` degrees, while image headings are generated
  every `5` degrees for smoother motion.
- The compass reserves WHITE BossBar sprites as transparent resource-pack sprites:
  `assets/minecraft/textures/gui/sprites/boss_bar/white_background.png` and
  `white_progress.png`.

Server-side verification passed:

- `resourcepack=active`
- `velocity=active`
- `minecraft=active`
- `lobby=active`
- `newworld=inactive`
- `LeeSeolHUD` enabled
- PlaceholderAPI expansion `leeseolhud` registered
- TAB enabled without YAML parser errors
- `belowname-objective.enabled: true`
- hosted `generated.zip` SHA matches Velocity configured SHA
- resource pack contains `assets/leeseolhud/font/compass.json`
- resource pack contains 72 compass textures including `000`, `005`, `045`, `090`,
  `180`, `270`, and `355`
- resource pack contains 72 compass providers and 1152 compass glyph chars
- sample compass raw glyph cell width is `120px`; rendered glyph cell width is
  `30px`, avoiding the earlier failed single-glyph width of `420px`
- resource pack contains transparent WHITE BossBar background/progress sprites
- deployed jar contains the direct `/compasshud <on|off>` command
- RCON `version LeeSeolHUD` succeeded
- RCON `lshud status` succeeded

Player-online verification still needed:

- Rejoin survival and accept/redownload the updated network resource pack.
- Confirm old target-health BossBar does not appear after hitting an entity.
- Confirm TAB nickname-underlay health bar appears under players.
- Confirm compass BossBar renders as an image, not as a private-use unicode glyph.
- Turn slowly and confirm the compass image changes every 5 degrees while labels
  remain 15-degree based.
- If the image does not show, clear the client server resource-pack cache and rejoin.
