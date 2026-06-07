# SHIFT_F_GUI_TEMPLATE_STUDY.md

작성일: 2026-06-05 KST

이 문서는 `NewGUI1.3.zip` 예시를 분석해 Shift+F 커스텀 GUI 제작에 필요한 구조만 정리한다.
예시 디자인은 복제하지 않는다. 디자인은 expedition 서버 전용으로 새로 만든다.

## 분석한 예시 구조

`NewGUI1.3.zip`은 아래 구조를 사용한다.

```text
BetterGUI/menu/server_selector.yml
ItemsAdder/data/items_packs/newgui/guis.yml
ItemsAdder/data/resource_pack/assets/newgui/textures/font/gui/server_selector.png
```

핵심 원리:

```text
1. ItemsAdder font image로 GUI 배경 PNG를 등록한다.
2. BetterGUI 메뉴 title에 font image를 넣는다.
3. 실제 클릭 판정은 air 아이템이 차지한 슬롯 묶음으로 처리한다.
```

예시 설정:

```yaml
menu-settings:
  name: '&f%img_offset_-12%%img_server_selector%'
  inventory-type: CHEST
  rows: 6

font_images:
  server_selector:
    path: "font/gui/server_selector.png"
    y_position: 47
```

예시 배경 이미지 크기:

```text
183x255 RGBA
```

## 우리 서버 적용 방향

우리 서버는 BetterGUI를 그대로 쓰지 않고, 기존 Shift+F 플러그인 로직과 ItemsAdder 리소스팩을 연동하는 방향이 우선이다.

권장 구조:

```text
Shift+F 입력
-> Java 플러그인이 Inventory GUI 오픈
-> GUI title에 ItemsAdder font image glyph 삽입
-> 버튼 클릭은 슬롯 번호 또는 슬롯 그룹으로 처리
-> 버튼 아이콘/배경은 ItemsAdder 리소스팩에서 제공
```

GUI 파트 담당:

```text
GUI 배경 PNG
ItemsAdder font_images 설정
unicode glyph 또는 image alias
버튼 시각 영역
아이콘 ID
슬롯 클릭맵 계약
generated.zip 갱신/SHA 검증
```

플러그인 파트 담당:

```text
Shift+F 감지
Inventory GUI 열기
클릭 이벤트 처리
서버 이동/상점/경매/퀘스트 action 실행
권한/쿨다운/월드 제한
```

## expedition 전용 샘플

새로 만든 샘플은 예시 디자인을 복제하지 않고 expedition 전용으로 제작했다.

```text
assets/generated/gui_samples/expedition_shift_f_menu_original_183x255.png
assets/generated/gui_samples/expedition_shift_f_menu_original_183x255_preview3x.png
assets/generated/gui_samples/expedition_shift_f_menu_original_183x255_clickmap.png
assets/generated/gui_samples/expedition_shift_f_menu_original_183x255_clickmap_preview3x.png
```

샘플 방향:

```text
서버명: expedition
스타일: 청색 금속, 탐험, 설원, 목재, 금속 프레임
구조: 3개 가로 선택 카드 + 하단 인벤토리 영역
텍스트: 고정 텍스트 최소화
클릭: 카드별 슬롯 묶음으로 처리 가능
```

## 임시 클릭맵 제안

6줄 CHEST 기준 슬롯 번호:

```text
0  1  2  3  4  5  6  7  8
9  10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

현재 샘플 기준 권장 슬롯 그룹:

```yaml
lobby:
  slots: [10, 11, 12, 13, 14, 15, 16]
survival:
  slots: [19, 20, 21, 22, 23, 24, 25]
newworld:
  slots: [28, 29, 30, 31, 32, 33, 34]
```

## ItemsAdder 설정 초안

namespace는 기존 expedition 리소스팩 namespace를 사용한다.

```yaml
info:
  namespace: expedition

font_images:
  shift_f_main_menu:
    path: "gui/shift_f_main_menu.png"
    y_position: 47
```

GUI title 후보:

```text
&f%img_offset_-12%%img_shift_f_main_menu%
```

실제 적용 전에는 현재 ItemsAdder 버전의 image alias 포맷을 서버에서 확인해야 한다.

## 적용 전 필수 검증

```text
1. 기존 generated.zip 백업
2. expedition namespace에 PNG 추가
3. ItemsAdder font_images 설정 추가
4. generated.zip 재생성
5. ZIP 무결성 검사
6. SHA1 계산
7. 공개 URL 다운로드 SHA1 비교
8. Velocity resourcepack.properties SHA1 갱신
9. Velocity 재시작
10. 클라이언트 재접속 확인
```

## 금지

```text
NewGUI 예시 이미지를 그대로 복사하지 않는다.
NewGUI namespace를 그대로 사용하지 않는다.
기존 \uE301 TAB 로고 glyph를 덮어쓰지 않는다.
lobby/survival server.properties에 resource-pack을 넣지 않는다.
ItemsAdder auto host를 다시 켜지 않는다.
```

## expedition v2 시안

2026-06-05에 NewGUI 구조를 참고하되 expedition 전용으로 다시 그린 코드 기반 시안이다.
AI 생성 결과가 GUI 구조를 제대로 따르지 않아, 실제 ItemsAdder 적용을 고려해 183x255 RGBA PNG로 직접 제작했다.

생성 스크립트:

```text
tools/generate_exp_gui_v2.py
```

생성 파일:

```text
assets/generated/gui_samples/expedition_shift_f_menu_v2_183x255.png
assets/generated/gui_samples/expedition_shift_f_menu_v2_183x255_preview3x.png
assets/generated/gui_samples/expedition_shift_f_menu_v2_183x255_clickmap.png
assets/generated/gui_samples/expedition_shift_f_menu_v2_183x255_clickmap_preview3x.png
```

검증 결과:

```text
size: 183x255
mode: RGBA
```

v2 적용 후보 슬롯 그룹은 기존 임시 클릭맵과 동일하게 유지한다.

```yaml
lobby:
  slots: [10, 11, 12, 13, 14, 15, 16]
survival:
  slots: [19, 20, 21, 22, 23, 24, 25]
newworld:
  slots: [28, 29, 30, 31, 32, 33, 34]
```

주의:

```text
이 이미지는 컨셉/적용 후보이다.
서버 적용 전에는 ItemsAdder font_images 등록, generated.zip 재생성, 공개 URL SHA1 검증, Velocity SHA 갱신 순서를 반드시 지킨다.
```

## 단계별 GUI 제작 로그

### 1단계: 외곽 블루 스틸 프레임

2026-06-06에 Shift+F GUI를 부품 단위로 다시 만들기 시작했다.
첫 단계는 외곽 프레임만 제작했고, 서버/ItemsAdder에는 적용하지 않았다.

생성 파일:

```text
assets/generated/gui_samples/expedition_frame_blue_steel_183x255.png
assets/generated/gui_samples/expedition_frame_blue_steel_183x255_preview3x.png
```

검증 결과:

```text
size: 183x255
mode: RGBA
transparent center area: present
```

다음 단계:

```text
상단 타이틀 영역 제작
```
### 1단계 수정: 상자 영역 전용 메탈 외곽

2026-06-06에 외곽 프레임을 다시 수정했다.

수정 기준:

```text
캔버스 크기: 183x255 유지
상자 GUI 영역: y=0..125에만 프레임 표시
유저 인벤토리 영역: y>=126 완전 투명
장식 요소: 제거
남기는 요소: 철판 느낌의 단순 블루 스틸 그라데이션 외곽선
```

검증 결과:

```text
size: 183x255
mode: RGBA
lower_nontransparent: 0
```
