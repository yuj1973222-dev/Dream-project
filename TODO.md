# TODO.md

현재 합의한 백로그. 사용자와 에이전트가 합의한 내용만 짧게 유지한다.

## 완료

- [완료] 3. 기존 시스템 수정: `LeeSeolTown`의 국가 타입을 제거하고 국가 고유
  색상 저장 구조로 마이그레이션했다. 색상은 채팅, 스코어보드, PlaceholderAPI,
  BlueMap 영토 마커, 영토 입장 표시에서 같은 값을 사용한다. `LeeSeolCleanup`은
  기존 TAB 타이머를 유지하면서 삭제 10초 전 액션바 카운트다운과 시작 사운드를
  추가했다. 전쟁 모드는 `침략전(invasion)`/`총력전(total)`만 남기고, 테스트 전용
  전쟁 명령어는 별도 존재하지 않음을 확인했다. 2026-06-25 survival에
  LeeSeolTown/LeeSeolCleanup 배포 및 최근 로그 검증 완료.

- [완료] 2. 랜덤 스폰 시스템: `LeeSeolCore`에 `/wild`, `/rtp`, `/randomtp`,
  `/야생` 랜덤 텔레포트, 일반 야생 10초 시전/이동·피해·전투 취소,
  랭크별 config 쿨타임, worldborder 기반 안전 위치 탐색, config 기반
  중립구역/금지구역 fallback, 튜토리얼 완료 후 랜덤 중립구역 이동 서비스
  메서드를 준비했다. 참고: 2026-06-25 survival에 LeeSeolCore만 배포했고,
  월드보더는 15000 x 15000으로 조정했으며 구현 보존 커밋은
  `c3d6934` (`codex/random-spawn-system`)이다.

- [완료] 0. 월드 재생성 및 정리: 야생 월드 재생성, Terralith + Incendium +
  Trek 적용, Geophilic 제외, 플레이어 데이터 보존, BlueMap 저사양
  `overworld` purge/update, Chunky radius `7500` 시작, Chunky 완료 후 BlueMap
  전체 지상 렌더 watcher 설치, 운영 치트시트 정리.
- [완료] 4. 핑 시스템: `LeeSeolCore` 중심으로 `/ping` 명령어와 GUI를 구현하고,
  BetterHUD 포인터로 카지노/던전/중립구역/국가 목적지의 방향, 거리, 마커를
  표시하도록 배포했다. 중립구역 핑 미표시 문제는 BetterHUD 2.0.0 명령어
  문법에 맞춰 수정했으며, 20블럭 이내 자동 해제까지 검증했다.

- [완료] 6. 접속자 수 제한 및 대기열: `LeeSeolProxy` 중심으로 survival 일반
  유저 100명 제한, `leeseol.queue.bypass` 관리자 즉시 입장, `/survival`과
  `/야생입장`, FIFO 메모리 대기열, 액션바 순번 표시, 빈자리 자동 입장을
  구현했다. `LeeSeolLobby`는 `limbo` 월드 이동, 대기실 행동 제한, `/lobby`와
  `/로비` 이탈 처리만 담당한다. 2026-06-25 velocity/lobby에 배포했고,
  `minecraft:limbo` 로드와 최근 로그 검증을 완료했다.

- [완료] 7. 월드 콘텐츠 배치 기반: `LeeSeolCore`에 `/content add/remove/rename/
  setradius/setspawn/list/tp`와 `neutral`, `casino`, `dungeon` 콘텐츠 registry를
  구현했다. WorldEdit 선택 영역, YAML 저장/로드, WorldGuard region 자동 생성/삭제,
  BlueMap 던전/카지노 마커, reload 시 region 재동기화를 포함한다. 기존
  `LeeSeolTown` 중립구역 관리 명령은 삭제하고, Town 보호/점령금지 판정은
  LeeSeolCore `neutral` 콘텐츠를 참조하도록 통합했다. legacy 중립구역
  `central`, `ne1`은 Core `contents.yml`로 이관했으며 2026-06-25 survival/lobby
  배포와 최근 로그 검증을 완료했다.

