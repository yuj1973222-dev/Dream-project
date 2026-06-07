# ROADMAP_GAP_ANALYSIS.md

작성일: 2026-06-03 KST

## 목적

현재 `expedition` Minecraft Velocity + Paper 서버의 구현 상태를 기존 기획 방향과 비교한다.

원칙:

- `DONE` 기능은 중복 개발하지 않는다.
- `PARTIAL`, `BROKEN`, `MISSING`만 작업 후보로 둔다.
- 기존 설정, 데이터, 리소스팩은 초기화하지 않는다.
- `Newworld`는 paused 상태로 유지한다.

## 현재 구조 요약

| 항목 | 현재 상태 |
|---|---|
| Proxy | Velocity, public `25565` |
| Survival | Paper, service `minecraft`, backend `127.0.0.1:25566` |
| Lobby | Paper, service `lobby`, backend `127.0.0.1:25567` |
| Newworld | service `newworld`, paused/inactive |
| Dungeon | survival Paper 내부 `dungeon` world |
| Resource pack | lobby ItemsAdder output을 `resourcepack` service가 `8163`으로 hosting |
| Shared DB | MariaDB for LuckPerms |

## 기능 상태 표

| 기능 | 상태 | 관련 플러그인 | 관련 파일 | 판단 근거 | 작업 여부 |
|---|---|---|---|---|---|
| Velocity 이동 명령어 | DONE | LeeSeolProxy | `LeeSeolProxy/src/main/java/...` | `/lobby`, `/survival`, `/servers` 계열 proxy command 구조 존재 | 패스 |
| Resource pack offer | PARTIAL | LeeSeolProxy | `LeeSeolProxyPlugin.java`, `resourcepack.properties` | URL/SHA1 일치. 단 클라이언트 적용 상태 로그 없음 | 보완 후보 |
| 로비 보호 | DONE | LeeSeolLobby | `LeeSeolLobbyPlugin.java`, `config.yml` | 블록/피해/허기/스폰/어드벤처 처리 구현 | 패스 |
| 로비 TAB 로고 | BROKEN | LeeSeolLobby / ItemsAdder / TAB | `LeeSeolLobbyPlugin.java`, ItemsAdder output, TAB config | 서버 매핑은 맞지만 유저 화면에서 유니코드만 보임 | 진단 우선 |
| TAB 랭크/소속 표시 | DONE | TAB / LeeSeolRanks / LeeSeolTown | TAB `groups.yml`, `RankPlaceholderExpansion`, `TownPlaceholderExpansion` | rank/town placeholders와 TAB prefix 연동 존재 | 패스 |
| 랭크 저장/권한 동기화 | DONE | LeeSeolRanks | `RankStore`, `PermissionService`, `RankAdminCommand` | shared ranks 파일, LuckPerms sync, admin/dev rank 구현 | 패스 |
| 랭크업 조건 확장 | PARTIAL | LeeSeolRanks | `config.yml`, `RankUpCommand`, `RankListener` | kill threshold 기반만 존재. 돈/퀘스트/던전/제작 조건 없음 | 확장 |
| 원화 경제/Vault | DONE | LeeSeolEconomy | `LeeSeolVaultEconomy`, `BalanceStore` | Vault provider, balance, pay 구조 존재 | 패스 |
| 상점 GUI | PARTIAL | LeeSeolEconomy | `ShopManager`, `ShopCommand`, `config.yml` | 기본 상점/구매/판매 존재. 고급 편집/밸런스 부족 | 보완 |
| NPC 상점 | PARTIAL | LeeSeolEconomy | `NpcManager`, `NpcCommand` | shop NPC 구조 존재. 스킨/고급 편집은 부족 | 보완 |
| Shift+F 서버 메뉴 | DONE | LeeSeolEconomy | `ServerMenuManager` | shortcut, blocked dungeon world, server movement item 구현 | 패스 |
| 경매 시스템 | PARTIAL | LeeSeolAuction | `AuctionService`, `AuctionGui`, `AuctionStore` | 제출/선정/입찰/종료 구현. 실제 플레이 검증과 UI polish 필요 | 보완 |
| 내부 던전 월드 | PARTIAL | LeeSeolDungeon | `DungeonWorldManager`, `DungeonPortalManager` | survival 내부 world 구조, portal/loot/protection 존재. 실제 portal/loot spot 설정 필요 | 보완 |
| 던전 보호 | DONE | LeeSeolDungeon | `DungeonProtectionListener` | block/place/bucket/explosion 제한 config 구현 | 패스 |
| 랜덤 루트 상자 | PARTIAL | LeeSeolDungeon | `LootChestManager` | loot table/spot 구조 존재. 실제 콘텐츠 설정 필요 | 보완 |
| 파티/국가/연방 | PARTIAL | LeeSeolTown | `TownService`, `TownStore`, `TownCommand` | party/nation/federation/claim/chat/war 일부 구현. 국가 core 파괴 없음 | 확장 |
| 신호기 기반 국가 클레임 | DONE | LeeSeolTown | `Nation`, `ClaimProtectionListener`, `TownService` | beacon claim, adjacency, claim cost, mining fatigue 구현 | 패스 |
| 국가 전쟁 선포/항복/보호 | PARTIAL | LeeSeolTown | `War`, `WarStatus`, `TownService` | 선포/수락/항복/보호/카르마/배상금 구현. core 파괴/자동 승패 없음 | 확장 |
| 국가 코어 파괴 | MISSING | LeeSeolTown 또는 신규 LeeSeolWar | 없음 | core 체력/파괴/보상/클레임 패널티 미구현 | 신규/확장 |
| 마을/국가 채팅 | DONE | LeeSeolTown | `TownChatListener`, `ChannelChatCommand` | party/nation chat mode 구현 | 패스 |
| 홀로그램 | DONE | LeeSeolHologram | `HologramService`, `HologramCommand` | in-game RGB hologram 관리 구조 존재 | 패스 |
| 전투 태그 | DONE | LeeSeolCombat | `CombatTagManager`, `PvpCombatListener` | 15초 combat tag 구현 | 패스 |
| 전투 로그아웃 클론 | PARTIAL | LeeSeolCombat / Citizens | `CombatCloneManager`, config | Citizens clone/hitbox 구현. 히트박스 품질은 추가 확인 필요 | 보완 |
| 드랍 아이템 정리 | DONE | LeeSeolCleanup | `CleanupManager`, `CleanupCommand` | periodic cleanup config/command 존재 | 패스 |
| 튜토리얼/퀘스트 | MISSING | 신규 LeeSeolQuest 권장 | 없음 | NPC 대화/진행 상태/목표 타입/보상 구조 없음 | 1순위 |
| 제작/가공/강화 | MISSING | 신규 LeeSeolCrafting 권장 | 없음 | recipe GUI, processing, enhancement 없음 | 1순위 |
| ItemsAdder 가구 클릭 GUI | MISSING | 신규 LeeSeolStations 권장 | 없음 | furniture click to GUI binding 없음 | 1순위 |
| Jobs 돈 수급 | MISSING | 신규 LeeSeolJobs 권장 | 없음 | mining/farming/fishing reward loop 없음 | 1순위 |
| PVP 포인트/증표 | MISSING | LeeSeolCombat 확장 또는 LeeSeolPvPPoints | 없음 | kill token, repeat cooldown, points 없음 | 2순위 |
| 칭호 시스템 | MISSING | 신규 LeeSeolTitles 권장 | 없음 | title acquisition/equip/placeholder 없음 | 3순위 |
| 웹 지도 | MISSING | BlueMap/Dynmap 검토 | 없음 | map plugin 미도입 | 3순위 |

