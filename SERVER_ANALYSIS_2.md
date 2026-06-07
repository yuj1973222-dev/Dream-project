# SERVER_ANALYSIS_2.md

작성일: 2026-06-03 KST

## 목적

이 문서는 기존 `SERVER_ANALYSIS.md` 이후의 2차 서버 분석 결과를 정리한다.

중점은 다음 세 가지다.

- 이미 구현된 기능을 중복 개발하지 않도록 현재 상태를 재분류한다.
- 현재 우선 버그인 ItemsAdder + TAB 로고 표시 문제의 진단 결과를 요약한다.
- 다음 신규 개발 후보인 `LeeSeolQuest`의 설계 방향을 정리한다.

이 문서는 서버 설정, 플러그인 config, 리소스팩 파일을 수정하지 않은 상태에서 작성되었다.

## 분석 기준

참고한 문서와 대상은 다음과 같다.

| 구분 | 파일 또는 대상 | 역할 |
|---|---|---|
| 1차 서버 분석 | `SERVER_ANALYSIS.md` | 배포 플러그인, 명령어, 권한, 서비스 구조 |
| 현재 서버 상태 | `SERVER_STATE.md` | 서비스명, 포트, 월드 구조, 배포 규칙 |
| 개발 로드맵 비교 | `ROADMAP_GAP_ANALYSIS.md` | 구현 상태와 기획 대비 부족 기능 |
| 로고 버그 진단 | `ITEMSADDER_TAB_LOGO_DIAGNOSIS.md` | ItemsAdder + TAB 로고 문제 분석 |
| 로컬 소스 | `LeeSeol*/src` | 커스텀 플러그인 구현 상태 확인 |
| 원격 설정 | `/opt/minecraft/...` | TAB, ItemsAdder, Velocity resource pack 설정 확인 |

## 현재 서버 구조 요약

| 영역 | 현재 상태 |
|---|---|
| 공개 진입점 | Velocity `0.0.0.0:25565` |
| Survival | Paper `minecraft`, `127.0.0.1:25566` |
| Lobby | Paper `lobby`, `127.0.0.1:25567` |
| Resource Pack | Lobby ItemsAdder output을 `resourcepack` 서비스가 `0.0.0.0:8163`에서 제공 |
| Newworld | `newworld` 서비스는 기본적으로 inactive/paused |
| Dungeon | 별도 Velocity 서버가 아니라 Survival 내부 `dungeon` 월드 |
| 권한 데이터 | LuckPerms + MariaDB |
| 경제 | LeeSeolEconomy + Vault |

중요 결정:

- Newworld는 현재 개발 대상이 아니다.
- Dungeon은 현재 Survival 내부 월드 방식으로 유지한다.
- 서버 간 이동은 Velocity backend 기준 `lobby`, `survival`, `newworld`만 사용한다.
- `dungeon`이라는 Velocity backend나 `dungeon.service`는 다시 만들지 않는다.

## 기능 상태 2차 요약

