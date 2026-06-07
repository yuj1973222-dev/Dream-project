# LeeSeolQuest Verification

작성일: 2026-06-04 KST

## 목적

`LeeSeolQuest`가 survival/lobby 서버에서 실제로 로드되고, 기본 명령어와 공유 데이터 구조가 동작하는지 확인한다.

## 1. 배포 상태

| 항목 | 상태 | 근거 |
|---|---|---|
| survival 서비스 | PASS | `minecraft: active` |
| lobby 서비스 | PASS | `lobby: active` |
| Velocity 서비스 | PASS | `velocity: active` |
| resourcepack 서비스 | PASS | `resourcepack: active` |
| newworld 서비스 | PASS | `inactive`, 현재 의도된 상태 |
| survival jar | PASS | `/opt/minecraft/server/plugins/LeeSeolQuest-0.1.0.jar` 존재 |
| lobby jar | PASS | `/opt/minecraft/lobby/plugins/LeeSeolQuest-0.1.0.jar` 존재 |
| shared quest data | PASS | `/opt/minecraft/shared/quests/data.yml` 존재 |
| 최근 LeeSeolQuest 오류 | PASS | 최근 30분 기준 `LeeSeolQuest`, `ERROR`, `Exception`, `Could not load` 관련 출력 없음 |

## 2. RCON 상태

| 서버 | 상태 | 근거 |
|---|---|---|
| survival RCON | PASS | `enable-rcon=true`, `rcon.port=25575` |
| lobby RCON | PASS | `enable-rcon=true`, `rcon.port=25576` |
| 기존 RCON 도구 | INFO | `mc-rcon.py`, `mcrcon`, `rcon-cli` 없음 |
| 검증 방식 | PASS | 비밀번호를 출력하지 않는 일회성 Python RCON 클라이언트로 확인 |

## 3. 기본 명령어 검증

| 명령어 | survival | lobby | 결과 |
|---|---|---|---|
| `version LeeSeolQuest` | PASS | PASS | 양쪽 모두 `LeeSeolQuest version 0.1.0` 반환 |
| `lsquest reload` | PASS | PASS | 양쪽 모두 `LeeSeolQuest 설정을 다시 불러왔습니다.` 반환 |
| `list` | PASS | PASS | survival/lobby 모두 RCON 응답 정상 |

## 3-1. 2026-06-04 보완/배포 결과

| 항목 | 상태 | 결과 |
|---|---|---|
| `/quest progress` 상세화 | PASS | 퀘스트 ID, 단계, 목표, 진행도, 다음 목표, 보상 표시로 변경 |
| `harvest-crop` objective | PASS | 완전히 자란 `Ageable` 작물 파괴 시 진행 |
| `dungeon-enter` objective | PASS | 월드 변경 후 현재 월드명 기준으로 진행 |
| `npc-dialogue` objective | PASS | Citizens NPC 엔티티 우클릭 시 NPC 이름 기준으로 진행 |
| 외부 API | PASS | `LeeSeolQuestApi#progress(...)`, `activeQuestId(...)` 추가 |
| Bukkit events | PASS | `QuestStartedEvent`, `QuestObjectiveProgressEvent`, `QuestStageAdvancedEvent`, `QuestCompletedEvent` 추가 |
| 빌드 | PASS | 서버 Maven 빌드 성공, jar `44K` |
| 배포 | PASS | survival/lobby jar 교체, 기존 jar 백업: `/opt/minecraft/backups/2026-06-04_04-11-43/leeseolquest` |
| 재시작 | PASS | `minecraft`, `lobby` active, `newworld` inactive 유지 |
| RCON 검증 | PASS | survival/lobby 모두 `version LeeSeolQuest`, `lsquest reload`, `list` 정상 |

주의:

- 재시작 직후에는 Paper 시작 시간이 길어 RCON이 일시적으로 거부되었다. 이후 `Done` 확인 뒤 재검증은 통과했다.
- 최근 로그에 `LeeSeolTown`의 `/opt/minecraft/shared/town/data.yml` YAML 파싱 오류가 보인다. Quest 배포와 직접 관련 없는 기존 공유 데이터 이슈로 보이며, 이번 작업 범위에서는 수정하지 않았다.

## 4. PlaceholderAPI 검증

| Placeholder | survival | lobby | 결과 |
|---|---|---|---|
| `%leeseolquest_active%` | SKIP | PASS | `lee_seol` lobby 접속 중 `첫 여정` 반환 |
| `%leeseolquest_stage%` | SKIP | PASS | `lee_seol` lobby 접속 중 `2` 반환 |
| `%leeseolquest_objective%` | SKIP | PASS | `lee_seol` lobby 접속 중 `&b야생에서 석탄 원석을 3개 캐보세요.` 반환 |
| `%leeseolquest_progress%` | SKIP | PASS | `lee_seol` lobby 접속 중 `0/3` 반환 |
| `%leeseolquest_completed_count%` | SKIP | PASS | `lee_seol` lobby 접속 중 `0` 반환 |

