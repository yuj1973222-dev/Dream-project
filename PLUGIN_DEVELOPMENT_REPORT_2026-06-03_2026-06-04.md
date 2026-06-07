# 플러그인 개발 보고서

작성일: 2026-06-04 KST

대상 기간:

- 2026-06-03
- 2026-06-04

대상 서버:

- Velocity + Paper 구조의 `expedition` 서버
- survival Paper: `/opt/minecraft/server`, service `minecraft`
- lobby Paper: `/opt/minecraft/lobby`, service `lobby`
- newworld: inactive 유지

## 요약

이번 작업의 핵심은 “플레이 루프 기반”을 만드는 것이었다.

완료된 큰 흐름:

1. 퀘스트/튜토리얼 기반 구축
2. 광질/농사/낚시 돈 수급 시스템 구축
3. 제작/가공/분해/수리 GUI 구축
4. 랭크업 조건 확장
5. BossBar HUD 구축
6. 전투 로그아웃 시체 로직 복구
7. PVP 포인트/플레이어 증표 보상 추가

서버 측 검증은 대부분 완료되었다.

- Maven 서버 빌드 성공
- jar 배포 성공
- 서비스 재시작 성공
- RCON 명령 응답 확인
- 최근 로그 기준 신규 `ERROR`, `Exception`, `Could not load` 없음

다만 실제 플레이어가 접속해야만 확인 가능한 항목은 아직 남아 있다.

## 개발/배포 완료 플러그인

| 플러그인 | 서버 | 상태 | 핵심 기능 |
|---|---|---|---|
| `LeeSeolQuest` | survival, lobby | 서버 검증 완료 | 튜토리얼/퀘스트, 진행도 저장, PAPI, Quest API |
| `LeeSeolJobs` | survival | 서버 검증 완료 | 광질/농사/낚시 보상, 일일 제한, Quest 연동 |
| `LeeSeolCrafting` | survival | 서버 검증 완료 | 제작/가공/분해/수리 GUI, Vault 비용, Quest 연동 |
| `LeeSeolRanks` | survival, lobby | 서버 검증 완료 | 랭크업 조건 확장, 돈/플레이타임/킬 조건 |
| `LeeSeolHUD` | survival | 서버 검증 완료 | BossBar 나침반, 대상 체력바 |
| `LeeSeolCombat` | survival | 서버 검증 완료 | 전투 로그아웃 처벌, 일반 로그아웃 시체, PVP 포인트/증표 |

## 1. LeeSeolQuest

### 구현 내용

- `/quest`
- `/quest start <id>`
- `/quest progress`
- `/tutorial start`
- `/tutorial skip`
- `/lsquest reload`
- 공유 데이터 파일:
  `/opt/minecraft/shared/quests/data.yml`
- PlaceholderAPI:
  - `%leeseolquest_active%`
  - `%leeseolquest_stage%`
  - `%leeseolquest_objective%`
  - `%leeseolquest_progress%`
  - `%leeseolquest_completed_count%`
- 외부 연동 API:
  `LeeSeolQuestApi#progress(...)`
- Bukkit quest events 추가
- Objective:
  - `open-gui`
  - `mine-block`
  - `fish`
  - `kill-player`
  - `harvest-crop`
  - `dungeon-enter`
  - `npc-dialogue`

### 서버 검증

- survival/lobby 배포 완료
- RCON `version LeeSeolQuest` 통과
- RCON `lsquest reload` 통과
- shared quest data 파일 존재 확인

### 인게임 테스트 필요

1. `/quest` GUI가 열리는지 확인
2. `/quest start tutorial_start`
3. `/quest progress`
4. `/tutorial start`
5. `/tutorial skip`
6. 로비와 서바이벌 사이 이동 후 퀘스트 진행도가 공유되는지 확인
7. 서버 재시작 후 퀘스트 진행도가 유지되는지 확인
8. PlaceholderAPI 확인:

```text
papi parse lee_seol %leeseolquest_active%
papi parse lee_seol %leeseolquest_stage%
papi parse lee_seol %leeseolquest_objective%
papi parse lee_seol %leeseolquest_progress%
papi parse lee_seol %leeseolquest_completed_count%
```

