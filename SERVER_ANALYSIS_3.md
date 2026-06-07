# SERVER_ANALYSIS_3.md

작성일: 2026-06-04 KST

## 목적

이 문서는 현재 `expedition` 서버의 3차 분석 결과다.

이전 문서와의 차이:

- `LeeSeolQuest` 기본 플러그인이 실제 배포된 상태를 반영한다.
- `LeeSeolRanks`의 ADMIN/DEV 권한 정책 변경을 반영한다.
- ItemsAdder + TAB 로고 문제는 리소스팩 채팅 담당으로 분리한다.
- 앞으로 플러그인 개발 채팅과 리소스팩 이미지 채팅을 분리해서 운영한다.

## 채팅 역할 분리

| 채팅 | 담당 |
|---|---|
| 플러그인 개발 채팅 | Java 플러그인, 명령어, GUI, 권한, 데이터 저장, 배포/검증 |
| 리소스팩 이미지 채팅 | 생성 이미지, ItemsAdder, font image, generated.zip, TAB 로고, SHA1 |

공통 원칙:

- 두 채팅 모두 `SERVER_STATE.md`와 `PLUGIN_INDEX.md`를 기준으로 한다.
- 리소스팩 채팅은 플러그인 코드를 수정하지 않는다.
- 플러그인 개발 채팅은 ItemsAdder/TAB 리소스팩 설정을 임의 수정하지 않는다.
- Newworld는 명시 요청 전까지 건드리지 않는다.

## 현재 서버 구조

| 항목 | 상태 |
|---|---|
| VM | Google Cloud `minecraft-server` |
| Zone | `asia-northeast3-a` |
| Machine | `e2-standard-2` |
| Proxy | Velocity `0.0.0.0:25565` |
| Survival | Paper `minecraft`, `127.0.0.1:25566` |
| Lobby | Paper `lobby`, `127.0.0.1:25567` |
| Resource pack host | `resourcepack`, `0.0.0.0:8163` |
| Newworld | `newworld`, inactive/paused |
| Dungeon | Survival 내부 `dungeon` 월드 |
| 권한 저장 | LuckPerms + MariaDB |
| 경제 | LeeSeolEconomy + Vault |

## 현재 서비스 기대 상태

```text
velocity: active
resourcepack: active
minecraft: active
lobby: active
newworld: inactive
```

`newworld`는 향후 신세계 개발용으로 유지한다. 현재 던전은 Survival 내부 월드 방식이다.

## 현재 커스텀 플러그인 상태

| 플러그인 | 배포 대상 | 상태 | 역할 |
|---|---|---|---|
| `LeeSeolProxy` | Velocity | DONE | 서버 이동 명령어, 리소스팩 전송, 리소스팩 상태 로그 |
| `LeeSeolCore` | survival/lobby | DONE | 포탈, 런치패드, 차원 제한, 기본 명령어 |
| `LeeSeolEconomy` | survival/lobby | PARTIAL | 원화 경제, Vault, 상점, NPC, Shift+F 메뉴 |
| `LeeSeolAuction` | survival/lobby | PARTIAL | 경매 등록/입찰/관리 흐름 |
| `LeeSeolDungeon` | survival | PARTIAL | 내부 dungeon 월드, 포탈, 보호, 루트 상자 |
| `LeeSeolLobby` | lobby | DONE | 로비 보호, 스폰, TAB header 직접 전송 |
| `LeeSeolTown` | survival/lobby | PARTIAL | 파티/국가/연방, 클레임, 채팅, 일부 전쟁 구조 |
| `LeeSeolHologram` | survival/lobby | DONE | RGB 홀로그램 |
| `LeeSeolCombat` | survival | PARTIAL | 전투 태그, 로그아웃 클론, 히트박스 보완 필요 |
| `LeeSeolCleanup` | survival | DONE | 드랍 아이템 주기 청소 |
| `LeeSeolRanks` | survival/lobby | DONE/PARTIAL | 랭크 저장, LuckPerms 동기화, ADMIN/DEV staff 권한 |
| `LeeSeolQuest` | survival/lobby | PARTIAL | 기본 퀘스트, GUI, 진행 저장, PlaceholderAPI |

## Resource Pack 상태

리소스팩은 현재 별도 보호 대상이다.

현재 구조:

- public URL: `http://34.64.126.179:8163/generated.zip`
- hosting service: `resourcepack`
- served path: `/opt/minecraft/lobby/plugins/ItemsAdder/output`
- Velocity가 네트워크 접속 시 resource pack offer 전송
- Paper backend의 `server.properties` resource-pack 필드는 비워둔다

현재 문서상 SHA1:

```text
8291368cc9c1cbb14872897a578fa42071e85e6e
```

현재 리소스팩에 포함된 주요 UI:

- BetterRanks 랭크 이미지
- `expedition` TAB title glyph
- TAB ping icon 투명화 리소스