## 기존 기획 대비 부족한 기능

### 1순위: 플레이 루프 완성

| 기능 | 권장 방식 | 이유 |
|---|---|---|
| 튜토리얼/퀘스트 | 신규 `LeeSeolQuest` | 여러 플러그인의 시작 흐름을 묶는 상위 진행 시스템이므로 분리 |
| 제작/가공/강화 | 신규 `LeeSeolCrafting` | GUI/레시피/재료/확률/랭크 조건이 커서 경제와 분리 |
| 가구 클릭 GUI 연동 | 신규 `LeeSeolStations` | ItemsAdder 의존 optional 처리 필요. 제작/퀘스트/상점과 연결 |
| 랭크업 조건 확장 | 기존 `LeeSeolRanks` 확장 | rank data와 LuckPerms sync가 이미 존재 |
| 초반 돈 수급 | 신규 `LeeSeolJobs` | mining/farming/fishing reward loop는 economy provider와 분리 |

### 2순위: 후반 경쟁 콘텐츠

| 기능 | 권장 방식 | 이유 |
|---|---|---|
| PVP 포인트/증표 | `LeeSeolCombat` 확장 또는 신규 `LeeSeolPvPPoints` | combat logout과 kill handling 연동 필요 |
| 국가 코어 파괴 | `LeeSeolTown` 확장 또는 신규 `LeeSeolWar` | 국가/클레임/전쟁 데이터와 밀접 |
| 세금/유지비 | `LeeSeolTown` + `LeeSeolEconomy` 연동 | 국가 기능과 돈 회수가 함께 필요 |

