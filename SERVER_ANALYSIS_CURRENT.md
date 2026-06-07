# SERVER_ANALYSIS_CURRENT.md

작성일: 2026-06-04 KST

이 문서는 현재 expedition 서버의 구조와 플러그인 연결 관계를 깨끗한 한국어로 다시 정리한 서버 분석 문서다.
기존 분석 문서 중 일부는 터미널 인코딩 문제로 한글이 깨져 있으므로, 새 작업의 기준은 이 문서와 `SERVER_STATE.md`로 둔다.

## 채팅 역할 분리

| 채팅 | 담당 범위 |
| --- | --- |
| 플러그인 개발 파트 | Java/Paper/Velocity 플러그인, 명령어, 권한, 데이터 저장, Vault/LuckPerms/PAPI 연동, 서버 배포와 콘솔 검증 |
| GUI/리소스팩 파트 | 생성 이미지, ItemsAdder 설정/API, font image, generated.zip, resource-pack SHA1, TAB 로고/시각 요소, BetterRanks 이미지 |

현재 채팅은 플러그인 개발 파트로 취급한다.

플러그인 개발 파트에서는 ItemsAdder 설정, generated.zip, TAB 로고 glyph, TAB 시각 레이아웃, BetterRanks 이미지, `resourcepack.properties` SHA1을 직접 수정하지 않는다.
`LeeSeolStations`와 커스텀 가구 클릭 GUI 연동도 GUI/리소스팩 파트 담당이다. 플러그인 파트는 필요 시 `/craftmenu open ...` 같은 명령어/API endpoint만 제공한다.

## 전체 서버 구조

```text
클라이언트
  -> Velocity Proxy : 34.64.126.179:25565
      -> lobby Paper    : 127.0.0.1:25567
      -> survival Paper : 127.0.0.1:25566
      -> newworld Paper : 127.0.0.1:25568, 기본 inactive

리소스팩:
  클라이언트
    -> Velocity LeeSeolProxy가 리소스팩 제안
    -> resourcepack.service : 34.64.126.179:8163/generated.zip
```

## 주요 서비스

| 서비스 | 역할 | 경로 | 상태 기준 |
| --- | --- | --- | --- |
| `velocity` | 공개 접속 프록시, 서버 이동, 리소스팩 제안 | `/opt/minecraft/velocity` | active |
| `resourcepack` | 정적 리소스팩 ZIP 제공 | `/opt/minecraft/lobby/plugins/ItemsAdder/output` | active |
| `minecraft` | survival Paper 서버 | `/opt/minecraft/server` | active |
| `lobby` | lobby Paper 서버 | `/opt/minecraft/lobby` | active |
| `newworld` | 미래 New World 서버 | `/opt/minecraft/dungeon` | inactive 기본 |
| `mariadb` | LuckPerms 공유 DB | system service | active |

## 포트

| 포트 | 공개 여부 | 역할 |
| --- | --- | --- |
| `25565` | 공개 | Velocity 접속 포트 |
| `8163` | 공개 | 리소스팩 다운로드 |
| `25566` | 내부 | survival backend |
| `25567` | 내부 | lobby backend |
| `25568` | 내부 | newworld backend |

## 현재 리소스팩 상태

| 항목 | 값 |
| --- | --- |
| URL | `http://34.64.126.179:8163/generated.zip` |
| SHA1 | `8291368cc9c1cbb14872897a578fa42071e85e6e` |
| 배포 주체 | Velocity `LeeSeolProxy` |
| 호스트 | `resourcepack.service` |
| ItemsAdder host | 사용하지 않음 |
| backend server.properties resource-pack | 비워둠 |

리소스팩은 서버 전체에서 하나만 사용한다.
lobby와 survival이 서로 다른 리소스팩을 보내면 안 된다.

## 현재 TAB UI 상태

### 로비

- TAB 목록 레이아웃은 survival과 같은 4열 구조를 사용한다.
- TAB 플러그인의 자체 header/footer는 lobby에서 비활성화되어 있다.
- `LeeSeolLobby`가 직접 TAB header/footer를 전송한다.
- 상단에는 `\uE301` glyph로 expedition 로고를 표시한다.
- 로고는 `assets/generated/expedition_logo_sleek_220x42_left32.png` 기반이다.
- PING 문구는 `PING : `이다.
- PING 색상은 `#87DFFF`이다.
- 온라인 접속자 footer 색상은 `#D8C6A3`이다.
- 최대 표시 인원은 `500명`이다.

