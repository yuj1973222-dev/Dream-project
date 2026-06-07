# LeeSeolHUD Compass Unicode Diagnosis

작성일: 2026-06-04 KST

## 증상

Survival 서버에서 BossBar 나침반 이미지가 보이지 않고, 나침반용 특수 유니코드 문자가 그대로 보인다.

## 확인한 사실

원격 서버에서 읽기 전용 진단을 수행했다.

- `resourcepack=active`
- `velocity=active`
- `minecraft=active`
- `lobby=active`
- `newworld=inactive`
- Velocity resource pack SHA, hosted `generated.zip` SHA, 실제 다운로드 SHA가 모두 일치한다.
- 현재 SHA:
  `6a6a7325e3ea6db267196700db68f105aa89c19b`
- `assets/minecraft/font/default.json` 안에 `leeseolhud:gui/compass/...` provider가 존재한다.
- compass provider 수: `24`
- compass codepoint 수: `24`
- compass codepoint 범위: `U+E340` through `U+E357`
- compass codepoint 중복: 없음
- 전체 font provider 기준 compass codepoint 중복: 없음

## 핵심 문제 후보

현재 provider는 이미지 1장을 문자 1개에 매핑한다.

예:

```text
provider file=leeseolhud:gui/compass/compass_000.png
image=420x24
chars_per_row=1
cell_width=420.0
height=24
ascent=20
```

이 구조는 단일 glyph 하나의 폭이 `420px`이다.

이 프로젝트에서는 이전에도 큰 단일 font glyph가 클라이언트에서 정상 렌더링되지 않은 적이 있다. `SERVER_STATE.md`와 `AGENTS.md`에도 “큰 PNG 하나를 Minecraft font glyph로 쓰지 말 것”이 반복 실수로 기록되어 있다.

따라서 이번 문제의 가장 가능성 높은 원인은 다음이다.

```text
나침반 resource pack 자체는 적용되었지만,
BossBar가 사용하는 compass glyph 1개의 폭이 너무 커서
클라이언트 font renderer가 해당 glyph를 정상 렌더링하지 못하고
private-use unicode fallback을 표시한다.
```

## 수정 원칙

다른 플러그인, TAB 전체 설정, ItemsAdder 전체 설정은 건드리지 않는다.

수정 대상은 다음으로 제한한다.

- `LeeSeolHUD`
- `assets/generated/leeseolhud_compass`
- hosted `generated.zip` 안의 `leeseolhud` compass font provider
- Velocity resource pack SHA

## 최소 수정 계획

1. 420px 나침반 이미지는 유지한다.
2. 단일 glyph 폭 `420px` 구조를 제거한다.
3. 각 나침반 이미지를 3개 glyph cell로 나누어 매핑한다.
   - 전체 이미지 폭: `420px`
   - glyph segment 수: `3`
   - glyph 1개당 폭: `140px`
4. `LeeSeolHUD`는 한 방향당 glyph 1개가 아니라 glyph 3개를 연속 출력한다.
5. 기존 `U+E340` through `U+E357` 단일 glyph 매핑은 제거한다.
6. 새 glyph 범위는 `U+E340` through `U+E387`을 사용한다.
   - 24 headings x 3 segments = 72 glyphs
7. 배포 후 다음을 검증한다.
   - hosted pack SHA 일치
   - compass PNG 수 24개
   - compass provider 수 24개
   - compass char 수 72개
   - 각 provider의 `chars_per_row=3`
   - 각 glyph cell 폭 `140px`
   - 중복 codepoint 없음
   - `LeeSeolHUD`, `TAB` 로딩 오류 없음

## 별도 발견 사항

Survival ItemsAdder가 `/ia` reload 또는 pack 처리 중 `contents/leeseolhud/textures/gui/compass/*.png`에 대해 `Permission denied`를 출력했다.

이는 source content 파일이 root 소유로 복사되어 ItemsAdder가 metadata stripping을 하지 못한 상태로 보인다. hosted pack 직접 패치와는 별개지만, 추후 ItemsAdder reload 시 오류를 유발할 수 있으므로 `contents/leeseolhud` 범위에 한해서 ownership을 서버 사용자에게 맞추는 것이 안전하다.

이 ownership 보정은 `leeseolhud` namespace만 대상으로 해야 하며, 다른 ItemsAdder namespace는 건드리지 않는다.

## 2026-06-04 후속 수정 결과

초기 최소 수정은 24 headings x 3 segments 구조였으나, 이후 디자인 요구사항에 맞춰 최종 구조를 다시 조정했다.

최종 구조:

- compass image count: `72`
- image heading step: `5` degrees
- label step inside image: `15` degrees
- glyph segments per heading: `16`
- glyph chars: `1152`
- glyph range: `U+E340` through `U+E7BF`
- sample image size: `1920x136`
- sample raw glyph cell width: `120px`
- sample rendered glyph cell width: `30px`
- hosted resource pack SHA1:
  `aa260c65b346d990e4420be277eeff5d6ccbb0db`

BossBar fill/background hiding:

- `LeeSeolHUD` compass still uses a Bukkit BossBar for positioning.
- The compass BossBar uses `WHITE`.
- The resource pack now overrides only WHITE BossBar sprites with transparent PNGs:
  - `assets/minecraft/textures/gui/sprites/boss_bar/white_background.png`
  - `assets/minecraft/textures/gui/sprites/boss_bar/white_progress.png`
- This is intentionally scoped to WHITE BossBars. Other bossbar colors are not
  modified.

Server-side verification passed:

- `compass_provider_count=72`
- `compass_char_count=1152`
- `sample_chars_per_row=16`
- `sample_raw_cell_width=120.0`
- `sample_rendered_cell_width=30.0`
- transparent WHITE BossBar sprites exist and are `182x5`
- `resourcepack`, `velocity`, and `minecraft` are active
- `newworld` remains inactive
- `LeeSeolHUD` and `TAB` loaded without parser errors
- direct command `/compasshud <on|off>` is present in the deployed jar
- compass labels no longer draw the old black shadow layer
