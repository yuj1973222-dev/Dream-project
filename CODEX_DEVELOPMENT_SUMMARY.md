# CODEX_DEVELOPMENT_SUMMARY.md

작성일: 2026-06-04 KST

## 목적

이 문서는 현재 채팅에서 Codex가 직접 개발, 수정, 배포, 검증한 부분을 정리한다.

앞으로 작업 채팅을 두 갈래로 나눌 때 기준은 다음과 같다.

| 채팅 | 담당 범위 |
|---|---|
| 플러그인 개발 채팅 | Java 플러그인 개발, 명령어, GUI, 권한, 데이터 저장, 서버 배포/검증 |
| 리소스팩 이미지 채팅 | 생성 이미지, ItemsAdder 리소스팩, font image, generated.zip, TAB 로고 이미지 적용 |

이 문서는 플러그인 개발 채팅에서 이어받을 개발 이력을 기록한다.

## 직접 개발/수정한 주요 항목

### 1. LeeSeolProxy 리소스팩 상태 로그

수정 파일:

- `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`

추가 내용:

- Velocity resource pack offer 전송 로그 추가
- `PlayerResourcePackStatusEvent` 기반 리소스팩 상태 로그 추가
- 유저별 리소스팩 적용 상태를 콘솔에서 확인 가능하게 함

기대 로그:

```text
Sent network resource pack offer to <player>
Resource pack status for <player>: SUCCESSFUL
Resource pack status for <player>: FAILED_DOWNLOAD
Resource pack status for <player>: DECLINED
```

검증 결과:

- Velocity 빌드/배포 완료
- `velocity` 서비스 active 확인
- 리소스팩 URL 응답 확인
- Velocity 설정 SHA1과 실제 `generated.zip` SHA1 일치 확인
- `LeeSeolProxy` 정상 로드 확인

주의:

- `LeeSeolProxy`의 resource pack URL/SHA1 처리 구조는 리소스팩 채팅에서만 신중하게 수정한다.
- 플러그인 개발 채팅에서는 리소스팩 관련 값 자체를 임의 변경하지 않는다.

## 2. LeeSeolQuest 신규 플러그인

생성 폴더:

- `LeeSeolQuest/`

주요 파일:

- `LeeSeolQuest/pom.xml`
- `LeeSeolQuest/src/main/resources/plugin.yml`
- `LeeSeolQuest/src/main/resources/config.yml`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/LeeSeolQuestPlugin.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/service/QuestService.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/storage/QuestStore.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/gui/QuestGui.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/listener/QuestObjectiveListener.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/hook/QuestPlaceholderExpansion.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/command/QuestCommand.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/command/TutorialCommand.java`
- `LeeSeolQuest/src/main/java/me/leeseol/quest/command/QuestAdminCommand.java`

배포 대상:

- Survival: `/opt/minecraft/server/plugins/LeeSeolQuest-0.1.0.jar`
- Lobby: `/opt/minecraft/lobby/plugins/LeeSeolQuest-0.1.0.jar`

공유 데이터:

```text
/opt/minecraft/shared/quests/data.yml
```

현재 구현된 명령어:

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

현재 구현된 PlaceholderAPI:

```text
%leeseolquest_active%
%leeseolquest_stage%
%leeseolquest_objective%
%leeseolquest_progress%
%leeseolquest_completed_count%
```

현재 objective 구조:

| Objective | 상태 | 비고 |
|---|---|---|
| `open-gui` | 구현 | `/quest` GUI 열기 감지 |
| `mine-block` | 구현 | `BlockBreakEvent` 기반 |
| `fish` | 구현 | `PlayerFishEvent.State.CAUGHT_FISH` 기반 |
| `kill-player` | 구현 | `PlayerDeathEvent` killer 기반 |
| `npc-dialogue` | 구조만 있음 | Citizens 연동 미구현 |
| `craft-item` | 구조만 있음 | LeeSeolCrafting 이후 연동 예정 |
| `harvest-crop` | 구조만 있음 | 수확 이벤트 구현 필요 |
| `dungeon-enter` | 구조만 있음 | LeeSeolDungeon 연동 필요 |

검증 결과:

- 서버 빌드 성공
- survival/lobby 배포 완료
- survival/lobby 모두 `LeeSeolQuest enabled`
- survival/lobby 모두 `Done`
- 오류/Exception/Could not load 없음
- Newworld 미수정

현재 부족한 부분:

- 인게임 실사용 검증 문서 필요: `LEESEOLQUEST_VERIFICATION.md`
- NPC 대화 목표
- 제작 목표
- 던전 입장 목표
- 농작물 수확 목표
- 퀘스트 트래커 UI 고도화
- `/quest progress` 상세 안내 강화

## 3. LeeSeolRanks DEV 권한 강화

수정 파일:

- `LeeSeolRanks/src/main/resources/config.yml`
- `LeeSeolRanks/src/main/java/me/leeseol/ranks/storage/PermissionService.java`
- `LeeSeolRanks/src/main/java/me/leeseol/ranks/listener/RankListener.java`

추가 내용:

- `staff.admin-permissions` 설정 추가
- 기본값을 LuckPerms wildcard `*`로 설정
- `ADMIN`과 `DEV`가 동일한 staff 관리자 권한 목록을 받도록 처리
- 접속 시 랭크 권한을 LuckPerms에 다시 동기화하도록 보완

현재 설정:

```yaml
staff:
  admin-permissions:
    - "*"
