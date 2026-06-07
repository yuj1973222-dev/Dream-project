# LEESEOLRANKS_REQUIREMENTS_VERIFICATION.md

작성일: 2026-06-04 KST

## 구현 범위

기존 `LeeSeolRanks` 0.1.0 안에 랭크업 조건 확장을 추가했다.

현재 기능:

- 기존 `thresholds` 하위 호환 유지
- `rank-up.requirements.<rank>` config 추가
- 조건 항목:
  - `kills`
  - `money`
  - `playtime-minutes`
- `/rank progress`
- `/rank requirements`
- `/rankup` 조건 상세 체크
- 킬 발생 시 복합 조건까지 반영해 승급 가능 메시지 판단
- `leeseolranks.bypass.requirements` 권한
- 승급 성공 시 LeeSeolQuest `rank-up` objective soft hook

## 배포 대상

| 항목 | 값 |
| --- | --- |
| survival jar | `/opt/minecraft/server/plugins/LeeSeolRanks-0.1.0.jar` |
| lobby jar | `/opt/minecraft/lobby/plugins/LeeSeolRanks-0.1.0.jar` |
| survival config | `/opt/minecraft/server/plugins/LeeSeolRanks/config.yml` |
| lobby config | `/opt/minecraft/lobby/plugins/LeeSeolRanks/config.yml` |
| 최종 백업 | `/opt/minecraft/backups/2026-06-04_04-58-59/leeseolranks` |
| newworld | inactive 유지 |

## 서버 검증 결과

확인 완료:

- Maven server-side build 성공
- survival/lobby jar 내부 `plugin.yml` 존재 확인
- survival/lobby 서비스 재시작 성공
- `minecraft` active 확인
- `lobby` active 확인
- `newworld` inactive 확인
- survival/lobby config에 `rank-up.requirements` 반영 확인
- survival/lobby 최근 에러 없음
- survival/lobby `LeeSeolRanks enabled. players=2` 확인
- survival/lobby PlaceholderAPI expansion 등록 확인

## RCON 검증 결과

기존 `~/mc-rcon.py`는 없었지만, 임시 RCON 클라이언트로 server.properties의 RCON 설정을 읽어 명령을 검증했다.
RCON 비밀번호는 출력하지 않았다.

survival 확인:

- `version LeeSeolRanks`
- `rank requirements`
- `leeseolrank status`
- `version LeeSeolCrafting`
- `lscrafting status`
- `lscrafting recipe list`
- `list`

lobby 확인:

- `version LeeSeolRanks`
- `rank requirements`
- `leeseolrank status`
- `list`

## 현재 기본 랭크업 조건

| 목표 랭크 | 킬 | 보유 돈 | 플레이타임 |
| --- | ---: | ---: | ---: |
| D | 10 | 0원 | 0분 |
| C | 20 | 10,000원 | 120분 |
| B | 30 | 50,000원 | 300분 |
| A | 50 | 150,000원 | 600분 |
| S | 100 | 500,000원 | 1200분 |

## 인게임 테스트 필요 항목

1. 일반 유저로 `/rank progress` 실행
2. 일반 유저로 `/rank requirements` 실행
3. 조건 부족 상태에서 `/rankup` 실행
4. 조건 충족 상태에서 `/rankup` 실행
5. 승급 후 킬 카운트가 0으로 초기화되는지 확인
6. 승급 후 LuckPerms rank permission이 동기화되는지 확인
7. 승급 후 Quest `rank-up` objective가 진행되는지 확인

## 이번 작업에서 건드리지 않은 영역

- 기존 `ranks.yml` 데이터 구조
- LuckPerms DB 직접 데이터
- TAB 설정
- BetterRanks 이미지
- ItemsAdder/resource-pack 설정
- Town 데이터
- Newworld 서비스
