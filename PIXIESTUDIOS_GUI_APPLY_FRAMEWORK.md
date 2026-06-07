# PixieStudios GUI 적용 틀

작성일: 2026-06-06 KST

이 문서는 `PixieStudios Game Menu.zip`을 expedition 서버의 Shift+F 메뉴에 적용하기 위한 기준이다.
YouTube 영상은 현재 도구에서 직접 자막/본문을 가져오지 못했으므로, 공식 ItemsAdder Font Images 문서와 구매 팩 내부 구조 분석을 기준으로 정리한다.

## 결론

2026-06-06 현재 운영 결론:

```text
PixieStudios DeluxeMenus 원본 메뉴 9개를 survival/lobby에 적용한다.
단, 예제 클릭 명령어는 서버 실제 명령으로 치환한다.
LeeSeolEconomy는 Shift+F 입력을 받아 DeluxeMenus 메뉴를 여는 외부 브리지 역할을 한다.
기존 LeeSeolEconomy 자체 GUI와 충돌하면 자체 GUI 쪽을 우선 중단하고 Pixie 메뉴를 먼저 검증한다.
```

적용 방향:

```text
PixieStudios / ItemsAdder = GUI 이미지, 폰트 이미지, 빈 슬롯 아이템 리소스
DeluxeMenus = 현재 Shift+F 메뉴의 화면/슬롯/설명/클릭 명령 실행
LeeSeolEconomy = Shift+F 감지, DeluxeMenus open bridge, `/leeseolmenu` 실제 기능
```

이유:

```text
1. 현재 서버는 이미 LeeSeolEconomy가 Shift+F 메뉴를 담당한다.
2. PixieStudios 팩은 DeluxeMenus 기반 예제 메뉴를 포함한다.
3. 사용자가 먼저 원본 슬롯/기능/설명 구조를 서버에 이식하길 요청했다.
4. 예제 명령어와 placeholder는 우리 서버 기능과 맞지 않는 것이 있어 실제 명령으로 패치했다.
5. 서버 이동, 던전 제한, 경매 제한, 권한 체크는 `/leeseolmenu` 쪽이 더 안전하다.
```

## 구매 팩 구조 요약

중요 경로:

```text
PixieStudios Game Menu/Raw Files/
PixieStudios Game Menu/Pre-Made Configurations/For ItemsAdder/
PixieStudios Game Menu/Pre-Made Configurations/For ItemsAdder/ItemsAdder/
PixieStudios Game Menu/Pre-Made Configurations/For ItemsAdder/DeluxeMenus/
```

사용할 후보:

```text
For ItemsAdder/ItemsAdder/contents/pixiestudios/
```

참고만 할 후보:

```text
For ItemsAdder/DeluxeMenus/config.yml
For ItemsAdder/DeluxeMenus/gui_menus/*.yml
```

사용하지 않을 후보:

```text
For Oraxen/
For Nexo/
__MACOSX/
```

## ItemsAdder 리소스 구조

구매 팩의 ItemsAdder namespace:

```yaml
info:
  namespace: "pixiestudios_gamemenu"
```

주요 GUI font image:

```yaml
font_images:
  pixiestudios_gamemenu1:
    path: "pixiestudios_gamemenu1.png"
    show_in_gui: true
    scale_location: 256
    y_position: 19
```

사용 방식:

```text
&f:offset_-8::pixiestudios_gamemenu1:
```

주의:

```text
GUI 이미지가 색이 변하면 title 앞에 &f를 붙인다.
y_position, scale_location 값은 원본 기준으로 먼저 유지한다.
렌더링 성공 후에만 y_position/offset을 조정한다.
```

## 현재 서버와의 충돌 후보

### 명령어

Pixie 예제는 다음 open_command를 가진다.

```text
/menu
/menus
/menu2
/menu3
/donate
/map
/map2
/map3
```

우리 서버의 실제 Shift+F 명령은:

```text
/servermenu
```

현재 방침:

```text
Shift+F는 LeeSeolEconomy external-menu로 `dm open pixiestudios_gamemenu1 %player%`를 실행한다.
Pixie open_command는 있을 수 있으나, 검증 기준은 Shift+F와 `/leeseolmenu` 연동이다.
예제 demo 명령어(`warp example1`, `eco take`, test button 등)는 실제 서버 명령으로 치환한다.
```

### Placeholder

Pixie 예제에 포함된 placeholder:

```text
%mmocore_class%
%mmocore_level%
%mmocore_level_percent%
%mmocore_skill_points%
%playerpoints_points_formatted%
%uclans_hasclan_formated%
%vault_eco_balance_commas%
```

방침:

```text
우리 서버에 없는 MMOCore, PlayerPoints, uClans placeholder는 제거한다.
경제 표시는 Vault/LeeSeolEconomy 기반으로 유지한다.
마을/국가 표시는 LeeSeolTown PlaceholderAPI 값을 사용한다.
랭크 표시는 LeeSeolRanks 값을 사용한다.
```

### model_id / CustomModelData

Pixie 예제 빈 슬롯 아이템:

```yaml
material: GOLD_NUGGET
model_id: 1900001
```

방침:

```text
서버 기존 CustomModelData와 충돌 여부를 VM에서 먼저 검색한다.
충돌이 없으면 1900001 유지 가능.
충돌이 있으면 expedition 전용 대역으로 변경한다.
```

