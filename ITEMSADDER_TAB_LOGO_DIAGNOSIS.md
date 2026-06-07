# ItemsAdder TAB Logo Diagnosis

작성일: 2026-06-03 KST

## Current Goal

TAB 플레이어 목록에 서버 로고 이미지를 표시한다.

구조:

1. ItemsAdder 리소스팩에 로고 PNG 등록
2. 리소스팩 font provider에서 특정 Private Use Area 문자에 이미지 매핑
3. TAB 또는 `LeeSeolLobby`가 해당 문자를 TAB header/footer에 출력
4. 클라이언트가 서버 리소스팩을 적용하면 문자가 이미지로 렌더링됨

## Current Symptom

인게임 TAB에서 로고 이미지 대신 특수 유니코드 문자가 그대로 보인다.

## Related Files

| 구분 | 파일 | 역할 |
|---|---|---|
| ItemsAdder lobby config | `/opt/minecraft/lobby/plugins/ItemsAdder/config.yml` | lobby ItemsAdder 리소스팩 설정 |
| ItemsAdder survival config | `/opt/minecraft/server/plugins/ItemsAdder/config.yml` | survival ItemsAdder 설정. 현재 public pack은 lobby output 사용 |
| Font image source | `/opt/minecraft/lobby/plugins/ItemsAdder/contents/betterranks/configs/ranks.yml` | BetterRanks 이미지와 expedition title 이미지 등록 |
| Logo source image | `/opt/minecraft/lobby/plugins/ItemsAdder/contents/betterranks/textures/expedition_title.png` | TAB 로고 원본 PNG |
| Unicode cache | `/opt/minecraft/lobby/plugins/ItemsAdder/storage/font_images_unicode_cache.yml` | ItemsAdder가 배정한 font image 문자 캐시 |
| Generated pack | `/opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip` | Velocity가 유저에게 보내는 실제 리소스팩 |
| Generated default font | `generated.zip:assets/minecraft/font/default.json` | 클라이언트가 읽는 실제 기본 폰트 매핑 |
| Generated explicit font | `generated.zip:assets/expedition/font/tab.json` | expedition 전용 font 매핑 |
| TAB lobby config | `/opt/minecraft/lobby/plugins/TAB/config.yml` | lobby TAB 설정. 현재 `U+E301` 사용 흔적 있음 |
| TAB survival config | `/opt/minecraft/server/plugins/TAB/config.yml` | survival TAB 설정 |
| Lobby header sender | `LeeSeolLobby/src/main/java/me/leeseol/lobby/LeeSeolLobbyPlugin.java` | lobby에서 `sendPlayerListHeaderAndFooter`로 TAB header/footer 직접 전송 |
| Lobby header config | `LeeSeolLobby/src/main/resources/config.yml` | `logo-font: minecraft:default`, `logo-glyph: "\\uE301"` |
| Velocity resource pack sender | `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java` | 네트워크 접속 시 resource pack offer 전송 |
| Velocity resource pack config | `/opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties` | pack URL, sha1, force 설정 |

## Expected Mapping

| 항목 | 값 |
|---|---|
| 로고 이미지 파일 | `assets/expedition/textures/gui/expedition_title.png` |
| namespace | `expedition` |
| explicit font key | `expedition:tab` |
| default font provider | `assets/minecraft/font/default.json` |
| generated font provider chars | `U+E301` (``) |
| TAB/lobby header에 들어간 문자 | `U+E301` (``) |
| Velocity pack URL | `http://34.64.126.179:8163/generated.zip` |
| 실제 zip SHA1 | `2b5a89e1fb3c2b23a6dc9e999b2758349e93b53f` |
| Velocity 설정 SHA1 | `2b5a89e1fb3c2b23a6dc9e999b2758349e93b53f` |

## Findings

- `resourcepack`, `velocity`, `minecraft`, `lobby` 서비스는 active 상태였다.
- resourcepack host는 `0.0.0.0:8163`에서 listen 중이고 `http://127.0.0.1:8163/generated.zip`은 HTTP 200을 반환했다.
- public pack으로 쓰는 lobby `generated.zip` 안에는 아래 파일이 존재한다.
  - `assets/minecraft/font/default.json`
  - `assets/expedition/font/tab.json`
  - `assets/expedition/textures/gui/expedition_title.png`