```

적용 대상:

```text
lee_seol=ADMIN
YamiyongO_o=DEV
```

검증 결과:

- `LeeSeolRanks` 빌드/배포 완료
- survival/lobby 모두 active
- survival/lobby 모두 `LeeSeolRanks enabled`
- survival/lobby 모두 `Done`
- RCON으로 `lee_seol`, `YamiyongO_o`에게 LuckPerms `*` 권한 즉시 동기화 실행

정확한 의미:

- Minecraft `/op` 플래그를 켠 것이 아니다.
- LuckPerms 기준 OP급 전체 권한인 `*`을 부여한 방식이다.
- 권한 회수는 `staff.admin-permissions` 목록과 랭크 동기화 흐름을 기준으로 처리된다.

## 4. 문서 갱신

수정/생성한 문서:

- `PLUGIN_INDEX.md`
- `SERVER_STATE.md`
- `TODO.md`
- `SERVER_ANALYSIS_2.md`
- `ITEMSADDER_TAB_LOGO_DIAGNOSIS.md`
- `ROADMAP_GAP_ANALYSIS.md`
- `SERVER_ANALYSIS_2.md`

문서화된 내용:

- 서버 구조
- 플러그인 배포 대상
- Newworld 미수정 원칙
- Resource pack 보호 원칙
- LeeSeolQuest 상태
- LeeSeolRanks ADMIN/DEV 권한 정책

## 현재 플러그인 개발 채팅의 다음 작업

우선순위:

1. `LEESEOLQUEST_VERIFICATION.md` 작성
2. `LeeSeolQuest` 명령어/GUI/데이터 공유/PlaceholderAPI 실사용 검증
3. 검증 결과 기반으로 부족한 기능만 최소 보완
4. `LEESEOLCRAFTING_DESIGN.md` 작성
5. 설계 승인 전까지 `LeeSeolCrafting` 전체 구현은 시작하지 않음

## 현재 리소스팩 이미지 채팅의 담당 작업

리소스팩 채팅에서만 다룰 내용:

- 생성 이미지 제작/수정
- ItemsAdder font image 설정
- `generated.zip` 생성/검증
- Velocity resource pack SHA1 갱신
- TAB 로고 이미지 표시 검증
- BetterRanks 이미지 리소스팩 적용

플러그인 개발 채팅에서는 위 영역을 직접 수정하지 않는다.

