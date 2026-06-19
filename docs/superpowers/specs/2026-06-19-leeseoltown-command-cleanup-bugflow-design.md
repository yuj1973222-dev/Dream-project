# LeeSeolTown Command Cleanup And Bugflow Design

## Goal

`LeeSeolTown`의 `/party` 명령어 구조를 작게 나누고, 앞으로 버그 목록을
받았을 때 한 번에 너무 많은 코드를 고치지 않도록 작업 흐름을 정한다.

이 작업은 기능을 새로 추가하는 작업이 아니라 유지보수 안정성을 높이는
정리 작업이다.

## Scope

- 1차 대상 플러그인: `LeeSeolTown`.
- 1차 대상 명령어: `/party`와 그 하위 명령.
- 유지할 명령어: 기존 `/party`, `/town`, `/village`, `/towny`, `/tc`, `/pc`,
  `/nc`.
- 유지할 권한: 기존 `leeseoltown.*` 권한 이름.
- 유지할 서버 대상: survival only.
- 함께 정리할 문서: 버그 목록을 작은 단위로 처리하는 요청/작업 규칙.

다른 플러그인 명령어 정리는 이 작업에서 직접 고치지 않는다. `LeeSeolTown`
에서 검증한 패턴을 기준으로 플러그인별 설계안을 따로 만든 뒤 진행한다.

## Current State

현재 `LeeSeolTown`의 `/party` 명령은 `TownCommand.java` 하나에 집중되어
있다. 이 파일은 다음 기능을 모두 직접 처리한다.

- 기본 파티 명령: 생성, 초대, 가입, 탈퇴, 위임, 추방, 정보.
- 영토 명령: claim, claimprice, unclaim.
- 채팅 모드 명령.
- 국가 명령: 생성, 해산, PvP, 건축 보호, 국고, 유지비, 국가 영토.
- 연방 명령.
- 전쟁 명령.
- 구조물 undo 명령.
- 중립지역 명령.
- 진단과 reload 명령.
- 위 명령들의 탭완성.

이 구조에서는 작은 버그를 고쳐도 같은 파일 안의 다른 하위 명령 분기와
탭완성을 건드리기 쉽다. 실제 서버 운영에서는 이런 변경 범위 확대가
플러그인 간 충돌처럼 보이는 간헐적 문제로 이어질 수 있다.

## Command Audit Findings

감사 기준은 세 가지다.

- 공개 등록 명령: `plugin.yml`에 등록된 root command와 alias.
- 실제 코드 분기: `TownCommand`가 처리하는 하위 명령과 숨은 alias.
- 현재 방향: `PLUGIN_INDEX.md`, `TODO.md`, survival 운영 방향에 남아 있는
  명령.

현재 `plugin.yml`의 공개 root command는 `town`이고 alias로 `party`,
`village`, `towny`가 붙어 있다. 하지만 실제 사용 방향은 `/party`다.
따라서 `/party`를 canonical command로 두고 `/town`, `/village`, `/towny`는
호환 alias 또는 제거 후보로 분리해야 한다.

현재 코드상 확실한 정리 대상은 다음과 같다.

- `/party claim`, `/party claimprice`, `/party unclaim`: 현재 서비스 로직은
  국가 소속, 국가장 권한, 국가 코어 조건을 요구한다. 파티 명령처럼 보이지만
  실제로는 국가 영토 명령이므로 canonical 위치는 `/party nation claim`,
  `/party nation claimprice`, `/party nation unclaim`이다.
- `/party federation ...`: `Federation`은 `NationType` 중 하나이고
  `disband`도 일반 국가 해산 로직을 사용한다. root의 `federation`은
  오래된 관리자 명령 형태로 보고, canonical 위치를 `/party nation
  federation ...`으로 옮긴다.
- `/party safezone ...`: 현재 명칭은 neutral zone이다. `safezone`은 오래된
  호환 alias로 보고 help와 tab completion에서는 제거한다.
