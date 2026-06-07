# LeeSeolJobs Design

작성일: 2026-06-04 KST

## 목적

초반 플레이어가 광질, 농사, 낚시로 돈을 벌 수 있는 기본 경제 루프를 만든다.

목표 흐름:

```text
야생 이동
→ 광질/농사/낚시
→ 소액 보상 획득
→ 제작/수리 비용 마련
→ 랭크업 조건 진행
```

## 신규 플러그인 분리 여부

`LeeSeolJobs` 신규 Paper 플러그인으로 분리한다.

이유:

- `LeeSeolEconomy`는 돈 저장/Vault/상점이 주 책임이다.
- Jobs는 이벤트 감지, 보상 제한, 악용 방지, 통계가 주 책임이다.
- 나중에 랭크업/퀘스트와 강하게 연결되므로 별도 서비스가 낫다.

## 대상 서버

| 서버 | 적용 여부 | 이유 |
|---|---|---|
| survival | YES | 광질/농사/낚시의 주 월드 |
| lobby | NO | 로비에서는 돈 수급 불필요 |
| newworld | NO | 현재 inactive |
| velocity | NO | Paper 이벤트 기반 |

## 패키지 구조

```text
LeeSeolJobs/
  pom.xml
  src/main/resources/
    plugin.yml
    config.yml
  src/main/java/me/leeseol/jobs/
    LeeSeolJobsPlugin.java
    command/
      JobsCommand.java
      JobsAdminCommand.java
    listener/
      MiningListener.java
      FarmingListener.java
      FishingListener.java
    model/
      JobType.java
      RewardRule.java
      PlayerJobStats.java
    service/
      RewardService.java
      CooldownService.java
      BlockHistoryService.java
      StatsService.java
      EconomyService.java
    storage/
      JobsStore.java
    event/
      LeeSeolJobRewardEvent.java
    hook/
      JobsPlaceholderExpansion.java
    util/
      Text.java
```

## 주요 기능

| 기능 | 설명 |
|---|---|
| 광질 보상 | 지정 광물 채굴 시 돈 지급 |
| 농사 보상 | 완전히 자란 작물 수확 시 돈 지급 |
| 낚시 보상 | 낚시 성공 시 돈 지급 |
| 월드 제한 | `world`, `world_nether` 등 config 지정 |
| 일일 보상 제한 | 직업별/플레이어별 하루 최대 지급액 |
| 쿨다운 | 반복 이벤트 악용 방지 |
| 랭크 배율 | D/C/B/A/S 랭크에 따른 보상 배율 |
| 던전 제한 | `dungeon` 월드는 별도 설정 또는 보상 제외 |
| 경제 로그 | 돈 유입 추적 |
| Quest 연동 | 보상 획득/행동 성공 시 Quest objective 진행 |

## 명령어

| 명령어 | 권한 | 역할 |
|---|---|---|
| `/jobs` | `leeseoljobs.use` | 내 직업 통계 GUI 또는 요약 |
| `/jobs stats` | `leeseoljobs.use` | 내 보상/일일 제한 확인 |
| `/jobs top` | `leeseoljobs.use` | 누적 보상 순위 |
| `/lsjobs reload` | `leeseoljobs.admin` | 설정 리로드 |
| `/lsjobs stats <player>` | `leeseoljobs.admin` | 유저 통계 확인 |
| `/lsjobs reset <player>` | `leeseoljobs.admin` | 유저 통계 초기화 |

## 권한

```text
leeseoljobs.use
leeseoljobs.admin
leeseoljobs.bypass.cooldown
leeseoljobs.bypass.daily-limit
leeseoljobs.multiplier.<id>
```

## Config 예시

```yaml
settings:
  enabled: true
  allowed-worlds:
    - world
    - world_nether
  blocked-worlds:
    - dungeon
  save-interval-seconds: 60

economy:
  provider: vault
  log-rewards: true

daily-limits:
  mining: 50000
  farming: 30000
  fishing: 30000

rank-multipliers:
  PLAYER: 1.0
  D: 1.05
  C: 1.10
  B: 1.20
  A: 1.35
  S: 1.50

mining:
  enabled: true
  cooldown-millis: 250
  anti-place-abuse:
    enabled: true
    remember-minutes: 30
  rewards:
    COAL_ORE: 20
    DEEPSLATE_COAL_ORE: 25
    IRON_ORE: 40
    DEEPSLATE_IRON_ORE: 50
    GOLD_ORE: 70
    DIAMOND_ORE: 250

farming:
  enabled: true
  require-fully-grown: true
  rewards:
    WHEAT: 8
    CARROTS: 8
    POTATOES: 8
    BEETROOTS: 8
    NETHER_WART: 12

fishing:
  enabled: true
  cooldown-millis: 1000
  rewards:
    default: 30
    treasure: 150
```