| 기능 | 상태 | 판단 |
|---|---|---|
| Velocity 이동 명령어 | DONE | `/lobby`, `/survival`, `/servers` 계열 존재 |
| 로비 보호 | DONE | `LeeSeolLobby`가 로비 보호, 스폰, 기본 제한 처리 |
| 런치패드/포탈 | DONE | `LeeSeolCore`에 인게임 설정 명령어와 config reload 존재 |
| 차원 이동 제한 | DONE | 로비/서바이벌/던전 차원 제한 구조 존재 |
| 원화 경제/Vault | DONE | `LeeSeolEconomy`가 Vault provider 역할 수행 |
| 상점 GUI/NPC 상점 | PARTIAL | 기본 구조는 있으나 고급 편집/꾸미기 과제 남음 |
| Shift+F 서버 메뉴 | DONE | `LeeSeolEconomy`에 구현됨 |
| 경매 시스템 | PARTIAL | 등록/입찰/관리 흐름 존재, UI polish와 실사용 검증 필요 |
| 내부 던전 월드 | PARTIAL | 월드/보호/포탈/루트 구조 존재, 실제 콘텐츠 세팅 필요 |
| 마을/파티/국가/연방 | PARTIAL | 소속, 청크, 채팅, 일부 전쟁 기능 존재. 국가 코어 파괴는 부족 |
| 홀로그램 | DONE | 인게임 RGB 홀로그램 관리 구조 존재 |
| 전투 태그 | DONE | 15초 전투 태그 구조 존재 |
| 전투 로그아웃 클론 | PARTIAL | Citizens 기반 구조 존재, 히트박스/처리 검증 이슈 남음 |
| 아이템 청소 | DONE | 주기적 드랍 아이템 정리 구현 |
| 랭크 저장/권한 동기화 | DONE | `LeeSeolRanks`와 LuckPerms 연동 구조 존재 |
| 랭크업 조건 확장 | PARTIAL | kill 기반은 있으나 돈/플레이타임/퀘스트/던전 조건 부족 |
| TAB 랭크/소속 표시 | DONE | PlaceholderAPI 기반 표시 구조 존재 |
| TAB 로고 이미지 표시 | BROKEN | 서버 매핑은 맞지만 클라이언트에서 유니코드 문자로 보임 |
| 튜토리얼/퀘스트 | PARTIAL | `LeeSeolQuest` 기본 버전 배포. GUI, 진행 저장, 기본 목표, PlaceholderAPI는 있음. NPC/제작/던전 연동은 추가 필요 |
| 제작/가공/강화 | MISSING | 중반 성장 루프용 제작 GUI 시스템 없음 |
| ItemsAdder 가구 클릭 GUI | MISSING | 커스텀 가구와 GUI 연결 시스템 없음 |
| Jobs 돈 수급 | MISSING | 광질/농사/낚시 기반 경제 유입 루프 없음 |
| 칭호 시스템 | MISSING | 칭호 획득/장착/표시 구조 없음 |
| 웹 지도 | MISSING | BlueMap/Dynmap 미도입 |

## ItemsAdder + TAB 로고 문제 2차 결론

현재 증상:

- TAB 플레이어 목록에서 서버 로고 이미지 대신 특수 유니코드 문자가 그대로 보인다.

확인된 서버 측 사실:

| 항목 | 확인 결과 |
|---|---|
| Resource pack host | `8163` 포트에서 정상 응답 |
| Velocity resource pack URL | `http://34.64.126.179:8163/generated.zip` |
| Velocity SHA1 | 실제 `generated.zip` SHA1과 일치 |
| Generated font JSON | `U+E301`을 로고 이미지에 매핑 |
| 로고 PNG | `assets/expedition/textures/gui/expedition_title.png` 존재 |
| TAB/Lobby 출력 문자 | `U+E301` 사용 |
| 기존 랭크 이미지 | `U+E029` ~ `U+E02D` provider 유지 |

2차 판단:

- 현재 서버 파일 기준으로는 TAB 문자가 잘못되었거나 generated pack에 로고 provider가 없는 문제는 아니다.
- 우선 원인은 클라이언트 리소스팩 미적용, 캐시, 적용 실패, 또는 TAB header 전송 타이밍 문제로 보는 것이 합리적이다.
- 따라서 ItemsAdder 전체 설정 초기화, TAB 전체 재작성, unicode 재배정은 하면 안 된다.

권장 다음 조치:

1. 클라이언트에서 서버 리소스팩을 `사용`으로 설정하고 재접속한다.
2. 그래도 문자로 보이면 클라이언트 resource pack 캐시를 지운 뒤 재접속한다.
3. 그래도 실패하면 `LeeSeolProxy`에 resource pack status 로그를 최소 추가한다.
4. resource pack 적용은 성공인데 TAB만 문자로 보이면 `LeeSeolLobby`에서 resource pack 적용 이후 TAB header를 다시 보내도록 최소 수정한다.

수정 금지:

- LuckPerms 데이터
- Vault/Economy 설정
- LeeSeolTown 데이터
- LeeSeolRanks 랭크 데이터
- TAB 전체 config 초기화
- ItemsAdder 전체 config 초기화
- 기존 unicode 값 임의 변경
- 기존 namespace/path 변경
- Newworld 서비스 또는 플러그인 폴더

## 현재 개발 우선순위