9. 실제 objective 테스트:
   - 광물 캐기
   - 작물 수확
   - 낚시 성공
   - dungeon 입장
   - NPC 우클릭

## 2. LeeSeolJobs

### 구현 내용

- 광질 보상
- 설치 광물 악용 방지
- 농사 수확 보상
- 낚시 보상
- 일일 보상 제한
- 쿨다운
- Vault/LeeSeolEconomy 지급 연동
- 랭크별 보상 배율
- Quest `earn-money` objective 연동
- 명령어:

```text
/jobs
/jobs stats
/jobs top
/lsjobs status
/lsjobs reload
```

### 서버 검증

- survival 배포 완료
- RCON `version LeeSeolJobs` 통과
- RCON `lsjobs status` 통과
- RCON `lsjobs reload` 통과

### 인게임 테스트 필요

1. 자연 생성 광물 채굴
   - 기대: 돈 지급, `/jobs stats` 증가
2. 직접 설치한 광물 채굴
   - 기대: 돈 미지급
3. 완전히 자란 작물 수확
   - 기대: 돈 지급
4. 덜 자란 작물 파괴
   - 기대: 돈 미지급
5. 낚시 성공
   - 기대: 돈 지급
6. 일일 제한 초과
   - 기대: 추가 보상 미지급
7. dungeon 월드에서 보상 제한 확인
8. Quest `earn-money` 진행도 증가 확인

## 3. LeeSeolCrafting

### 구현 내용

- `/craftmenu`
- `/forge`
- `/process`
- `/disassemble`
- `/repair`
- `/lscrafting status`
- `/lscrafting reload`
- `/lscrafting recipe list`
- `/lscrafting recipe give <player> <recipeId>`
- config 기반 레시피
- Vault 비용 차감
- 성공/실패 확률
- 실패 시 재료/돈 소모 여부 config화
- 랭크 요구 조건
- 월드 제한
- Quest `craft-item` objective 연동

### 기본 레시피

| id | 타입 | 결과 | 비용 |
|---|---|---|---|
| `iron_pickaxe_basic` | crafting | `IRON_PICKAXE` | 5000원 |
| `raw_iron_processing` | processing | `IRON_INGOT` | 100원 |
| `iron_tool_disassemble` | disassemble | `IRON_NUGGET x9` | 0원 |

### 서버 검증

- survival 배포 완료
- RCON `version LeeSeolCrafting` 통과
- RCON `lscrafting status` 통과
- RCON `lscrafting recipe list` 통과

### 인게임 테스트 필요

1. `/craftmenu` GUI 열기
2. `/process` GUI 열기
3. `/disassemble` GUI 열기
4. `/repair` GUI 열기
5. 레시피 선택 후 확정 버튼으로 제작
6. 재료 부족 시 제작 차단
7. 돈 부족 시 제작 차단
8. 성공 시 돈/재료 차감 및 결과 아이템 지급
9. 실패 확률이 있는 레시피의 실패 처리 확인
10. 손상 장비를 들고 `/repair`
11. 수리 확정 시 돈 차감 및 내구도 회복
12. Quest `craft-item` 진행도 증가 확인

## 4. LeeSeolRanks

### 구현 내용

- 기존 킬 카운트 기반 랭크업 구조 확장
- `rank-up.requirements.<rank>` config 추가
- 조건:
  - 킬 수
  - 보유 돈
  - 플레이타임
- `/rank progress`
- `/rank requirements`
- `/rankup`
- `leeseolranks.bypass.requirements`
- 승급 성공 시 Quest `rank-up` objective 연동

### 현재 랭크업 조건

| 목표 랭크 | 킬 | 보유 돈 | 플레이타임 |
|---|---:|---:|---:|
| D | 10 | 0원 | 0분 |
| C | 20 | 10,000원 | 120분 |
| B | 30 | 50,000원 | 300분 |
| A | 50 | 150,000원 | 600분 |
| S | 100 | 500,000원 | 1200분 |