### 3순위: 홍보/편의

| 기능 | 권장 방식 | 이유 |
|---|---|---|
| 칭호 | 신규 `LeeSeolTitles` | TAB/채팅 placeholder와 보상 시스템 연동 |
| 웹 지도 | BlueMap 우선 검토 | 국가 클레임 overlay 가능성 |
| 로비 이스터에그 | `LeeSeolLobby` 확장 | 로비 전용 기능 |

## 신규 플러그인 우선순위

1. `LeeSeolQuest`
2. `LeeSeolCrafting`
3. `LeeSeolStations`
4. `LeeSeolJobs`
5. `LeeSeolTitles`

## LeeSeolQuest 설계 제안

### 신규 플러그인 분리 여부

추천: 신규 플러그인 `LeeSeolQuest`로 분리.

이유:

- 튜토리얼, NPC, GUI, 채굴/수확/낚시/던전/PVP 목표가 여러 시스템을 가로지른다.
- `LeeSeolCore`나 `LeeSeolEconomy`에 넣으면 책임 범위가 커진다.
- 진행 상태 저장과 PlaceholderAPI 연동이 독립적으로 필요하다.
- Citizens, ItemsAdder, LeeSeolDungeon, LeeSeolCrafting은 optional/softdepend로 붙이는 편이 안전하다.

### 추천 패키지 구조

```text
LeeSeolQuest/
  pom.xml
  src/main/resources/plugin.yml
  src/main/resources/config.yml
  src/main/java/me/leeseol/quest/LeeSeolQuestPlugin.java
  src/main/java/me/leeseol/quest/command/QuestCommand.java
  src/main/java/me/leeseol/quest/command/QuestAdminCommand.java
  src/main/java/me/leeseol/quest/model/Quest.java
  src/main/java/me/leeseol/quest/model/QuestStage.java
  src/main/java/me/leeseol/quest/model/QuestObjective.java
  src/main/java/me/leeseol/quest/model/PlayerQuestData.java
  src/main/java/me/leeseol/quest/storage/QuestStore.java
  src/main/java/me/leeseol/quest/service/QuestService.java
  src/main/java/me/leeseol/quest/gui/QuestGui.java
  src/main/java/me/leeseol/quest/listener/QuestObjectiveListener.java
  src/main/java/me/leeseol/quest/hook/QuestPlaceholderExpansion.java
  src/main/java/me/leeseol/quest/hook/VaultEconomyHook.java
  src/main/java/me/leeseol/quest/util/Text.java
```

### 데이터 저장 방식

| 데이터 | 위치 | 형식 |
|---|---|---|
| 퀘스트 정의 | `plugins/LeeSeolQuest/config.yml` | config 기반 |
| 플레이어 진행 | `/opt/minecraft/shared/quests/data.yml` | YAML, UUID 기준 |
| 완료 기록 | 같은 data file | quest id별 completed timestamp |