### 1순위: 신규 유저 플레이 루프 완성

| 순서 | 기능 | 권장 방식 |
|---|---|---|
| 1 | 튜토리얼/퀘스트 | 신규 `LeeSeolQuest` |
| 2 | 제작/가공/강화 | 신규 `LeeSeolCrafting` |
| 3 | ItemsAdder 가구 클릭 GUI | 신규 `LeeSeolStations` |
| 4 | 랭크업 조건 확장 | 기존 `LeeSeolRanks` 확장 |
| 5 | 광질/농사/낚시 돈 수급 | 신규 `LeeSeolJobs` |

목표 플레이 흐름:

```text
로비 접속
→ 튜토리얼 시작
→ NPC 대화
→ 야생 이동
→ 광질/농사/낚시로 돈 획득
→ 제작 시설 사용
→ 장비 제작
→ 랭크업 조건 확인
→ 던전 입장
```

### 2순위: 후반 경쟁 콘텐츠

| 기능 | 권장 방식 |
|---|---|
| PVP 포인트 | `LeeSeolCombat` 확장 또는 신규 `LeeSeolPvPPoints` |
| 플레이어 증표 | PVP 포인트와 함께 설계 |
| 국가 코어 | `LeeSeolTown` 확장 또는 신규 `LeeSeolWar` |
| 전쟁 보상/패널티 | `LeeSeolTown` 중심 확장 |
| 세금/유지비 | `LeeSeolTown` + `LeeSeolEconomy` 연동 |

### 3순위: 홍보/편의 기능

| 기능 | 권장 방식 |
|---|---|
| 칭호 | 신규 `LeeSeolTitles` |
| 로비 이스터에그 | `LeeSeolLobby` 확장 |
| 웹 지도 | BlueMap 우선 검토 |
| 스킨/NPC 외형 강화 | Citizens 연동 개선 |

## LeeSeolQuest 설계 2차 제안

### 신규 플러그인 분리 여부

추천: 신규 플러그인 `LeeSeolQuest`로 분리한다.

이유:

- 퀘스트는 NPC, GUI, 경제, 랭크, 던전, 제작 시스템을 모두 연결하는 상위 진행 시스템이다.
- `LeeSeolCore`에 통합하면 core 책임 범위가 커진다.
- 이후 `LeeSeolCrafting`, `LeeSeolJobs`, `LeeSeolDungeon`과 느슨하게 연동하기 쉽다.
- Citizens, PlaceholderAPI, Vault는 optional/softdepend로 두는 편이 안전하다.

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
| 퀘스트 정의 | `plugins/LeeSeolQuest/config.yml` | YAML |
| 플레이어 진행 상태 | `/opt/minecraft/shared/quests/data.yml` | YAML |
| 완료 기록 | 같은 data file | UUID 기준 |

공유 경로를 쓰는 이유:

- Lobby와 Survival 사이 퀘스트 진행 상태를 공유해야 한다.
- MariaDB를 바로 도입하기 전에는 YAML 공유 파일이 현재 프로젝트 스타일과 가장 가깝다.
- 동시 쓰기 위험이 커지면 이후 DB로 이전한다.

### config 예시