2026-06-08 KST에 lobby 접속 플레이어 `lee_seol` 기준 PlaceholderAPI 파싱을 확인했다.
survival은 당시 접속자가 없어 아직 별도 확인이 필요하다.

다음에 survival 플레이어가 접속 중일 때 아래 명령어로 확인한다.

```text
papi parse <player> %leeseolquest_active%
papi parse <player> %leeseolquest_stage%
papi parse <player> %leeseolquest_objective%
papi parse <player> %leeseolquest_progress%
papi parse <player> %leeseolquest_completed_count%
```

## 5. 데이터 검증

| 항목 | 상태 | 결과 |
|---|---|---|
| shared data directory | PASS | `/opt/minecraft/shared/quests/` 존재 |
| shared data file | PASS | `/opt/minecraft/shared/quests/data.yml` 존재 |
| lobby/survival 공유 구조 | PARTIAL | 같은 shared 파일을 바라보는 구조는 확인됨. 실제 플레이어 진행 데이터 동기화는 접속자 기반 테스트 필요 |
| restart persistence | TODO | 퀘스트 데이터 변경 후 재시작 검증 필요 |

## 6. Objective 상태

| Objective | 현재 상태 | 검증 상태 | 비고 |
|---|---|---|---|
| `open-gui` | 구현됨 | TODO | 인게임 GUI 열기 테스트 필요 |
| `mine-block` | 구현됨 | TODO | 실제 블록 채굴 테스트 필요 |
| `fish` | 구현됨 | TODO | 실제 낚시 성공 이벤트 테스트 필요 |
| `kill-player` | 구현됨 | TODO | 실제 PVP 처치 또는 테스트 이벤트 필요 |
| `npc-dialogue` | 구현됨 | TODO | Citizens NPC 우클릭 실사용 테스트 필요 |
| `craft-item` | 구조만 있음 | MISSING | LeeSeolCrafting 설계 후 연동 필요 |
| `harvest-crop` | 구현됨 | TODO | 완전히 자란 작물 수확 실사용 테스트 필요 |
| `dungeon-enter` | 구현됨 | TODO | dungeon 월드 입장 실사용 테스트 필요 |
| `earn-money` | 구조만 있음 | MISSING | LeeSeolEconomy 또는 LeeSeolJobs 이벤트 연동 필요 |
| `rank-up` | 구조만 있음 | MISSING | LeeSeolRanks rankup 이벤트 연동 필요 |

## 7. 현재 결론

`LeeSeolQuest`는 survival/lobby에 배포되어 있고, 양쪽 서버에서 플러그인 버전 확인과 reload 명령어가 정상 동작한다.

아직 완료되지 않은 검증은 플레이어 접속이 필요한 항목이다.

- `/quest` GUI 열기
- `/quest start <id>`
- `/quest progress` 인게임 표시 확인
- `/quest abandon`
- `/tutorial start`
- `/tutorial skip`
- survival PlaceholderAPI 실제 파싱
- shared quest data의 진행도 저장/동기화
- objective별 실제 이벤트 반응

## 7-1. 2026-06-08 Economy 서버 메뉴 연동

`LeeSeolEconomy`의 `/servermenu`와 Shift+F 외부 DeluxeMenus 브리지 경로에 Quest soft hook을 추가했다.
메뉴가 실제로 열리면 `LeeSeolQuestApi#progress(player, "open-gui", "server-menu", 1)`를 reflection으로 호출한다.

검증 결과:

- 서버 Maven 빌드 성공, jar 내부 `plugin.yml` 확인.
- survival/lobby 기존 `LeeSeolEconomy-0.1.0.jar` 백업:
  `/opt/minecraft/backups/LeeSeolEconomy-quest-hook-2026-06-08_03-58-11`
- survival/lobby jar 교체 후 `minecraft`, `lobby` 재시작.
- 양쪽 Paper 서버 `Done` 확인.
- RCON `version LeeSeolEconomy`, `won help`, `servermenu` player-only 응답 확인.
- 배포 이후 `journalctl -u minecraft -u lobby --since "2026-06-08 03:58:00" -p err` 결과 오류 없음.

아직 남은 검증:

- 실제 플레이어가 Shift+F 또는 `/servermenu`를 열었을 때, `open-gui` target `server-menu` objective가 진행되는지 확인.

## 8. 다음 단계

1. 접속자가 있을 때 플레이어 대상 명령어와 PlaceholderAPI를 검증한다.
2. 검증 결과에 따라 `LeeSeolQuest`를 최소 수정한다.
3. 바로 구현하지 말고 `LEESEOLCRAFTING_DESIGN.md`를 작성해 제작/수리 시스템 범위를 확정한다.