- [완료] 17. 국가 코어 schem 파일 정리: `LeeSeolTown` jar/resources에서 내장
  `nation_core.schem`을 제거하고, 국가 코어 `schematic` 연결을 빈 값으로
  비활성화했다. schem 미연결 상태에서는 WorldEdit paste를 시도하지 않고,
  국가 코어 블록 설치와 국가 코어 데이터 등록 흐름만 유지한다. 2026-06-25
  survival에 배포했고, 기존 서버 `nation_core.schem`은 백업 후 제거했다.
  운영 테스트용 schem 5개는 충돌 확인 후
  `/opt/minecraft/server/plugins/WorldEdit/schematics/`에 배치했으며, 국가 코어
  설치 흐름에는 아직 연결하지 않았다.

## 3. 기존 시스템 수정 - 추가 작업

- `LeeSeolQuest`로 돈을 수급하는 기능은 모두 기능 정지.
- Primary: `LeeSeolQuest`.
- Allowed consumers: `LeeSeolEconomy`는 잔액 지급 처리 확인이 필요한 경우만.
- Bridge/config: Vault, `/won give` 같은 경제 지급 명령어 보상, Quest reward config.
- 작업 기준: 퀘스트 완료 보상, 운영 패스, 튜토리얼, 일일/주간 퀘스트 등 `LeeSeolQuest`에서 직접 돈을 지급하는 모든 경로를 비활성화.
- 돈 대신 지급할 대체 보상은 이번 범위에서 새로 설계하지 않음.
- 확인 대상: Quest config 보상, command reward, Vault deposit 호출, `/won give` 호출, Quest GUI/lore/reward text, 완료 메시지.
- 검증: Quest reload 후 샘플 퀘스트 완료 시 돈이 증가하지 않고, 관련 오류가 없는지 확인.

## 5. 미확인 구성요소 검증

- 파티원끼리 건축이 가능한지 확인.
- 파티원끼리 PVP가 가능한지 확인.
- 국가원끼리 건축이 가능한지 확인.
- 국가원끼리 PVP가 가능한지 확인.
- 중립구역 보호가 정상 작동하는지 확인.

## [완료] 8. 파티 명령어 UX 개선

- 2026-06-26 완료: `LeeSeolTown` 중심으로 `/party`와 `/nation` 명령어 UX를 분리.
- `/party`는 파티 생성/초대/수락/거절/가입/탈퇴/해산/리더 위임/추방/정보/채팅/진단/리로드만 담당.
- `/nation`은 국가 생성/해산/PVP/build 설정/금고/upkeep/deposit/클레임/전쟁/구조물 undo를 담당.
- 클레임 명령어는 `/nation claim add`, `/nation claim remove`, `/nation claim list`, `/nation claim price`, `/nation claim upkeep`로 정리.
- `/party` 입력 시 파티 GUI, `/nation` 입력 시 국가 GUI를 열며, 위험 작업은 확인 GUI를 거침.
- 파티/국가 GUI 아이템 설명은 기존 서버 `config.yml`에 새 lore 키가 없어도 기본 문구 fallback으로 표시.
- 파티/국가 초대 유효 시간은 60초이며, 채팅 클릭 메시지의 `[수락]`, `[거절]` 버튼으로 처리.
- 한글 별칭과 별도 `/neutral` 명령어는 추가하지 않음. 중립구역 관리는 기존대로 `/content neutral` 담당.
- 검증/배포: `mvn -f LeeSeolTown/pom.xml package` 통과, jar `plugin.yml` 확인, survival `minecraft`만 재시작, RCON `version LeeSeolTown`, `party help`, `nation help` 확인, 최근 LeeSeolTown 오류 없음.

## [완료] 9. 던전 시스템

- 2026-06-25 완료: `LeeSeolDungeon` 중심으로 survival 내부 `dungeon`
  월드 포털 입장, spawnarea 랜덤 안전 위치, 퇴장 포털 귀환, 던전 월드
  설치/파괴 보호, 탈출성 명령 제한, 기존 `LootChestManager` 보상 상자
  유지, mobarea 기본 몹 스폰, mobgear GUI, 던전 몹 드롭 제한을 구현/배포.
- 배포/검증: survival `/opt/minecraft/server/plugins/LeeSeolDungeon-0.1.0.jar`,
  `minecraft`만 재시작, `plugin.yml` 확인, 최근 로그/RCON smoke 통과.
