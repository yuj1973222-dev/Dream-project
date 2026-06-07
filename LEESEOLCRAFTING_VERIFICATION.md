# LEESEOLCRAFTING_VERIFICATION.md

작성일: 2026-06-04 KST

## 구현 범위

`LeeSeolCrafting` 0.1.0을 신규 survival 전용 Paper 플러그인으로 구현했다.

현재 기능:

- `/craftmenu`, `/forge`: 제작 GUI
- `/process`: 가공 GUI
- `/disassemble`: 분해 GUI
- `/repair`: 손에 든 손상 장비 돈 기반 수리 확인 GUI
- `/lscrafting status`
- `/lscrafting reload`
- `/lscrafting recipe list`
- `/lscrafting recipe give <player> <recipeId>`
- config 기반 레시피
- 제작 비용 Vault 연동
- 성공/실패 확률
- 실패 시 돈/재료 소모 여부 config화
- 랭크 요구 조건 확인
- 월드 제한
- Quest `craft-item` objective soft hook

## 배포 대상

| 항목 | 값 |
| --- | --- |
| 서버 | survival Paper |
| 서비스 | `minecraft` |
| 배포 jar | `/opt/minecraft/server/plugins/LeeSeolCrafting-0.1.0.jar` |
| 백업 | `/opt/minecraft/backups/2026-06-04_04-46-20/leeseolcrafting` |
| newworld | inactive 유지 |

## 서버 검증 결과

확인 완료:

- Maven server-side build 성공
- jar 내부 `plugin.yml` 존재 확인
- survival 서비스 재시작 성공
- `minecraft` 서비스 active 확인
- `newworld` inactive 확인
- 최근 로그에서 `LeeSeolCrafting v0.1.0` 로드 확인
- 최근 로그에서 `Loaded 3 crafting recipes.` 확인
- 최근 로그에서 `LeeSeolCrafting enabled. recipes=3` 확인
- 최근 로그에서 `Done` 확인

RCON 확인:

- 기존 `~/mc-rcon.py` 파일은 없었지만, 임시 RCON 클라이언트로 검증했다.
- `version LeeSeolCrafting` 정상 응답
- `lscrafting status` 정상 응답, 레시피 3개 확인
- `lscrafting recipe list` 정상 응답

## 현재 기본 레시피

| id | 타입 | 결과 | 비용 |
| --- | --- | --- | --- |
| `iron_pickaxe_basic` | crafting | `IRON_PICKAXE` | 5000원 |
| `raw_iron_processing` | processing | `IRON_INGOT` | 100원 |
| `iron_tool_disassemble` | disassemble | `IRON_NUGGET x9` | 0원 |

## 인게임 테스트 필요 항목

접속 상태에서 다음을 확인해야 한다.

1. `/craftmenu`가 GUI를 여는지
2. `/process`가 GUI를 여는지
3. `/disassemble`이 GUI를 여는지
4. 레시피 선택 후 확정 버튼으로 제작되는지
5. 재료가 부족할 때 제작이 막히는지
6. 돈이 부족할 때 제작이 막히는지
7. 제작 성공 시 돈과 재료가 차감되고 결과 아이템이 지급되는지
8. `/repair`가 손에 든 손상 장비 수리 확인 GUI를 여는지
9. `/repair` 확정 시 돈이 차감되고 내구도가 회복되는지
10. `/lscrafting reload` 후 config 변경이 반영되는지
11. Quest `craft-item` objective가 제작 성공 시 진행되는지

## 이번 작업에서 건드리지 않은 영역

- ItemsAdder
- `generated.zip`
- TAB 설정
- BetterRanks 이미지
- Velocity resource-pack SHA
- LuckPerms 데이터
- Economy 데이터
- Town 데이터
- Newworld 서비스