- `/party diagnose`: `/party diag`의 긴 alias다. 관리자 숨김 alias로 유지할 수
  있지만 help/tab에는 `diag`만 표시한다.
- `/party nation buy`, `purchase`, `price`, `cost`, `claimcost`: 국가 영토
  명령의 편의 alias다. `claim`, `claimprice`, `unclaim`만 canonical로
  표시하고 나머지는 숨김 호환 alias로 둘지 제거할지 작업 전에 한 번 더
  확인한다.

현재 코드상 유지할 수 있는 alias는 다음과 같다.

- `/party deny`와 `/party reject`: 같은 초대 거절 의미이므로 유지 가능하다.
- `/party me`, `/party status`, `/party 소속`: 개인 소속 정보 조회이므로
  유지 가능하다. tab completion에는 `me`만 보여도 된다.
- `/party chat party`와 `/party chat town`: 내부 모델 이름은 `TOWN`이지만
  플레이어 표현은 party가 더 맞다. canonical 표시를 `party`로 두고 `town`은
  호환 alias로 유지한다.

`/party war ...`는 국가 간 전쟁 기능이지만, 전쟁은 국가 설정이 아니라 별도
행동 도메인이다. 1차 정리에서는 `/party war`를 유지한다. 사용자가 모든 국가
관련 행동을 `/party nation ...` 아래로 묶길 원하면 별도 설계안으로 분리한다.

## Command Design

`TownCommand`는 `/party`의 중앙 진입점으로만 남긴다.

역할:

- 공통 권한 `leeseoltown.use` 확인.
- 첫 번째 인자를 기준으로 하위 명령 핸들러에 전달.
- 도움말 출력.
- 기능별 핸들러의 탭완성 결과 연결.

기능별 하위 명령은 작은 클래스로 나눈다.

- `PartyCommandGroup`: create, invite, accept, deny, join, leave, disband,
  transfer, kick, info, me.
- `ClaimCommandGroup`: claim, claimprice, unclaim.
- `NationCommandGroup`: nation create, disband, pvp, build, treasury, upkeep,
  deposit, claim, claimprice, unclaim, federation.
- `WarCommandGroup`: war declare, accept, surrender, release, paydebt, finish.
- `StructureCommandGroup`: structure undo.
- `NeutralCommandGroup`: neutral pos1, pos2, create, delete, list, info.
- `AdminCommandGroup`: diag, reload.
- `TownCommandContext`: `LeeSeolTownPlugin`, `TownService`, 공통 파서, 공통
  player-only 처리, 공통 탭완성 helper.

각 그룹은 자기 하위 명령의 실행과 탭완성을 같이 가진다. 이렇게 하면
예를 들어 전쟁 명령 버그를 고칠 때 국가 생성, 중립지역, 구조물 undo
분기를 같이 읽거나 수정할 필요가 줄어든다.

## Compatibility Rules

기존 플레이어 명령어는 바꾸지 않는다.

- canonical 명령어는 유지하거나 더 명확한 위치로 옮긴다.
- 오래된 alias는 즉시 삭제하지 않고 help/tab에서 먼저 숨긴다.
- 삭제 후보 alias는 별도 목록으로 남기고 사용 여부를 확인한 뒤 제거한다.
- 메시지 key를 바꾸지 않는다.
- 권한 이름을 바꾸지 않는다.
- `plugin.yml`의 공개 명령어 구조는 `/party` canonical 방향으로 정리하되,
  기존 접속자가 쓰던 명령은 호환 alias로 남긴다.
- `/tc`, `/pc`, `/nc`는 이미 작은 `ChannelChatCommand`에 있으므로 유지한다.

명령어 도움말의 출력 순서는 현재 플레이어가 익숙한 흐름을 최대한 유지한다.

## Bugflow Design

버그 목록을 받으면 아래 순서로 처리한다.

