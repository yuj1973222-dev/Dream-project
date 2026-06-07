# LeeSeolRanks Rankup Expansion Design

작성일: 2026-06-04 KST

## 목적

기존 `LeeSeolRanks`를 유지하면서 `/rankup` 조건을 킬 수 하나에서 돈, 플레이타임, 퀘스트, 제작, 던전, PVP 포인트 기반 조건으로 확장한다.

신규 플러그인으로 분리하지 않는다.

## 현재 상태

| 항목 | 상태 |
|---|---|
| 랭크 순서 | `PLAYER -> D -> C -> B -> A -> S` |
| 관리자 랭크 | `ADMIN`, `DEV` |
| 현재 승급 조건 | 다음 랭크별 킬 수 |
| 승급 시 처리 | rank 변경, kills 0 초기화, LuckPerms 동기화 |
| 저장 파일 | `/opt/minecraft/shared/ranks/ranks.yml` |
| 중요 원칙 | 한 유저당 하나의 LeeSeolRanks rank |

현재 config:

```yaml
rank-up:
  thresholds:
    D: 10
    C: 20
    B: 30
    A: 50
    S: 100
```

## 설계 원칙

1. 기존 rank data 구조를 바로 변경하지 않는다.
2. LuckPerms DB 직접 수정 금지.
3. `/rankup` 흐름은 유지한다.
4. 부족 조건을 명확히 보여준다.
5. 외부 플러그인 데이터는 soft 연동한다.
6. 외부 데이터가 없으면 해당 조건은 실패 또는 비활성 처리한다.

## Config 확장안

```yaml
rank-up:
  requirements:
    D:
      kills: 10
      money: 0
      playtime-minutes: 0
      completed-quests: []
      crafting-count: 0
      dungeon-clears: 0
      pvp-points: 0
    C:
      kills: 20
      money: 10000
      playtime-minutes: 120
      completed-quests:
        - tutorial_start
      crafting-count: 5
      dungeon-clears: 0
      pvp-points: 0
    B:
      kills: 30
      money: 50000
      playtime-minutes: 300
      completed-quests: []
      crafting-count: 20
      dungeon-clears: 1
      pvp-points: 0
    A:
      kills: 50
      money: 150000
      playtime-minutes: 600
      completed-quests: []
      crafting-count: 50
      dungeon-clears: 5
      pvp-points: 100
    S:
      kills: 100
      money: 500000
      playtime-minutes: 1200
      completed-quests: []
      crafting-count: 100
      dungeon-clears: 10
      pvp-points: 300
```

기존 `thresholds`는 하위 호환용으로 유지한다. `requirements`가 없으면 기존 thresholds 방식으로 동작한다.

## 명령어 확장

| 명령어 | 권한 | 역할 |
|---|---|---|
| `/rankup` | `leeseolranks.use` | 조건 충족 시 승급 |
| `/rank progress` | `leeseolranks.use` | 다음 랭크까지 조건 표시 |
| `/rank requirements` | `leeseolranks.use` | 랭크별 요구 조건 확인 |
| `/rankadmin points <player> <set|add|take> <amount>` | `leeseolranks.admin` | PVP/랭크 포인트 조정 |
| `/rankadmin resetcooldown <player>` | `leeseolranks.admin` | 반복 처치 제한 초기화 |

## 권한

```text
leeseolranks.use
leeseolranks.admin
leeseolranks.bypass.requirements
```

ADMIN/DEV는 기존 wildcard 권한 정책으로 우회 가능하다.

## 조건 체크 서비스

추가 구조:

```text
service/
  RankRequirementService.java
  RequirementResult.java
  RequirementLine.java
hook/
  EconomyHook.java
  QuestHook.java
  CraftingHook.java
  DungeonHook.java
  PvpPointHook.java
```

`RankUpCommand`는 직접 조건을 계산하지 않고 `RankRequirementService`에 위임한다.

## 데이터 저장 확장

기존 `ranks.yml`에는 rank/kills만 유지한다.

추가 데이터가 필요하면 별도 파일로 분리한다.

```text
/opt/minecraft/shared/ranks/progress.yml
```

저장 후보:

```yaml
players:
  <uuid>:
    pvp-points: 0
    dungeon-clears: 0
    crafting-count: 0
    last-kill-times:
      <victim-uuid>: 2026-06-04T04:00:00
```

이렇게 하면 기존 `ranks.yml` 마이그레이션 위험을 줄일 수 있다.

## 외부 연동

### Vault / LeeSeolEconomy

돈 조건은 Vault Economy balance로 확인한다.

주의:

- 승급 시 돈을 차감할지 여부는 별도 결정 필요.
- 1차는 “보유 조건”만 권장한다.

### LeeSeolQuest

퀘스트 완료 조건은 `LeeSeolQuestApi` 또는 이벤트 기반 데이터로 확인한다.

현재 `LeeSeolQuestApi#activeQuestId(...)`만 있으므로, 완료 퀘스트 조회 API가 추가로 필요하다.

추가 후보:

```java
LeeSeolQuestApi#hasCompleted(UUID playerId, String questId)
LeeSeolQuestApi#completedCount(UUID playerId)
```

### LeeSeolCrafting

제작 횟수 조건은 `LeeSeolCraftSuccessEvent`를 받아 별도 progress 데이터에 누적한다.

### LeeSeolDungeon

던전 입장/클리어 조건은 `LeeSeolDungeon`이 이벤트를 발행해야 한다.

추가 후보:

```java
LeeSeolDungeonEnterEvent
LeeSeolDungeonClearEvent
```

### LeeSeolCombat / PVP 포인트

PVP 포인트 조건은 별도 PVP 포인트 시스템과 연결한다.

주의:

- 같은 마을/국가 소속 킬은 포인트 미지급.
- 같은 플레이어 반복 처치 10분 쿨타임.
- 전투 로그아웃 clone 처치와 일반 플레이어 처치를 어떻게 계산할지 별도 결정 필요.

## `/rank progress` 표시 예시

```text
[랭크] 다음 랭크: C
- 킬: 12 / 20
- 보유 돈: 8,500 / 10,000원
- 플레이타임: 95 / 120분
- 퀘스트 tutorial_start: 완료
- 제작 횟수: 2 / 5
```

## 구현 순서

1. `requirements` config 로더 추가
2. 기존 thresholds 하위 호환 유지
3. `RankRequirementService` 추가
4. `/rank progress`, `/rank requirements` 추가
5. Vault 돈 조건 확인
6. 플레이타임 조건 확인
7. Quest 완료 조회 API 추가 후 연결
8. Crafting/Dungeon/PVP는 해당 시스템 구현 후 연결
9. 부족 조건 메시지 정리
10. 실사용 검증

## 검증 기준

| 테스트 | 기대 결과 |
|---|---|
| 기존 thresholds만 있는 config | 기존 방식으로 `/rankup` 동작 |
| requirements 있는 config | 복합 조건 체크 |
| 돈 부족 | 승급 실패, 부족 금액 표시 |
| 킬 부족 | 승급 실패, 부족 킬 표시 |
| 최고 랭크 | 기존 `info-max` 유지 |
| ADMIN/DEV | 조건 우회 가능 |
| LuckPerms sync | 승급 후 기존처럼 동기화 |
| 데이터 구조 | 기존 `ranks.yml` rank/kills 유지 |