```yaml
settings:
  data-file: "/opt/minecraft/shared/quests/data.yml"
  auto-start-tutorial: true
  tracker-enabled: true

quests:
  tutorial_start:
    display-name: "첫 여정"
    auto-start: true
    stages:
      1:
        objective:
          type: npc-dialogue
          target: guide
        message: "&b안내자와 대화하세요."
      2:
        objective:
          type: open-gui
          target: server-menu
        message: "&b서버 메뉴를 열어보세요."
      3:
        objective:
          type: mine-block
          material: COAL_ORE
          amount: 3
        message: "&b석탄을 3개 캐세요."
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
| `/quest progress` | `leeseolquest.use` | 현재 진행도 확인 |
| `/quest abandon <id>` | `leeseolquest.use` | 퀘스트 포기 |
| `/lsquest reload` | `leeseolquest.admin` | 설정 reload |
| `/lsquest set <player> <questId> <stage>` | `leeseolquest.admin` | 진행 단계 강제 설정 |
| `/tutorial start` | `leeseolquest.use` | 튜토리얼 시작 |
| `/tutorial skip` | `leeseolquest.use` | 튜토리얼 스킵 |
| `/tutorial reset <player>` | `leeseolquest.admin` | 튜토리얼 초기화 |

### 권한 설계

| 권한 | 기본값 | 설명 |
|---|---|---|
| `leeseolquest.use` | true | 일반 퀘스트 사용 |
| `leeseolquest.admin` | op | 관리자 명령어 |
| `leeseolquest.bypass` | op | 조건 우회 |

### 이벤트 리스너 후보

| 리스너 | 목적 |
|---|---|
| `PlayerJoinEvent` | 신규 유저 튜토리얼 자동 시작 |
| `InventoryClickEvent` | 퀘스트 GUI 클릭 처리 |
| `BlockBreakEvent` | 광물 채굴 목표 |
| `BlockPlaceEvent` | 설치 목표가 필요할 경우 |
| `PlayerFishEvent` | 낚시 목표 |
| `PlayerHarvestBlockEvent` 또는 관련 Paper 이벤트 | 농작물 수확 목표 |
| `PlayerDeathEvent` | 처치 목표 |
| 커스텀 플러그인 연동 이벤트 | 제작/던전/상점/GUI 목표 |

### PlaceholderAPI 키 후보

| Placeholder | 값 |
|---|---|
| `%leeseolquest_active%` | 현재 진행 중인 퀘스트 이름 |
| `%leeseolquest_stage%` | 현재 단계 |
| `%leeseolquest_objective%` | 현재 목표 설명 |
| `%leeseolquest_progress%` | 진행도 숫자 |
| `%leeseolquest_completed_count%` | 완료 퀘스트 수 |

### 다른 플러그인과의 연동 방식

| 대상 | 방식 |
|---|---|
| Citizens | softdepend. NPC 대화 목표를 구현할 때만 사용 |
| PlaceholderAPI | softdepend. 퀘스트 트래커/TAB/scoreboard 표시 |
| Vault/LeeSeolEconomy | 보상 지급 시 Vault Economy 사용 |
| LeeSeolRanks | 랭크업 조건에서 완료 퀘스트 수 참조 |
| LeeSeolDungeon | 던전 입장/클리어 목표 연동 |
| LeeSeolCrafting | 제작 완료 목표 연동 |
| ItemsAdder | 가구/커스텀 아이템 목표는 optional 처리 |

## 구현 전 확인할 점

`LeeSeolQuest` 구현 전에 다음 선택이 필요하다.

| 항목 | 추천값 | 이유 |
|---|---|---|
| 플러그인명 | `LeeSeolQuest` | 튜토리얼보다 확장성이 넓음 |
| 첫 구현 범위 | NPC 대화, GUI 열기, 광물 채굴, 보상 지급 | 신규 유저 루프의 최소 단위 |
| 데이터 저장 | shared YAML | 현재 서버 구조에 맞고 간단함 |
| Citizens 의존 | softdepend | Citizens가 없어도 서버가 꺼지면 안 됨 |
| ItemsAdder 의존 | 아직 없음 | 첫 단계에서는 리소스팩 문제와 분리 |
| TAB 연동 | PlaceholderAPI만 제공 | TAB config 직접 수정은 나중에 |

## 다음 작업 제안

1. TAB 로고 버그를 먼저 끝낼 경우:
   - 클라이언트 리소스팩 적용 확인
   - 필요 시 `LeeSeolProxy`에 resource pack status 로그 추가
   - 필요 시 `LeeSeolLobby` TAB header 재전송 보완

2. 신규 콘텐츠 개발로 넘어갈 경우:
   - `LeeSeolQuest` 신규 모듈 생성
   - 기본 config, data store, `/quest`, `/lsquest reload` 구현
   - 신규 유저 튜토리얼 1개만 먼저 구현
   - 빌드 후 lobby/survival 중 필요한 서버에만 배포

현재 권장 순서:

```text
TAB 로고 원인 확정
→ LeeSeolQuest 최소 버전 구현
→ LeeSeolJobs로 초반 돈 수급 추가
→ LeeSeolCrafting으로 제작 루프 추가
```
