# LeeSeolJobs Verification

작성일: 2026-06-04 KST

## 목적

`LeeSeolJobs`가 survival 서버에서 로드되고, 광질/농사/낚시 기반 돈 수급 루프를 제공할 준비가 되었는지 확인한다.

## 1. 배포 상태

| 항목 | 상태 | 근거 |
|---|---|---|
| 대상 서버 | PASS | survival only |
| jar 빌드 | PASS | `target/LeeSeolJobs-0.1.0.jar`, 31K |
| 배포 경로 | PASS | `/opt/minecraft/server/plugins/LeeSeolJobs-0.1.0.jar` |
| 백업 | PASS | `/opt/minecraft/backups/2026-06-04_04-33-25/leeseoljobs` |
| 서비스 | PASS | `minecraft: active` |
| newworld | PASS | `inactive` 유지 |

## 2. 콘솔/RCON 검증

| 명령어 | 상태 | 결과 |
|---|---|---|
| `version LeeSeolJobs` | PASS | `LeeSeolJobs version 0.1.0` |
| `lsjobs status` | PASS | 등록 유저 0명 |
| `lsjobs reload` | PASS | 설정 reload 메시지 반환 |
| `list` | PASS | RCON 정상 응답 |

## 3. 현재 구현 범위

| 기능 | 상태 | 비고 |
|---|---|---|
| 광질 보상 | IMPLEMENTED | config의 mining rewards 기준 |
| 설치 광물 악용 방지 | IMPLEMENTED | 설치한 보상 대상 광물은 일정 시간 보상 제외 |
| 농사 보상 | IMPLEMENTED | 완전히 자란 `Ageable` 작물 기준 |
| 낚시 보상 | IMPLEMENTED | `CAUGHT_FISH` 기준 |
| 일일 제한 | IMPLEMENTED | job type별 daily limit |
| 쿨다운 | IMPLEMENTED | mining/fishing cooldown |
| Vault 지급 | IMPLEMENTED | Vault economy provider 사용 |
| Rank 배율 | IMPLEMENTED | `leeseolranks.rank.*` 권한 기준 |
| Quest 연동 | IMPLEMENTED | reflection 기반 `LeeSeolQuestApi#progress(..., "earn-money", "jobs", amount)` |

## 4. 플레이어 실사용 검증 필요

접속자가 없어 아래 항목은 아직 검증하지 못했다.

| 테스트 | 기대 결과 |
|---|---|
| 자연 광물 채굴 | 돈 지급, `/jobs stats` 증가 |
| 직접 설치한 광물 채굴 | 돈 미지급 |
| 완전히 자란 작물 수확 | 돈 지급 |
| 덜 자란 작물 파괴 | 돈 미지급 |
| 낚시 성공 | 돈 지급 |
| daily limit 초과 | 돈 미지급 |
| dungeon 월드 | 보상 미지급 |
| Quest `earn-money` objective | Jobs 보상 지급 시 진행 |

## 5. 다음 작업

1. 접속자가 있을 때 실사용 보상 테스트.
2. 보상 액수 밸런스 조정.
3. 필요 시 `/jobs top` 구현.
4. 필요 시 PlaceholderAPI 추가.