### 서바이벌

- TAB 목록은 4열 구조다.
- 1~3열은 접속 유저 목록이다.
- 4열은 마을원 목록이다.
- rank/town/nation 표시는 PlaceholderAPI 기반으로 연결되어 있다.

## 커스텀 플러그인 분석

| 플러그인 | 서버 | 역할 |
| --- | --- | --- |
| `LeeSeolProxy` | Velocity | `/lobby`, `/survival`, `/servers`, 리소스팩 제안 |
| `LeeSeolCore` | lobby/survival | 기본 명령어, 포탈, 런치패드, 차원 이동 제한 |
| `LeeSeolLobby` | lobby | 로비 보호, 스폰, TAB header/footer |
| `LeeSeolEconomy` | lobby/survival | 원화 경제, Vault, 상점, Shift+F 서버 메뉴 |
| `LeeSeolAuction` | lobby/survival | 경매 등록, 관리자 경매 개장, 입찰 GUI |
| `LeeSeolTown` | lobby/survival | 마을/국가/연방, 소속 표시, 채팅 |
| `LeeSeolRanks` | lobby/survival | 공유 랭크, ADMIN/DEV/S/A/B/C/D/player, 랭크업 조건, PlaceholderAPI |
| `LeeSeolQuest` | lobby/survival | 퀘스트/튜토리얼 진행 상태 공유, Quest API/events, 기본 objective 처리 |
| `LeeSeolJobs` | survival | 광질/농사/낚시 기반 초반 돈 수급, Vault 보상 |
| `LeeSeolCrafting` | survival | 제작/가공/분해 GUI, 돈 기반 수리, Quest craft-item 연동 |
| `LeeSeolHologram` | lobby/survival | 인게임 RGB 홀로그램 |
| `LeeSeolDungeon` | survival | 내부 dungeon 월드, 던전 입장/보호/상자 |
| `LeeSeolCombat` | survival | 전투 로그아웃 시체/NPC, 전투 태그 |
| `LeeSeolCleanup` | survival | 드롭 아이템 정리 타이머 |

## 외부 플러그인 분석

| 플러그인 | 역할 | 주의점 |
| --- | --- | --- |
| LuckPerms | 권한 저장, MariaDB 공유 | 직접 DB 편집 금지 |
| TAB | TAB 목록 레이아웃 | lobby header/footer는 LeeSeolLobby가 담당 |
| PlaceholderAPI | 랭크/마을/퀘스트 표시 연결 | 각 커스텀 플러그인의 expansion 확인 |
| Vault | 경제 API | LeeSeolEconomy와 연동 |
| ItemsAdder | 리소스팩 콘텐츠 | host/auto-apply는 꺼둠 |
| BetterRanks | 랭크 이미지 리소스 | 리소스팩 안 이미지가 필요 |
| Citizens | survival 전투 시체/NPC | lobby/newworld에는 무작정 복사 금지 |
| WorldEdit | lobby 포탈/구역 설정 보조 | 관리자 작업용 |

## 플러그인 연결 구조

```text
LeeSeolProxy
  -> Velocity backend 이동
  -> resourcepack.properties SHA 기반 리소스팩 제안

LeeSeolRanks
  -> LuckPerms 권한 부여/정리
  -> PlaceholderAPI rank prefix 제공
  -> TAB/채팅 표시에서 사용
  -> BetterRanks 이미지 리소스팩에 의존
  -> Vault 잔액과 플레이타임을 포함한 랭크업 조건 확인
  -> LeeSeolQuest rank-up objective 진행

LeeSeolTown
  -> PlaceholderAPI affiliation 제공
  -> TAB/채팅에서 rank 다음에 표시
  -> 경제/청크 구매와 연결 예정

LeeSeolEconomy
  -> Vault economy provider
  -> LeeSeolAuction 정산에 사용
  -> Shift+F 서버 메뉴와 연결

LeeSeolCrafting
  -> Vault 비용 차감
  -> LeeSeolRanks 권한 기반 랭크 요구 조건 확인
  -> LeeSeolQuest craft-item objective 진행

LeeSeolAuction
  -> Vault 잔액 사용
  -> dungeon world에서는 차단

LeeSeolDungeon
  -> survival 내부 dungeon world만 사용
  -> LeeSeolEconomy 서버 메뉴 제한과 연결
  -> LeeSeolAuction dungeon 차단과 연결

LeeSeolLobby
  -> lobby 보호
  -> TAB header/footer 직접 전송
  -> 리소스팩 glyph `\uE301` 표시
```