### config 예시

```yaml
settings:
  data-file: "/opt/minecraft/shared/quests/data.yml"
  auto-start-tutorial: true

quests:
  tutorial_start:
    display-name: "첫 여정"
    auto-start: true
    stages:
      1:
        objective:
          type: npc-dialogue
          npc-id: "guide"
        message: "안내자와 대화하세요."
      2:
        objective:
          type: server-menu-open
        message: "Shift+F 서버 메뉴를 열어보세요."
      3:
        objective:
          type: mine-block
          material: COAL_ORE
          amount: 3
        message: "석탄을 3개 캐세요."
    rewards:
      money: 1000
      commands:
        - "say %player% completed tutorial_start"
```

### 명령어 설계

| 명령어 | 권한 | 설명 |
|---|---|---|
| `/quest` | `leeseolquest.use` | 퀘스트 GUI 열기 |
| `/quest start <id>` | `leeseolquest.use` | 퀘스트 시작 |
| `/quest progress` | `leeseolquest.use` | 현재 진행 확인 |
| `/quest abandon <id>` | `leeseolquest.use` | 포기 가능한 퀘스트 포기 |
| `/tutorial start` | `leeseolquest.use` | 튜토리얼 시작 |
| `/tutorial skip` | `leeseolquest.skip` | 튜토리얼 스킵 |
| `/lsquest reload` | `leeseolquest.admin` | 설정 reload |
| `/lsquest set <player> <questId> <stage>` | `leeseolquest.admin` | 진행 상태 설정 |
| `/lsquest reset <player> [questId]` | `leeseolquest.admin` | 진행 초기화 |

### 권한 설계

| 권한 | 기본값 | 설명 |
|---|---|---|
| `leeseolquest.use` | true | 기본 퀘스트 사용 |
| `leeseolquest.skip` | true | 튜토리얼 스킵 허용 |
| `leeseolquest.admin` | op | 관리자 명령 |

### 이벤트 리스너 목록

| 리스너 | 목표 타입 |
|---|---|
| `PlayerJoinEvent` | 신규 유저 auto tutorial |
| `InventoryOpenEvent` | 특정 GUI 열기 |
| `BlockBreakEvent` | 광물 채굴 |
| `BlockDropItemEvent` 또는 `BlockBreakEvent` | 농작물 수확 |
| `PlayerFishEvent` | 낚시 성공 |
| `PlayerDeathEvent` | 플레이어 처치 |
| custom hook call | 던전 입장, 제작 완료, NPC 대화 |

### PlaceholderAPI 키

| Placeholder | 값 |
|---|---|
| `%leeseolquest_active%` | 현재 활성 퀘스트 이름 |
| `%leeseolquest_stage%` | 현재 stage 번호 |
| `%leeseolquest_objective%` | 현재 목표 설명 |
| `%leeseolquest_progress%` | `현재/필요` |
| `%leeseolquest_completed_count%` | 완료 퀘스트 수 |

### 다른 플러그인과의 연동

| 플러그인 | 방식 |
|---|---|
| Vault / LeeSeolEconomy | 보상 돈 지급 |
| Citizens | NPC 대화 목표. softdepend |
| LeeSeolDungeon | 던전 입장 목표를 API 또는 command hook으로 처리 |
| LeeSeolCrafting | 제작 완료 목표를 future hook으로 처리 |
| LeeSeolRanks | 랭크업 조건에 quest complete count 제공 |
| PlaceholderAPI | quest tracker 표시 |
| TAB | 직접 수정하지 않고 PlaceholderAPI만 제공 |

## 작업 금지/주의

| 대상 | 정책 |
|---|---|
| LuckPerms data | 수정 금지 |
| Vault/Economy balances | 직접 수정 금지. API/hook만 사용 |
| TAB 전체 설정 | 초기화 금지 |
| ItemsAdder 전체 설정 | 초기화 금지 |
| LeeSeolTown chat format | 퀘스트 작업 중 수정 금지 |
| Newworld | 건드리지 않음 |

