# LEESEOLCOMBAT_RESTORE_VERIFICATION.md

작성일: 2026-06-04 KST

## 복구 기준

`LeeSeolCombat` 시체/NPC 로직을 다음 기준으로 복구했다.

| 상황 | 동작 |
|---|---|
| 전투 태그 중 접속 종료 | 즉시 사망 처벌. 시체 NPC 생성 안 함. |
| 전투 상태가 아닌 survival 이탈 | Citizens 기반 누운 시체 NPC 생성. |
| 시체 NPC 처치 | 원 주인 pending death 처리 및 아이템 드랍. |

## 코드 변경

- `SessionListener#onQuit`에 분기 의도를 명확히 하는 주석 추가
- 잘못된 이전 진단 문서 수정

핵심 분기:

```java
if (tagged && combatLogoutKill) {
    clear combat tag;
    punishCombatLogout(player);
    return;
}
spawnLogoutClone(player);
```

## 배포 대상

| 항목 | 값 |
|---|---|
| 서버 | survival |
| 서비스 | `minecraft` |
| jar | `/opt/minecraft/server/plugins/LeeSeolCombat-0.1.0.jar` |
| config | `/opt/minecraft/server/plugins/LeeSeolCombat/config.yml` |

## 서버 검증 결과

확인 완료:

- 서버 빌드 성공
- survival jar 배포 완료
- `minecraft` active
- `newworld` inactive 유지
- `LeeSeolCombat` 로드 확인
- `Citizens` 로드 확인
- 서버 `Done` 확인
- RCON `version LeeSeolCombat` 정상
- RCON `version Citizens` 정상
- RCON `combat status` 정상
- config `combat-logout.kill-during-combat: true` 확인
- config `logout-clone.enabled: true` 확인

## 인게임 테스트 필요

1. `/combat force <유저1> <유저2>` 후 전투 중 한 명이 종료
   - 기대: 시체 NPC 없이 즉시 처벌, 인벤토리 드랍/정리
2. 전투 상태가 아닌 survival 유저가 종료
   - 기대: 누운 시체 NPC 생성
3. 다른 유저가 시체 NPC 처치
   - 기대: 아이템 드랍, 원 주인 다음 접속 시 pending death 처리
4. 원 주인이 시체가 죽기 전 재접속
   - 기대: `remove-on-owner-return: true`에 따라 시체 제거