## 현재 중요한 운영 규칙

- 리소스팩은 Velocity에서 한 번만 제안한다.
- `resourcepack.service`가 제공하는 ZIP과 Velocity SHA가 반드시 같아야 한다.
- lobby/survival Paper의 resource-pack 설정은 비워둔다.
- ItemsAdder host/auto-apply는 비활성 유지한다.
- TAB empty-slot latency 아이콘은 리소스팩에서 투명 텍스처로 제거한다.
- `LeeSeolDungeon`은 survival에만 배포한다.
- `LeeSeolCombat`은 survival에만 배포한다.
- `newworld`는 기본 inactive 상태이며, 명시 요청 없이는 시작하지 않는다.

## 검증 명령어

서비스 확인:

```bash
systemctl is-active resourcepack velocity minecraft lobby newworld
sudo ss -ltnp | grep -E '25565|25566|25567|25568|8163'
sudo journalctl -u velocity -u lobby -u minecraft --since "5 minutes ago" -p err --no-pager
```

리소스팩 확인:

```bash
sudo unzip -t /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip
sha1sum /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip
grep -E '^(url|sha1|force)=' /opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties
curl -fsS http://127.0.0.1:8163/generated.zip -o /tmp/pack-check.zip
sha1sum /tmp/pack-check.zip
```

로비 TAB 설정 확인:

```bash
grep -A14 '^tab-header:' /opt/minecraft/lobby/plugins/LeeSeolLobby/config.yml
grep -E '^max-players=' /opt/minecraft/lobby/server.properties
```

## 현재 문서 기준

앞으로 작업을 통합할 때 우선순위는 다음과 같다.

1. `AGENTS.md`
2. `SERVER_STATE.md`
3. `PLUGIN_INDEX.md`
4. `RESOURCEPACK_IMAGE_HANDOFF.md`
5. `SERVER_ANALYSIS_CURRENT.md`

플러그인 개발 채팅은 `PLUGIN_INDEX.md`와 각 플러그인 소스 중심으로 진행한다.
이미지/리소스팩 채팅은 `RESOURCEPACK_IMAGE_HANDOFF.md` 중심으로 진행한다.

## 플러그인 파트 다음 작업

1. `LEESEOLQUEST_VERIFICATION.md` 작성 완료
2. `LeeSeolQuest` 기본 배포/RCON/reload 검증 완료
3. `LeeSeolQuest` 부족 objective 중 ItemsAdder가 필요 없는 항목 보완 완료:
   `harvest-crop`, `dungeon-enter`, Citizens NPC 클릭 기반 `npc-dialogue`
4. 플레이어 접속 상태에서 `/quest` GUI, PlaceholderAPI, shared progress,
   objective 실사용 검증 필요
5. `LEESEOLCRAFTING_DESIGN.md` 작성 완료
6. 돈 기반 수리 시스템을 `LeeSeolCrafting` 설계에 포함 완료
7. `ANVIL_REPAIR_IMPACT_REVIEW.md`,
   `ADVANCED_ENCHANTMENTS_BALANCE_REVIEW.md`, `LEESEOLJOBS_DESIGN.md`,
   `LEESEOLRANKS_RANKUP_EXPANSION_DESIGN.md`, `LEESEOLHUD_DESIGN.md` 작성 완료
8. `LeeSeolJobs` survival 배포 완료. 다음은 접속자 상태에서 Jobs 실사용 검증 필요
9. `LeeSeolCrafting` survival 배포 완료. 다음은 접속자 상태에서 제작/수리 GUI 실사용 검증 필요
10. `LeeSeolRanks` rankup 조건 확장 배포 완료. 다음은 접속자 상태에서 `/rank progress`, `/rankup` 실사용 검증 필요
11. `LEESEOLHUD_DESIGN.md` 작성