## LeeSeolEconomy 수정 틀

현재 `ServerMenuManager`는 다음만 지원한다.

```text
단일 slot
server 또는 command
permission
local-servers
worlds / hidden-worlds
일반 material 아이콘
```

Pixie 팩 적용을 위해 필요한 최소 수정:

```text
1. size: 54 지원은 이미 가능
2. title에 ItemsAdder image alias 사용
3. items.<id>.slots 리스트 지원
4. items.<id>.custom-model-data 지원
5. filler에도 custom-model-data 지원
6. 같은 버튼이 여러 슬롯을 점유할 수 있도록 slot -> ServerButton 매핑 확장
7. 클릭 가능한 슬롯과 장식 슬롯을 분리
8. 페이지 이동이 필요하면 action: menu-page 추가
```

권장 config 구조:

```yaml
server-menu:
  title: "&f:offset_-8::pixiestudios_gamemenu1:"
  size: 54
  filler:
    enabled: true
    material: GOLD_NUGGET
    custom-model-data: 1900001
    name: " "
  items:
    lobby:
      slots: [0, 1, 9, 10, 18, 19, 27, 28]
      server: "lobby"
      material: GOLD_NUGGET
      custom-model-data: 1900001
      name: "&b로비"
      lore:
        - "&7상점, 안내, 서버 이동"
    survival:
      slots: [20, 21, 22, 29, 30, 31]
      server: "survival"
      material: GOLD_NUGGET
      custom-model-data: 1900001
      name: "&a야생"
      lore:
        - "&7채집, 건축, 탐험"
    auction:
      slots: [23, 24, 25, 32, 33, 34]
      command: "auction"
      material: GOLD_NUGGET
      custom-model-data: 1900001
      name: "&6경매장"
      lore:
        - "&7아이템 경매 메뉴"
```

## 한글화 기준

한글화는 두 종류로 나눈다.

### YML 텍스트

수정 난이도 낮음.

대상:

```text
display_name
lore
messages
button 설명
```

### PNG 안의 글자

수정 난이도 높음.

대상 예시:

```text
MENU
MAP
GAMES
DONATE STORE
AUCTION
DUNGEON
HOME
STORAGE
```

방침:

```text
처음에는 원본 영어 PNG로 렌더링 성공 여부를 검증한다.
렌더링이 성공하면 PNG 한글화 작업으로 넘어간다.
한글화 PNG는 원본을 덮어쓰지 않고 expedition_* 이름으로 복사해 작업한다.
```

## 적용 순서

### 1. 로컬 분석

```text
압축 파일 무결성 확인
ItemsAdder 폴더만 추출
DeluxeMenus 설정에서 슬롯 구조만 추출
이미지 크기와 alpha 채널 확인
```

### 2. 충돌 검사

```text
namespace pixiestudios_gamemenu 중복 확인
CustomModelData 1900001 중복 확인
기존 ItemsAdder config.yml 파손 여부 확인
기존 generated.zip 무결성 확인
```

### 3. 리소스팩 병합

```text
ItemsAdder contents/pixiestudios 추가
/iareload 또는 서버 재시작
/iazip 실행
generated.zip 테스트
public URL 다운로드
SHA1 계산
Velocity resourcepack.properties SHA 갱신
Velocity 재시작
```

### 4. 플러그인 수정

```text
LeeSeolEconomy ServerMenuManager에 slots/custom-model-data 추가
server-menu.size를 54로 변경
server-menu.title에 Pixie font image alias 적용
클릭 슬롯을 우리 기능에 연결
던전 월드 차단 유지
권한 제한 유지
```

### 5. 인게임 검증

```text
리소스팩 다운로드 성공
Shift+F로 메뉴 열림
GUI 이미지 위치 확인
클릭 슬롯이 시각 버튼과 일치
로비 이동
서바이벌 이동
경매장 열기
던전 월드에서 차단
관리자 전용 버튼 권한 확인
```

## 실패 시 우선 확인 항목

GUI 이미지가 안 보임:

```text
generated.zip에 PNG가 있는지 확인
font json에 glyph가 들어갔는지 확인
Velocity SHA와 public generated.zip SHA가 같은지 확인
클라이언트 리소스팩 캐시 삭제 후 재접속
Force Unicode 관련 설정 확인
```

이미지가 흰 사각형으로 보임:

```text
이미지 파일명에 대문자/공백/특수문자가 없는지 확인
이미지 크기가 256x256 제한 안인지 확인
y_position이 scale 값보다 큰지 확인
```

클릭 영역이 안 맞음:

```text
DeluxeMenus slots 배열과 LeeSeolEconomy config slots 비교
GUI title offset 조정
ItemsAdder y_position 조정
```

서버 기능이 안 됨:

```text
LeeSeolEconomy command/server action 확인
Velocity backend 이름 확인: lobby, survival, newworld
권한 노드 확인
blocked-worlds 확인
```

## 금지 사항

```text
DeluxeMenus config.yml을 그대로 서버에 덮어쓰지 않는다.
Oraxen/Nexo 설정을 함께 넣지 않는다.
ItemsAdder config.yml을 broad string-rewrite로 수정하지 않는다.
generated.zip만 수정하고 SHA 갱신을 빼먹지 않는다.
영상/예제 명령어의 테스트 메시지를 그대로 운영 서버에 넣지 않는다.
```
