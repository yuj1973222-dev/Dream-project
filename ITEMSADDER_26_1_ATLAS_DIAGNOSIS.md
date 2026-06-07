# ItemsAdder 26.1 Atlas Diagnosis

작성일: 2026-06-07 KST

## 현재 증상

- ItemsAdder DefaultPack 아이템 일부가 GUI/손에 들었을 때 보라/검정 missing texture로 보인다.
- 블록은 설치하면 기능과 월드 렌더링은 동작하지만, 아이템 상태 또는 GUI 아이콘이 깨진다.
- 방패는 손에 들 때 모델/텍스처가 깨진다.
- 기존 TAB 로고, HUD, BetterRanks 같은 font image 리소스는 현재 롤백 후 정상 유지된다.

## 현재 live 상태

- Resource pack URL: `http://34.64.126.179:8163/generated.zip`
- Live SHA1 after fix: `c822a1d77d37c1497d8636fb5cb6270fdf50de26`
- Velocity resourcepack SHA1도 위 값과 일치한다.
- ItemsAdder version: `4.0.17`
- ItemsAdder 로그상 이 빌드는 Minecraft 26.1.1+용 beta build이며, production 사용 주의 경고가 출력된다.

## 클라이언트 로그 핵심

`latest.log`에서 반복 확인된 핵심 오류:

```text
Unable to bake item model: 'minecraft:shield'
java.lang.IllegalStateException: Multiple atlases used in model,
expected minecraft:textures/atlas/blocks.png,
but also got minecraft:textures/atlas/items.png
```

```text
Unable to bake item model: 'minecraft:paper'
java.lang.IllegalStateException: Multiple atlases used in model,
expected minecraft:textures/atlas/items.png,
but also got minecraft:textures/atlas/blocks.png
```

부가 오류:

```text
Pack declares support for version newer than 64,
but is missing mandatory fields min_format and max_format
```

이 부가 오류는 별도 정리 대상이다. 다만 현재 missing texture의 직접 원인은
`paper`/`shield` item model bake 실패다.

## 공식 문서와 일치하는 부분

ItemsAdder FAQ의 `Multiple atlases used in model` 항목은 같은 오류를 다룬다.

공식 문서의 핵심은 1.21.11+/26.1 계열에서 한 item model이 block atlas와 item atlas를
섞어 쓰면 클라이언트가 모델 bake를 실패한다는 것이다.

관련 설정:

```yaml
resource-pack:
  zip:
    fix_1_21_11_plus_atlas_items_models:
      enabled: true
      models:
        - paper
        - potion
```

현재 서버 config에는 lobby/survival 모두 위 설정이 이미 존재한다.
하지만 현재 pack에서는 `paper`가 여전히 실패한다.

## 실제 ZIP 구조 진단

현재 live `generated.zip`에는 26.1 overlay item definition이 있다.

- `ia_overlay_26_1_plus/assets/minecraft/items/paper.json`
- `ia_overlay_26_1_plus/assets/minecraft/items/shield.json`

진단 결과:

### minecraft:paper

- `paper.json`은 78개 model reference를 가진다.
- 같은 `minecraft:paper` item definition 안에 다음 계열이 같이 들어간다.
  - `_iainternal` / `mcicons` GUI 아이콘
  - `iasurvival` 설치형 블록
  - `iaalchemy` 가구/장치성 모델
  - `iafestivities` 장식 모델
- 그중 일부 model은 block atlas를 쓰고, 일부 model은 item atlas를 쓴다.
- 예시:
  - `iafestivities:christmas/christmas_candle`
    - `minecraft:block/glass`
    - `minecraft:block/red_concrete`
    - `_b_iafestivities:christmas/christmas_candle/fire`
    - atlas: `blocks`, `items` 둘 다
  - `iaalchemy:item/mysterious_stone`
    - `minecraft:block/stone_bricks`
    - atlas: `blocks`
  - `mcicons` / `_iainternal` GUI 아이콘
    - item atlas 계열

즉 `PAPER` 하나에 아이콘, 설치형 블록, 가구성 모델이 함께 묶여 있고,
Minecraft 26.1 클라이언트가 이를 하나의 item model로 bake하면서 atlas 혼합 오류를 낸다.

### minecraft:shield

`shield.json`은 custom shield model들을 포함한다.

예시:

```json
"textures": {
  "anvil_base": "minecraft:block/anvil",
  "base": "_iasurvival:item/shields/ruby_shield",
  "particle": "#anvil_base"
}
```

이 구조는 한 shield model 안에서:

- `minecraft:block/anvil` → block atlas
- `_iasurvival:item/shields/ruby_shield` → item atlas

를 동시에 사용한다. 그래서 `shield`도 같은 atlas 혼합 오류가 난다.

## 실패했던 방식

아래 방식은 이미 실패했으므로 반복 금지.

1. live `generated.zip`의 item atlas에 private namespace sprite를 강제로 추가
   - 결과: 이미 설치된 블록 렌더링까지 깨짐.
2. live `generated.zip` 내부 model JSON/atlas를 직접 rewrite
   - 결과: held item 문제를 해결하지 못했고 shield가 더 나빠짐.

실패 원인:

- source config와 ItemsAdder generator가 만드는 구조를 그대로 둔 채 최종 ZIP만 바꿨다.
- `paper` item definition 전체가 atlas를 섞어 쓰는 구조적 문제를 해결하지 못했다.
- live pack에 직접 적용해서 기존 정상 리소스까지 같이 위험해졌다.

## 현재 원인 결론