## 데이터 저장

경로:

```text
/opt/minecraft/shared/jobs/data.yml
```

저장 항목:

```yaml
players:
  <uuid>:
    name: lee_seol
    totals:
      mining: 12000
      farming: 4500
      fishing: 3000
    daily:
      date: "2026-06-04"
      mining: 3000
      farming: 500
      fishing: 0
```

1차는 YAML로 충분하다. 데이터가 커지면 MariaDB 전환을 검토한다.

## 악용 방지

### 광물 설치 후 재채굴

문제:

- 플레이어가 광물을 설치하고 다시 캐면 돈을 무한 생성할 수 있다.

대응:

- `BlockPlaceEvent`에서 보상 대상 블록 위치를 일정 시간 기록.
- 해당 위치에서 채굴하면 보상 지급 안 함.
- 재시작 후 기록 유지가 필요하면 `placed-blocks.yml` 추가.

### 자동 농장

문제:

- 자동 수확 장치로 돈이 과도하게 생성될 수 있다.

대응:

- 플레이어가 직접 부순 `BlockBreakEvent`만 보상.
- 완전히 자란 작물만 보상.
- 일일 제한 적용.

### 낚시 매크로

문제:

- 장시간 자동 낚시로 돈 생성 가능.

대응:

- 일일 제한.
- 보상 쿨다운.
- 같은 위치 장시간 반복은 후순위 탐지 후보.

## Vault 연동

보상 지급은 Vault Economy를 사용한다.

직접 `LeeSeolEconomy` 저장소를 수정하지 않는다.

지급 실패 시:

- 유저에게 메시지 전송
- 로그 1회 기록
- 통계 증가 없음

## LeeSeolQuest 연동

`LeeSeolJobs`는 보상 지급 성공 시 이벤트를 발행한다.

```java
LeeSeolJobRewardEvent(player, JobType.MINING, materialName, amount)
```

그리고 `LeeSeolQuestApi#progress(...)`를 선택적으로 호출한다.

예시:

```java
LeeSeolQuestApi.progress(player, "earn-money", "jobs", rewardAmount);
```

주의:

- `LeeSeolQuest`가 없어도 Jobs는 로드되어야 한다.
- API 호출은 soft 연동으로 처리한다.

## LeeSeolRanks 연동

랭크별 보상 배율은 권한 기준으로 먼저 처리한다.

예:

```text
leeseolranks.rank.d
leeseolranks.rank.c
leeseolranks.rank.b
leeseolranks.rank.a
leeseolranks.rank.s
```

추후 `LeeSeolRanks` API가 생기면 직접 조회로 바꿀 수 있다.

## PlaceholderAPI

| Placeholder | 의미 |
|---|---|
| `%leeseoljobs_daily_mining%` | 오늘 광질 보상 |
| `%leeseoljobs_daily_farming%` | 오늘 농사 보상 |
| `%leeseoljobs_daily_fishing%` | 오늘 낚시 보상 |
| `%leeseoljobs_total_money%` | 누적 Jobs 보상 |

## 구현 순서

1. 플러그인 골격 생성
2. config 로더 작성
3. Vault economy hook
4. mining/farming/fishing 리스너
5. 일일 제한/쿨다운
6. 광물 설치 악용 방지
7. 통계 저장
8. `/jobs stats`, `/lsjobs reload`
9. Quest API soft 연동
10. survival 배포 후 실제 보상 테스트

## 검증 기준

| 테스트 | 기대 결과 |
|---|---|
| 광물 채굴 | 돈 지급, 통계 증가 |
| 설치한 광물 채굴 | 돈 미지급 |
| 완전히 자란 작물 수확 | 돈 지급 |
| 덜 자란 작물 파괴 | 돈 미지급 |
| 낚시 성공 | 돈 지급 |
| 일일 제한 초과 | 돈 미지급 |
| dungeon 월드 | 기본 보상 미지급 |
| Vault 없음 | 서버 크래시 없이 기능 비활성 |
| reload | 보상표 반영 |