### 서버 검증

- survival/lobby 배포 완료
- RCON `version LeeSeolRanks` 통과
- RCON `rank requirements` 통과
- RCON `leeseolrank status` 통과
- LuckPerms 직접 DB 수정 없음

### 인게임 테스트 필요

1. `/rank progress`
2. `/rank requirements`
3. 조건 부족 상태에서 `/rankup`
   - 기대: 부족한 조건 안내
4. 조건 충족 상태에서 `/rankup`
   - 기대: 랭크 상승
5. 승급 후 킬 카운트 초기화 확인
6. LuckPerms 권한 동기화 확인
7. TAB/채팅 랭크 표시 확인
8. Quest `rank-up` 진행도 증가 확인

## 5. LeeSeolHUD

### 구현 내용

- BossBar 나침반 HUD
- 공격 대상 체력 BossBar
- `/hud`
- `/hud compass on`
- `/hud compass off`
- `/hud target on`
- `/hud target off`
- `/lshud reload`
- 월드 제한:
  - `world`
  - `world_nether`
  - `dungeon`

### 서버 검증

- survival 배포 완료
- RCON `version LeeSeolHUD` 통과
- RCON `lshud status` 통과
- RCON `lshud reload` 통과
- TAB/ItemsAdder/resource-pack 미수정

### 인게임 테스트 필요

1. survival 접속 시 나침반 BossBar가 기본 표시되는지 확인
2. `/hud`
3. `/hud compass off`
   - 기대: 나침반 BossBar 사라짐
4. `/hud compass on`
   - 기대: 나침반 BossBar 재표시
5. `/hud target off`
6. `/hud target on`
7. 몹 또는 플레이어 공격
   - 기대: 대상 체력 BossBar 표시
8. world/world_nether/dungeon 이동 시 HUD 정리 확인
9. 권한 제거 시 HUD가 보이지 않는지 확인

## 6. LeeSeolCombat

### 6-1. 전투 로그아웃/시체 로직 복구

복구 기준:

| 상황 | 기대 동작 |
|---|---|
| 전투 태그 중 접속 종료 | 즉시 사망 처벌, 시체 NPC 생성 안 함 |
| 전투 상태가 아닌 survival 종료 | Citizens 기반 누운 시체 NPC 생성 |
| 시체 NPC 처치 | 원 주인 pending death 처리 및 아이템 드랍 |

서버 검증:

- `LeeSeolCombat` 로드 확인
- `Citizens` 로드 확인
- RCON `combat status` 통과
- config:
  - `combat-logout.kill-during-combat: true`
  - `logout-clone.enabled: true`

### 6-2. PVP 포인트/증표 보상

구현 내용:

- 플레이어 처치 시 PVP 포인트 지급
- 피해자의 머리 아이템을 증표로 드랍
- 같은 유저 반복 처치 10분 쿨다운
- 같은 마을/국가 소속 처치 보상 제외
- `/combat pvp [player]`
- `/combat pvppoints <set|add|take> <player> <amount>`

저장 파일:

```text
/opt/minecraft/shared/combat/pvp.yml
```

서버 검증:

- survival 배포 완료
- RCON `version LeeSeolCombat` 통과
- RCON `combat status` 통과
- RCON `combat pvp` 사용법 응답 통과
- RCON `combat pvppoints` 사용법 응답 통과

### 인게임 테스트 필요

전투 로그아웃:

1. `/combat force <유저1> <유저2>`
2. 전투 중 한 명이 접속 종료
   - 기대: 시체 NPC 없이 즉시 처벌
3. 전투 상태가 아닌 survival 유저가 접속 종료
   - 기대: 누운 시체 NPC 생성
4. 다른 유저가 시체 NPC 처치
   - 기대: 아이템 드랍, 원 주인 다음 접속 시 pending death 처리
5. 원 주인이 시체가 죽기 전 재접속
   - 기대: 시체 제거

PVP 보상:

1. `world`에서 유저 A가 유저 B 처치
   - 기대: A의 PVP 포인트 +1