- 후속 인게임 운영: WorldEdit으로 `spawnarea`와 `mobarea`를 등록하고,
  필요 시 `mob-spawning.spawn-table`의 `gear`를 mobgear ID에 연결.

## 10. 카지노 시스템

- 카지노 시스템 구현 시작.
- 7번에서 준비한 `/content casino` 등록 위치를 기준으로 출입, 보호구역, 게임/상점/경제 연동 범위를 구체화.
- 경제 플러그인과의 실제 연동은 16번 경제 및 상점 작업과 충돌하지 않게 범위를 나눠 진행.

## 11. 오류 수정

- 마을원 목록에 타 국가 인원이 함께 표시되는 문제 수정.
- 점령전에서 타 국가원이 있어도 점령 게이지가 채워지는 문제 수정.
- 점령구역에 아무도 없는데 점령 게이지가 채워지는 문제 수정.
- 점령구역에 아군만 있을 때 점령 게이지가 낮아지는 문제 수정.
- 개발 완료된 RTP 기능이 현재 작동하지 않는 문제 확인 및 수정.
  - Primary: `LeeSeolCore`.
  - 확인 대상: `/rtp`, `/wild`, `/randomtp`, `/야생`, 10초 시전 대기, 이동/피해/전투 취소, 쿨타임, 금지구역/월드보더 조건.
- `LeeSeolCombat`의 통칭 "시체기능"이 현재 작동하지 않는 문제 확인 및 수정.
  - Primary: `LeeSeolCombat`.
  - 확인 대상: 전투 로그아웃 또는 일반 로그아웃 시 생성되는 시체/클론 처리, 사망/드롭/정리 흐름, Citizens 의존성, 최근 로그 오류.

## 12. 커스텀 인챈트

- Primary: `LeeSeolEnchanting`.
- Allowed consumers: `LeeSeolCrafting`, 추후 `LeeSeolDungeon` 보상 연동.
- Bridge/config: AdvancedEnchantments, ItemsAdder는 커스텀 아트 요청 시만.
- 1차 목표는 기존 커스텀 인챈트 점검 + 제작법 최소 구현.
- AdvancedEnchantments는 커스텀 인챈트 효과 처리 엔진으로 계속 사용.
- `LeeSeolEnchanting`은 서버용 UX 계층으로 전용 적용 GUI, 책 생성/검증, 장비 적용 검증, AdvancedEnchantments 연동을 담당.
- `LeeSeolCrafting`은 제작 GUI/재료 처리/제작법 config를 담당하고, 제작 성공 시 `LeeSeolEnchanting`에 커스텀 인챈트 책 생성을 요청.
- 커스텀 인챈트 획득/적용은 두 단계 구조로 진행: 인챈트 책 제작/획득 후 전용 GUI에서 장비에 적용.
- 기존 AdvancedEnchantments 우클릭 적용 UX 대신 전용 GUI에서 책 + 장비를 넣고 적용하는 방식으로 진행.
- 적용 GUI에는 장비 슬롯, 인챈트 책 슬롯, 결과 미리보기, 적용 버튼, 경험치 비용, 현재 슬롯/최대 슬롯 표시를 제공.
- 책 제작 비용은 재료 + 경험치로 구성하고, 돈 비용은 16번 경제/상점 작업 이후 필요 시 추가.
- 장비 적용 비용은 책 소모 + 경험치 소모로 구성.
- 초기 적용 성공률은 100%로 설정하고, 실패 확률/보호 아이템은 이번 범위에서 제외.
- 장비당 커스텀 인챈트 기본 슬롯은 3개로 제한.
- AdvancedEnchantments 기존 슬롯 증가 아이템을 사용해 최대 5개까지 확장 가능하게 설정.
- 현재 AdvancedEnchantments config 기준 기존값은 기본 9개, 슬롯 증가 최대 13개였으므로 서버 기준에 맞게 3/5 구조로 낮추는 방향.
- 무기/방어구/도구 구분 없이 모든 장비에 동일한 3개 기본, 5개 최대 규칙을 적용.
- 슬롯 개수 제한은 AdvancedEnchantments config를 우선 사용하고, `LeeSeolEnchanting` GUI는 해당 제한을 표시하고 초과 적용을 막음.
- 기존 마법부여대 route로 붙을 수 있는 커스텀 인챈트는 제작법 대상으로 중복 추가하지 않음.
- 제작법 대상은 마법부여대에서 나오지 않거나, 더 높은 티어/특수 목적의 인챈트 중심으로 선정.
- 1차 제작법은 커스텀 인챈트 책 5개로 시작.
- 제작법 대상 5개는 에이전트가 몇 가지 후보를 추천하고, 운영자가 그중에서 최종 선택.
- 추천 후보는 기존 AdvancedEnchantments 전체 목록과 서버 방향을 기준으로 유틸/PvE/던전 성장용/고위험 제외 여부를 함께 설명.
- 점검 항목: 인챈트 목록, 적용 방식, lore 표시, 명령어 상태, 장비 타입 제한, 바닐라 인챈트 충돌, AdvancedEnchantments config, 오류/로그.
- 제외: 던전 보상 전체 설계, 경제 밸런스, 아이템 리소스팩 대규모 수정, AdvancedEnchantments 엔진 자체 대체, 마법부여대 route 인챈트의 중복 제작법.