이번 문제는 Crates and Stuff만의 문제가 아니다.

현재 DefaultPack 자체가 Minecraft 26.1 / ItemsAdder 4.0.17 beta 환경에서:

- 많은 설치형 블록과 장식 모델을 `material: PAPER`에 묶고,
- 해당 모델들이 block texture와 item texture를 섞어 참조하고,
- `shield` 모델도 block atlas와 item atlas를 동시에 참조해서,
- 클라이언트가 `minecraft:paper`와 `minecraft:shield` item model bake를 실패하는 구조다.

따라서 단순히 PNG 파일 누락을 복사하거나 atlas에 추가하는 방식은 근본 해결이 아니다.

## 안전한 해결 방향

live pack을 바로 수정하지 않는다.

### 1단계: test pack 생성

- 현재 live pack을 기준으로 별도 test pack을 만든다.
- Velocity SHA는 변경하지 않는다.
- 클라이언트에 배포하기 전 ZIP 내부 검증만 한다.

### 2단계: 공식 fix 설정 보완

현재:

```yaml
models:
  - paper
  - potion
```

추가 후보:

```yaml
models:
  - paper
  - potion
  - shield
```

단, `paper`는 이미 포함되어도 실패 중이므로 이것만으로 충분하다고 판단하지 않는다.

### 3단계: source-level 수정 검토

최종 ZIP 직접 rewrite가 아니라 ItemsAdder source content 기준으로 수정해야 한다.

검토 대상:

- `material: PAPER`를 쓰는 설치형 블록/장식/가구 모델
- `resource.generate: true`인데 `textures: block/...`를 쓰는 항목
- `model_path`가 block texture를 참조하는데 base item이 `PAPER`인 항목
- shield model의 `minecraft:block/anvil` 참조

가능한 수정 방향:

- held item / GUI 표시용 model은 item atlas만 쓰게 별도 item-display model을 만든다.
- placed block model은 block atlas를 유지한다.
- shield model은 item atlas만 쓰도록 `minecraft:block/anvil` 의존을 제거하거나 item-atlas용 복사 텍스처로 분리한다.
- ItemsAdder 4.0.17 beta보다 최신 빌드가 이 문제를 수정했는지 확인한다.

### 4단계: ZIP 내부 검증

배포 전에 반드시 다음이 0이어야 한다.

- `minecraft:paper` item definition에서 block atlas와 item atlas 혼합
- `minecraft:shield` item definition에서 block atlas와 item atlas 혼합

또한 기존 리소스가 유지되어야 한다.

- expedition TAB logo
- LeeSeolHUD compass
- BetterRanks images
- transparent bossbar/ping sprites
- PixieStudios GUI textures

### 5단계: live 배포

test pack 검증이 통과한 뒤에만:

1. live `generated.zip` 백업
2. test pack 교체
3. SHA1 계산
4. Velocity resourcepack SHA 갱신
5. resourcepack/velocity restart
6. 클라이언트 cache 삭제 후 접속 테스트

## 다음에 절대 하지 말 것

- live `generated.zip` 내부 model JSON/atlas를 즉흥 rewrite하지 않는다.
- `_b_*` texture를 item atlas와 block atlas에 동시에 넣는 방식으로 해결하려 하지 않는다.
- `/iazip` 후 바로 Velocity SHA를 갱신하지 않는다.
- Crates and Stuff를 다시 live에 넣어 확인하지 않는다.
- 기존 TAB/HUD/rank/font 리소스를 포함한 pack 전체를 초기화하지 않는다.

## 다음 작업의 판단 기준

해결 여부는 인게임 감각이 아니라 먼저 아래 조건으로 판정한다.

```text
latest.log:
  Unable to bake item model: 'minecraft:paper' 없음
  Unable to bake item model: 'minecraft:shield' 없음
  Multiple atlases used in model 없음
```

그리고 인게임에서 확인한다.

- `/ia` GUI 아이콘 정상
- 설치형 블록 아이템 아이콘 정상
- 설치형 블록 배치 후 정상
- shield held model 정상

## 2026-06-07 적용 결과

공식 설정만으로는 실패했다.

- `fix_1_21_11_plus_atlas_items_models.models`에 `shield`를 추가하고 lobby를
  재시작해 pack을 재생성했다.
- ItemsAdder 로딩은 성공했지만, 검증 결과 `minecraft:paper`와
  `minecraft:shield`의 atlas 혼합이 그대로 남았다.
- 이 상태는 자동 롤백했다.

성공한 방식은 `iafix` candidate pack이다.

- live pack을 바로 수정하지 않고 `/tmp/generated-iafix-candidate.zip`을 먼저 만들었다.
- `minecraft:paper`와 `minecraft:shield` item definition에서 참조하는 표시용
  모델만 `iafix` namespace로 복제했다.
- 원본 placed block 모델은 수정하지 않았다.
- 필요한 vanilla/custom 텍스처 105개를 `assets/iafix/textures/item/...`로
  복사하고 item atlas에 등록했다.
- duplicate display model 87개를 생성했다.
- candidate 검증에서 `paper`/`shield` atlas 혼합이 사라진 것을 확인한 뒤에만
  live pack으로 교체했다.
- 새 SHA1:
  `c822a1d77d37c1497d8636fb5cb6270fdf50de26`
- public URL 다운로드 SHA1도 같은 값으로 확인했다.
- 배포 전 백업:
  `/opt/minecraft/backups/itemsadder-iafix-candidate-20260607-062154`
- 기존 TAB 로고/HUD/rank 이미지 정상