주의:

- 플러그인 개발 중에는 ItemsAdder/TAB/generated.zip/SHA1을 수정하지 않는다.
- 리소스팩 변경은 리소스팩 이미지 채팅에서 별도로 처리한다.

## LeeSeolQuest 현재 상태

배포:

- survival: `/opt/minecraft/server/plugins/LeeSeolQuest-0.1.0.jar`
- lobby: `/opt/minecraft/lobby/plugins/LeeSeolQuest-0.1.0.jar`

데이터:

```text
/opt/minecraft/shared/quests/data.yml
```

명령어:

```text
/quest
/quest start <id>
/quest progress
/quest abandon
/tutorial start
/tutorial skip
/tutorial reset <player>
/lsquest reload
/lsquest set <player> <questId> <stage>
/lsquest advance <player>
/lsquest objective <player> <type> [target]
```

PlaceholderAPI:

```text
%leeseolquest_active%
%leeseolquest_stage%
%leeseolquest_objective%
%leeseolquest_progress%
%leeseolquest_completed_count%
```

현재 판단:

- 기본 구조는 완료.
- 실제 플레이 루프 검증은 아직 문서화 필요.
- 다음 문서로 `LEESEOLQUEST_VERIFICATION.md`를 작성해야 한다.

## LeeSeolRanks 현재 상태

현재 shared rank file:

```text
/opt/minecraft/shared/ranks/ranks.yml
```

현재 기대 기록:

```text
lee_seol=ADMIN
YamiyongO_o=DEV
```

권한 정책:

- ADMIN과 DEV는 같은 staff admin permission 목록을 받는다.
- 현재 기본값은 LuckPerms wildcard `*`.
- 실제 `/op` 플래그가 아니라 LuckPerms 기반 OP급 권한이다.

관련 config:

```yaml
staff:
  admin-permissions:
    - "*"
```

주의:

- LuckPerms 데이터 직접 DB 수정 금지.
- 랭크 데이터 구조 변경 금지.
- 한 유저는 하나의 `LeeSeolRanks` 랭크만 가진다는 원칙 유지.

## 현재 DONE 기능

| 기능 | 상태 |
|---|---|
| Velocity 서버 이동 | DONE |
| Resource pack status logging | DONE |
| 로비 보호 | DONE |
| 런치패드/포탈 기본 구조 | DONE |
| 차원 제한 | DONE |
| 원화 경제/Vault provider | DONE |
| Shift+F 서버 메뉴 | DONE |
| 홀로그램 | DONE |
| 전투 태그 | DONE |
| 아이템 청소 | DONE |
| 랭크 저장/PlaceholderAPI | DONE |
| ADMIN/DEV staff 권한 동기화 | DONE |
| LeeSeolQuest 기본 생성/배포 | DONE |

## 현재 PARTIAL 기능

| 기능 | 남은 작업 |
|---|---|
| LeeSeolQuest | 실제 명령어/GUI/data 공유 검증, NPC/제작/던전 목표 |
| LeeSeolEconomy shop | UI polish, 고급 편집 |
| LeeSeolAuction | 실사용 검증, UI polish |
| LeeSeolDungeon | 포탈/루트 위치 실세팅 |
| LeeSeolTown | 국가 코어, 전쟁 후반 시스템 |
| LeeSeolCombat | 로그아웃 클론 히트박스/처리 검증 |
| LeeSeolRanks rankup | 돈/퀘스트/던전/제작 조건 확장 |

## 현재 MISSING 기능

| 기능 | 권장 방식 |
|---|---|
| LeeSeolCrafting | 신규 플러그인 설계 먼저 |
| LeeSeolStations | ItemsAdder API 검토 후 신규 플러그인 |
| LeeSeolJobs | 신규 플러그인 |
| 칭호 시스템 | 신규 `LeeSeolTitles` |
| 웹 지도 | BlueMap 우선 검토 |

## 다음 작업 순서

플러그인 개발 채팅:

1. `LEESEOLQUEST_VERIFICATION.md` 작성
2. `LeeSeolQuest` 인게임/콘솔 검증
3. 부족한 `LeeSeolQuest` 기능만 최소 보완
4. `LEESEOLCRAFTING_DESIGN.md` 작성
5. 설계 승인 전까지 `LeeSeolCrafting` 전체 구현 보류

리소스팩 이미지 채팅:

1. 생성 이미지 정리
2. ItemsAdder font image 적용
3. generated.zip 검증
4. SHA1 갱신
5. 클라이언트 적용 확인

## 현재 금지 작업

- Newworld 재활성화
- ItemsAdder 전체 초기화
- TAB 전체 재작성
- LuckPerms DB 직접 수정
- LeeSeolRanks 데이터 구조 변경
- 국가 코어/전쟁 대규모 구현
- 세금/칭호/웹지도 구현
- 리소스팩 채팅 없이 generated.zip 구조 변경

