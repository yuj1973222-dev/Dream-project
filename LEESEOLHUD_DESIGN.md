# LeeSeolHUD Design

작성일: 2026-06-04 KST

## 목적

리소스팩 이미지 없이 Paper 플러그인만으로 표시 가능한 HUD를 만든다.

범위:

- BossBar 상단 나침반 HUD
- 피격 대상 체력바
- 향후 PVP/던전 상태 표시

리소스팩 이미지 기반 HUD는 GUI/리소스팩 파트 담당이다.

## 신규 플러그인 분리 여부

`LeeSeolHUD` 신규 Paper 플러그인으로 분리한다.

이유:

- BossBar/ActionBar는 여러 시스템이 사용할 수 있어 충돌 관리가 필요하다.
- Combat, Dungeon, Jobs, Quest와 soft 연동할 수 있다.
- TAB/ItemsAdder 시각 리소스와 분리된 순수 서버 로직이다.

## 대상 서버

| 서버 | 적용 여부 | 이유 |
|---|---|---|
| survival | YES | 나침반, 전투 체력바, 던전 안내 |
| lobby | OPTIONAL | 로비에서는 필요 시 안내 HUD만 |
| newworld | NO | 현재 inactive |
| velocity | NO | Paper 전용 |

1차 배포는 survival만 권장한다.

## 패키지 구조

```text
LeeSeolHUD/
  pom.xml
  src/main/resources/
    plugin.yml
    config.yml
  src/main/java/me/leeseol/hud/
    LeeSeolHudPlugin.java
    command/
      HudCommand.java
      HudAdminCommand.java
    service/
      CompassHudService.java
      TargetHealthService.java
      PlayerHudStateService.java
    listener/
      HudPlayerListener.java
      TargetDamageListener.java
    model/
      CompassDirection.java
      HudPreference.java
    storage/
      HudStore.java
    util/
      Text.java
```

## Config 예시

```yaml
settings:
  enabled: true
  save-preferences: true

compass:
  enabled: true
  worlds:
    - world
    - world_nether
    - dungeon
  update-ticks: 10
  default-enabled: true
  bossbar:
    color: BLUE
    style: SOLID
  format: "&b%compass% &7| &f%yaw%"

target-health:
  enabled: true
  worlds:
    - world
    - world_nether
    - dungeon
  show-seconds: 5
  bossbar:
    color-high: GREEN
    color-mid: YELLOW
    color-low: RED
    style: SEGMENTED_10
  format: "&c%target% &f%health%/%max_health%"
  ignore-citizens-npcs: true

pvp:
  combat-tag-indicator:
    enabled: false
    source-plugin: LeeSeolCombat

dungeon:
  status-indicator:
    enabled: false
    source-plugin: LeeSeolDungeon
```

## 명령어

| 명령어 | 권한 | 역할 |
|---|---|---|
| `/hud` | `leeseolhud.use` | 내 HUD 상태 확인 |
| `/hud compass on` | `leeseolhud.use` | 나침반 켜기 |
| `/hud compass off` | `leeseolhud.use` | 나침반 끄기 |
| `/hud target on` | `leeseolhud.use` | 대상 체력바 켜기 |
| `/hud target off` | `leeseolhud.use` | 대상 체력바 끄기 |
| `/lshud reload` | `leeseolhud.admin` | 설정 리로드 |

## 권한

```text
leeseolhud.use
leeseolhud.admin
leeseolhud.compass
leeseolhud.target-health
```

## 상단 나침반 HUD

### 동작

플레이어 yaw를 기준으로 방향을 계산한다.

| yaw 방향 | 표시 |
|---|---|
| 북 | N |
| 북동 | NE |
| 동 | E |
| 남동 | SE |
| 남 | S |
| 남서 | SW |
| 서 | W |
| 북서 | NW |

BossBar 텍스트 예시:

```text
W | NW | N | NE | E
```

1차는 단순 방향 문자열로 구현한다. 실제 배틀그라운드식 눈금형 UI는 리소스팩 없이 한계가 있으므로 후순위다.

### 성능 기준

- 모든 tick 업데이트 금지.
- 기본 `update-ticks: 10`.
- 플레이어가 허용 월드에 있을 때만 갱신.
- 퇴장 시 BossBar 제거.

## 피격 대상 체력바

### 동작

플레이어가 엔티티를 공격하면 대상 체력을 BossBar로 표시한다.

표시 대상:

- Monster
- Player
- 추후 config로 확장

제외 대상:

- Citizens NPC
- ArmorStand
- Interaction hitbox
- 죽은 엔티티

### 표시 기준

```text
대상 이름 체력/최대체력
```

색상:

- 70% 이상: GREEN
- 30~70%: YELLOW
- 30% 미만: RED

## ActionBar 사용 여부

1차에서는 ActionBar를 사용하지 않는다.

이유:

- 다른 플러그인의 안내 메시지를 덮어쓸 가능성이 크다.
- BossBar가 목적에 더 적합하다.

ActionBar는 추후 짧은 상태 알림 전용으로만 사용한다.

## 데이터 저장

경로:

```text
/opt/minecraft/shared/hud/preferences.yml
```

저장 항목:

```yaml
players:
  <uuid>:
    compass: true
    target-health: true
```

저장 기능은 1차에 선택 사항이다. 기본값이 충분하면 config만 사용한다.

## 연동 계획

### LeeSeolCombat

후순위:

- 전투 태그 남은 시간 BossBar 표시
- 전투 종료 시 자동 제거

주의:

- Combat도 메시지를 보내므로 ActionBar는 피한다.

### LeeSeolDungeon

후순위:

- 던전 입장 중 상태
- 던전 보호/상자 안내
- dungeon 월드에서만 별도 HUD

### LeeSeolQuest

후순위:

- 현재 퀘스트 목표를 BossBar에 표시하지 않는다.
- 퀘스트 트래커는 PlaceholderAPI/TAB 또는 별도 scoreboard와 충돌 가능성이 있다.

## 구현 순서

1. 플러그인 골격 생성
2. config 로더
3. `/hud compass on/off`
4. 개인별 BossBar 관리
5. yaw 기반 방향 계산
6. 대상 체력바 리스너
7. 퇴장/월드 이동 cleanup
8. `/lshud reload`
9. survival 배포

## 검증 기준

| 테스트 | 기대 결과 |
|---|---|
| 접속 | 기본 설정에 따라 나침반 BossBar 표시 |
| `/hud compass off` | 나침반 숨김 |
| 월드 이동 | 허용 월드 기준으로 표시/숨김 |
| 퇴장 | BossBar 제거 |
| 몬스터 공격 | 대상 체력바 표시 |
| 대상 사망 | 체력바 제거 |
| Citizens NPC 공격 | 체력바 미표시 |
| reload | 설정 반영 |

## 주의점

- TAB BossBar 기능과 충돌하지 않게 TAB 설정 확인이 필요하다.
- 여러 BossBar를 동시에 띄울 때 화면이 과해질 수 있다.
- 전투/던전 HUD는 LeeSeolCombat/LeeSeolDungeon API가 정리된 뒤 붙인다.