## 13. 성능 정리

- 서버에 렉을 유발할 수 있는 부분을 추려서 대체 시스템을 만들거나, 유저 가치가 낮은 시스템은 제거.
- 진행 방식은 코드 위험 후보 추출 + Spark 실측 + timings/log 보조 확인을 함께 사용.
- 감사 범위는 자체 개발 플러그인과 직접 연동 중인 외부 플러그인/config까지 포함.
- 자체 개발 플러그인 후보: `LeeSeolTown`, `LeeSeolCore`, `LeeSeolRanks`, `LeeSeolCleanup`, `LeeSeolCombat`, `LeeSeolDungeon`, 필요 시 기타 LeeSeol 플러그인.
- 외부/브리지 후보: TAB, PlaceholderAPI, BetterHUD, WorldGuard, BlueMap marker 연동, ItemsAdder 연동, Paper 주요 성능 설정.
- Chunky/BlueMap 렌더 상태와 진행 상황은 사용자가 직접 관리하므로 기본 감사 범위에서 제외.
- BlueMap은 렌더 상태가 아니라 자체 플러그인의 marker 갱신 빈도와 연동 부담만 확인.
- 상세 결과물은 `PERFORMANCE_AUDIT.md`로 관리하고, `TODO.md`에는 방향만 유지.
- `PERFORMANCE_AUDIT.md`에는 후보 위치, 근거, 영향도, 조치 방향, 작업 충돌 가능성, 상태를 기록.
- 실측 도구는 Spark 우선, timings와 최근 로그를 보조 근거로 사용.
- 지금 1차 감사로 코드/설정 위험 후보를 정리하고, 베타테스트 직전 2차 감사로 최종 통합 상태를 재측정.
- 1차 감사의 첫 우선순위는 표시 갱신 계열.
- 우선 확인 대상: 스코어보드, TAB 표시, PlaceholderAPI 호출, 액션바, 보스바, BetterHUD, 랭크/국가/파티 표시, 잔액/구역/전투 상태 반복 계산.
- 조치 기준은 유저 가치가 낮은 기능은 삭제 후보, 핵심 기능은 대체/최적화 후보로 분리.
- 삭제 후보는 즉시 삭제하지 않고 먼저 사용자에게 후보와 근거를 보고한 뒤 승인받은 것만 삭제.
- 승인 전에는 config 비활성화, 갱신 주기 조정, 캐싱, 비동기화 같은 안전한 대안을 우선 제안.
- 제외: 기능 추가, 밸런스 조정, Chunky/BlueMap 상태 확인, 광범위한 리팩터링, 승인 없는 기능 삭제.

## 14. 최종 GUI 검토

- 커스텀 GUI 생성.
- 최종 GUI 검토.

## 15. 베타테스트

- 베타테스트 시작.

## 16. 경제 및 상점

- 경제 플러그인과 상점 시스템 생성 또는 완성.

## 18. 튜토리얼

- 튜토리얼 시스템을 추가.
- 상세 내용은 사용자가 "18번 하자"라고 말하면 그때 함께 구체화.

## 나중에 구체화할 것

- 전쟁 테스트 명령어 삭제 시 함께 제거할 설정/데이터 파일 범위.