2. B의 머리 증표가 드랍되는지 확인
3. 10분 안에 A가 B를 다시 처치
   - 기대: 추가 보상 미지급
4. 같은 마을/국가 소속끼리 처치
   - 기대: 보상 미지급
5. `/combat pvp`
6. `/combat pvp <player>`
7. 관리자:

```text
/combat pvppoints add <player> 10
/combat pvppoints take <player> 5
/combat pvppoints set <player> 0
```

## 우선 테스트 순서

테스트 효율을 위해 아래 순서로 진행하는 것을 권장한다.

### 1순위: 서버 접속과 기본 명령어

```text
/quest
/jobs stats
/craftmenu
/rank progress
/hud
/combat status
```

목적:

- GUI가 정상적으로 열리는지
- 명령어 권한이 정상인지
- 플러그인 기본 응답이 정상인지 확인

### 2순위: 경제 루프

1. 광물 채굴
2. 농사 수확
3. 낚시 성공
4. `/jobs stats`
5. 돈 증가 확인

목적:

- 초반 돈 수급 루프가 실제로 작동하는지 확인

### 3순위: 제작 루프

1. 재료와 돈 준비
2. `/craftmenu`
3. 기본 곡괭이 제작
4. `/process`
5. 원석 가공
6. `/disassemble`
7. 아이템 분해
8. `/repair`

목적:

- 돈 회수와 아이템 성장 루프가 실제로 이어지는지 확인

### 4순위: 퀘스트 연동

1. `/tutorial start`
2. 광질/농사/낚시/제작 수행
3. `/quest progress`
4. PlaceholderAPI 파싱

목적:

- Jobs/Crafting/Ranks가 Quest에 진행도를 넘기는지 확인

### 5순위: 랭크업

1. `/rank requirements`
2. `/rank progress`
3. 조건 부족 상태 `/rankup`
4. 관리자 명령으로 조건을 맞춘 뒤 `/rankup`
5. LuckPerms/TAB/채팅 표시 확인

목적:

- 랭크업 조건과 권한 동기화 확인

### 6순위: HUD

1. 접속 시 나침반 BossBar 확인
2. `/hud compass off/on`
3. 대상 공격 후 체력 BossBar 확인
4. 월드 이동 시 표시 정리 확인

목적:

- 화면 표시가 과하지 않고 정상적으로 정리되는지 확인

### 7순위: Combat

2명 이상 필요.

1. 강제 전투 태그 후 접속 종료 처벌
2. 일반 로그아웃 시체 생성
3. 시체 처치
4. 실제 PVP 처치 보상
5. 증표 드랍
6. 반복 처치 쿨다운
7. 같은 소속 보상 제외

목적:

- 서버 악용 방지와 PVP 보상 루프 확인

## 현재 주의사항

- `newworld`는 계속 inactive 상태를 유지해야 한다.
- ItemsAdder, TAB 로고, BetterRanks 이미지, resource-pack SHA는 이 플러그인 개발 파트에서 건드리지 않는다.
- `LeeSeolCombat`의 반복 처치 쿨다운은 현재 런타임 메모리 기반이다. 서버 재시작 후에는 초기화된다.
- `LeeSeolTown` shared data YAML 오류는 2026-06-04 재점검 결과 현재 재현되지 않았다. 다만 town/nation 실제 생성 테스트 후 다시 확인해야 한다.
- 대부분의 남은 검증은 실제 플레이어 접속이 필요하다.

## 참고 문서

- `LEESEOLQUEST_VERIFICATION.md`
- `LEESEOLJOBS_VERIFICATION.md`
- `LEESEOLCRAFTING_VERIFICATION.md`
- `LEESEOLRANKS_REQUIREMENTS_VERIFICATION.md`
- `LEESEOLHUD_VERIFICATION.md`
- `LEESEOLCOMBAT_RESTORE_VERIFICATION.md`
- `LEESEOLCOMBAT_PVP_REWARDS_VERIFICATION.md`
- `SERVER_STATE.md`
- `TODO.md`