- lobby `generated.zip:assets/minecraft/font/default.json` 첫 provider는 `file: expedition:gui/expedition_title.png`, `chars: ["U+E301"]`, `height: 48`, `ascent: 42`이다.
- lobby `generated.zip:assets/expedition/font/tab.json`도 같은 이미지와 `U+E301`을 매핑한다.
- lobby TAB config에는 `U+E301` 문자가 들어간 줄이 있다.
- `LeeSeolLobby`도 `logo-font: minecraft:default`, `logo-glyph: "\\uE301"` 설정으로 `U+E301`을 보낸다.
- Velocity의 `resourcepack.properties` SHA1은 실제 lobby `generated.zip` SHA1과 일치한다.
- survival 쪽 ItemsAdder output zip에는 expedition logo provider가 없다. 다만 현재 네트워크 resource pack URL은 lobby output을 바라보므로, 이것만으로는 현재 증상의 직접 원인이라고 보기 어렵다.
- ItemsAdder unicode cache에는 `betterranks:expedition_title`이 `U+E02E`로 기록되어 있다. 그러나 실제 lobby generated pack에는 별도 수동 provider로 `U+E301`이 들어가 있으므로, 현재 TAB 로고 경로는 ItemsAdder cache 값이 아니라 generated pack의 `U+E301` provider를 사용한다.
- 기존 BetterRanks rank image provider는 `U+E029`부터 `U+E02D`까지 정상 포함되어 있다.

## Likely Root Cause

현재 파일 대조 기준으로 `TAB 문자열 문자값`, `generated font provider`, `로고 PNG 경로`, `리소스팩 URL`, `SHA1`은 서로 맞다.

따라서 가장 가능성이 높은 원인은 다음 순서다.

1. 클라이언트가 최신 resource pack을 실제로 적용하지 않은 상태이다.
2. 클라이언트 resource pack 캐시가 남아 있어 이전 pack을 보고 있다.
3. Velocity resource pack offer는 전송되지만, 유저 측에서 적용 실패/거부/미완료 상태이다.
4. TAB header가 resource pack 적용 전에 먼저 보이고 있고, 적용 완료 후 재전송 또는 재접속이 필요하다.

현재 단계에서 바로 설정을 수정하면 기존 rank image, TAB layout, ItemsAdder output을 깨뜨릴 가능성이 있다. 서버 파일만 보면 매핑은 이미 맞으므로, 우선 적용 상태 검증이 필요하다.

## Minimal Fix Plan

1. 유저 클라이언트에서 서버 resource pack 적용 상태를 확인한다.
2. 클라이언트의 서버 리소스팩 캐시를 삭제하거나 서버 목록에서 resource pack을 `사용`으로 설정한 뒤 재접속한다.
3. 재접속 후 TAB을 다시 열어 `U+E301`이 이미지로 렌더링되는지 확인한다.
4. 여전히 문자로 보이면, Velocity resource pack status 이벤트 로그를 `LeeSeolProxy`에 최소 추가해 실제 `ACCEPTED/SUCCESSFUL/FAILED` 상태를 기록한다.
5. resource pack 적용 성공인데도 문자로 보이면, `LeeSeolLobby` header 전송 시점을 resource pack successful 이후로 늦추거나, 접속 후 몇 초 뒤 한 번 더 header를 재전송한다.

## Files To Modify

현재 진단 기준으로 즉시 수정할 파일은 없다.

| 파일 | 수정 이유 | 위험도 |
|---|---|---|
| `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java` | 필요 시 resource pack status 로그 추가 | 낮음 |
| `LeeSeolLobby/src/main/java/me/leeseol/lobby/LeeSeolLobbyPlugin.java` | 필요 시 resource pack 적용 후 TAB header 재전송 지연 | 낮음 |

## Files Not To Touch

| 파일 | 이유 |
|---|---|
| LuckPerms data | 권한 데이터와 로고 렌더링은 무관 |
| Vault/Economy config | 경제와 무관 |
| LeeSeolTown config/data | 로고와 무관 |
| LeeSeolRanks data | 랭크 데이터와 무관 |
| TAB 전체 config 초기화 | rank prefix/layout이 이미 동작 중이므로 위험 |
| ItemsAdder 전체 config 초기화 | 기존 rank images와 generated pack이 유지되어야 함 |
| ItemsAdder contents 전체 재생성 | 기존 GUI/rank image 손상 위험 |
| Newworld server | 현재 paused이며 로고 문제와 무관 |

## Verification Steps

1. `curl -I http://34.64.126.179:8163/generated.zip` 또는 서버 내부 `curl -I http://127.0.0.1:8163/generated.zip` 확인
2. `sha1sum /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip` 확인
3. `/opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties`의 `sha1`과 비교
4. `generated.zip` 안의 `assets/minecraft/font/default.json`에서 `U+E301` provider 확인
5. lobby TAB config 또는 `LeeSeolLobby` config가 `U+E301`을 보내는지 확인
6. 클라이언트 서버 리소스팩 설정을 `사용`으로 둔 뒤 재접속
7. 클라이언트 캐시 삭제 후 재접속
8. 기존 rank image가 TAB/채팅에서 계속 보이는지 확인
9. TAB 로고가 문자 대신 이미지로 보이는지 인게임 확인