1. 목록을 플러그인별로 분류한다.
2. 한 번에 하나의 primary plugin만 선택한다.
3. 한 번에 하나의 버그만 수정한다.
4. 수정 전에 재현 조건이나 코드상 원인을 먼저 확인한다.
5. 변경 파일을 버그와 직접 관련된 파일로 제한한다.
6. 테스트 또는 빌드로 검증한다.
7. 서버 배포가 필요한 경우 대상 플러그인 jar만 배포한다.
8. affected service만 재시작한다.
9. 최근 로그에서 changed plugin 관련 오류만 확인한다.

버그 목록이 여러 플러그인에 걸쳐 있으면 바로 전체 구현에 들어가지 않고
작업 묶음을 제안한다. 예를 들어 `LeeSeolTown`, `LeeSeolEconomy`,
`LeeSeolCombat` 문제가 섞여 있으면 세 개의 작업 단위로 나누고, 각 단위는
별도 설계 또는 짧은 작업 계획을 만든다.

## Future Plugin Cleanup

다른 플러그인 명령어 정리는 다음 기준으로 진행한다.

- 플러그인 하나를 하나의 작업 단위로 잡는다.
- 명령어 구조가 작은 플러그인은 문서 없이 바로 점검할 수 있지만, 여러
  하위 명령이 얽힌 플러그인은 먼저 설계안을 보낸다.
- 공통 명령어 프레임워크를 바로 만들지 않는다.
- 두 개 이상의 플러그인에서 같은 패턴이 반복되고 실제 중복이 확인되면
  그때 공통 helper를 추출한다.

우선순위는 운영 영향이 큰 플러그인부터 잡는다.

1. `LeeSeolTown`: 파티, 국가, 영토, 전쟁, 중립지역.
2. `LeeSeolEconomy`: 돈, 시장, 상점, Vault 연동.
3. `LeeSeolCombat`: 사망/복구/전투 보상.
4. 나머지 플러그인은 버그 목록과 운영 빈도에 따라 결정한다.

## Testing

1차 검증은 로컬 빌드와 단위 테스트로 한다.

- `mvn -pl LeeSeolTown test` 또는 현재 repo 구조에 맞는 LeeSeolTown 단독
  Maven 테스트 명령.
- 기존 `NationClaimCommandTest` 유지.
- 새로 분리한 파서나 탭완성 helper는 가능한 범위에서 단위 테스트를 추가한다.
- Bukkit 런타임이 필요한 명령 실행은 과하게 mock하지 않고, 빌드와 실제
  서버 smoke verification으로 검증한다.

배포가 필요한 경우 survival의 `minecraft` 서비스만 재시작한다.

검증 기준:

- `LeeSeolTown` jar에 `plugin.yml`이 포함된다.
- survival 서비스가 active 상태이고 `Done`에 도달한다.
- 가능한 경우 `version LeeSeolTown`이 정상 응답한다.
- 최근 로그에 `LeeSeolTown` 관련 새 `ERROR`, `Exception`, `Could not load`
  라인이 없다.

## Out Of Scope

- 전체 플러그인 공통 명령어 프레임워크 작성.
- `TownService`의 대규모 도메인 로직 재작성.
- 경제, 전투, 퀘스트 등 다른 플러그인의 동시 수정.
- GUI/resource-pack 작업.
- Chunky/BlueMap 상태 확인이나 관리.
- `newworld` 배포.

## Rollout

구현은 다음 순서로 진행한다.

1. 명령어 공통 context와 group interface를 만든다.
2. 위험이 낮은 `structure`, `diag`, `reload`부터 분리한다.
3. `neutral`처럼 상태를 가진 명령을 분리한다.
4. `war`, `federation`, `nation`을 분리한다.
5. 기본 party/claim 명령을 분리한다.
6. `TownCommand`가 라우팅과 도움말만 담당하는지 확인한다.
7. 테스트와 빌드로 검증한다.

각 단계는 가능한 한 작은 커밋 또는 작은 작업 단위로 유지한다. 중간에
문제가 생기면 마지막으로 분리한 그룹만 되돌릴 수 있어야 한다.
