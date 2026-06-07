# LeeSeolCombat Corpse Diagnosis

작성일: 2026-06-04 KST

## 현재 올바른 동작 기준

`LeeSeolCombat`의 시체/NPC 기능은 다음 기준으로 동작해야 한다.

| 상황 | 기대 동작 |
|---|---|
| 전투 태그 중 접속 종료 | 즉시 사망 처벌. 시체 NPC를 만들지 않는다. |
| 전투 상태가 아닌 상태로 survival에서 이탈 | 플레이어 스킨/장비를 가진 누운 시체 NPC를 생성한다. |
| 시체 NPC가 처치됨 | 원 주인은 다음 접속 시 사망 처리/인벤토리 정리된다. |
| 원 주인이 시체 처치 전 재접속 | 설정에 따라 기존 시체 NPC를 제거한다. |

## 현재 코드 기준

`SessionListener#onQuit`의 의도는 다음과 같다.

```java
if (tagged && combatLogoutKill) {
    clear combat tag;
    punishCombatLogout(player);
    return;
}
spawnLogoutClone(player);
```

즉 전투 중 로그아웃은 `punishCombatLogout`으로 바로 처벌되고,
일반 survival 이탈만 `spawnLogoutClone`으로 시체 NPC를 만든다.

## 전투 중 로그아웃 처벌

`CombatCloneManager#punishCombatLogout`는 다음을 수행한다.

- 플레이어 위치에 인벤토리/장비 드랍
- 플레이어 인벤토리 정리
- `pending-deaths.yml`에 pending death 등록
- 다음 접속 시 pending death를 consume한 뒤 스폰으로 이동/상태 정리

이 처벌은 시체 NPC 생성과 분리되어야 한다.

## 일반 로그아웃 시체

`CombatCloneManager#spawnLogoutClone`는 다음을 수행한다.

- Citizens in-memory NPC 생성
- 플레이어 스킨 적용
- 장비 적용
- SleepTrait로 누운 시체 자세 적용
- Interaction hitbox 생성
- NPC가 죽으면 인벤토리 드랍 및 pending death 처리

## 주의사항

- Paper backend에서는 Velocity 로비 이동과 클라이언트 접속 종료가 모두
  `PlayerQuitEvent`로 보인다. 별도 transfer marker가 없으면 둘을 구분할 수 없다.
- 현재 시체 NPC는 Citizens in-memory registry 기반이라 서버/플러그인 재시작 시 유지되지 않는다.
- 전투 중 로그아웃에 시체 NPC를 생성하는 방식은 현재 요구사항과 반대이므로 적용하지 않는다.

## 확인할 테스트

1. `/combat force <유저1> <유저2>` 후 한 명이 종료하면 시체 NPC 없이 즉시 처벌되는지 확인
2. 전투 상태가 아닌 survival 유저가 종료하면 시체 NPC가 생성되는지 확인
3. 시체 NPC를 처치하면 아이템이 드랍되고 원 주인이 다음 접속 시 정리되는지 확인
4. 원 주인이 시체 처치 전 재접속하면 시체가 제거되는지 확인
