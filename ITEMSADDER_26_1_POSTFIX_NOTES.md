# ItemsAdder 26.1 Post-Fix Notes

작성일: 2026-06-07 KST

## 왜 이전 적용이 반복 실패했는가

이전 실패의 핵심 원인은 단순 PNG 누락이 아니었다.

Minecraft 26.1 계열 클라이언트는 한 item model 안에서 `items.png` atlas와
`blocks.png` atlas를 동시에 참조하면 모델 bake를 실패한다. 실제 클라이언트
로그의 핵심 오류는 다음과 같았다.

```text
Unable to bake item model: minecraft:paper
Unable to bake item model: minecraft:shield
Multiple atlases used in model
```

DefaultPack의 많은 설치형 블럭/GUI 아이콘은 `material: PAPER`에 묶여 있었고,
그 중 일부 모델은 block texture를, 일부 모델은 item texture를 참조했다. 그래서
`minecraft:paper` item definition 하나가 item atlas와 block atlas를 섞게 됐다.

방패도 마찬가지였다. custom shield 모델 안에서 `minecraft:block/anvil` 같은
block atlas 텍스쳐와 `_iasurvival:item/shields/...` item atlas 텍스쳐가 같이
참조되어 `minecraft:shield` bake가 실패했다.

이전 실패 방식:

- live `generated.zip`의 atlas에 누락 texture를 계속 추가했다.
- 원본 model JSON을 광범위하게 rewrite했다.
- `/iazip` 결과물과 기존 수동 리소스(TAB 로고/HUD/랭크 이미지)를 직접 섞었다.

이 방식은 문제의 근본인 “한 item model 안의 atlas 혼합”을 분리하지 못했고, 오히려
기존 정상 리소스까지 깨뜨렸다.

성공한 방식:

- 원본 placed block 모델은 건드리지 않았다.
- item/GUI/held 표시용 모델만 `iafix` namespace로 복제했다.
- 복제한 표시용 모델의 texture를 item atlas 전용 경로로 분리했다.
- `minecraft:paper`, `minecraft:shield` item definition은 `iafix` 표시용 모델만
  참조하게 했다.
- 후보 ZIP에서 atlas 혼합이 사라진 것을 검증한 뒤 live에 적용했다.

## 2026-06-07 방패 위치 보정

리소스 적용 성공 후 방패 텍스쳐와 모델은 보였지만, 손/몸 기준 위치가 맞지 않았다.
이는 atlas 문제가 아니라 `display` transform 문제였다.

기존 `iafix` 방패 모델은 DefaultPack의 custom display 값을 그대로 들고 있었다.
예를 들면 non-blocking 모델의 `thirdperson_righthand`가 다음처럼 되어 있었다.

```json
{"rotation":[0,0,0],"translation":[8,-2,1],"scale":[1,1,1]}
```

로컬 26.1.2 클라이언트 jar의 vanilla shield 기준값은 다음과 같았다.

```json
{"rotation":[0,90,0],"translation":[10,6,-4],"scale":[1,1,1]}
```

따라서 `generated.zip` 안의 `iafix` 방패 표시 모델 10개만 보정했다.

수정 대상:

```text
assets/iafix/models/item/generated/iasurvival/item/shields/*_shield.json
assets/iafix/models/item/generated/iasurvival/item/shields/*_shield_blocking.json
```

건드리지 않은 것:

```text
paper.json
shield.json
atlas JSON
TAB logo
HUD compass
BetterRanks images
original placed block models
ItemsAdder source configs
```

적용 후 SHA:

```text
d6610a69323127f0860c647784c7d256673b43bf
```

백업:

```text
/opt/minecraft/backups/itemsadder-shield-display-20260607-064648
```

## REAL_NOTE 세로 스택 노출 현상

커스텀 블럭을 세로로 쌓고 파괴할 때 주크박스/노트블럭처럼 보이는 현상은 atlas
오류와 별개다.

현재 DefaultPack의 많은 설치형 블럭은 ItemsAdder 설정에서 다음 구조를 사용한다.

```yaml
specific_properties:
  block:
    placed_model:
      type: REAL_NOTE
```

`REAL_NOTE`는 실제 서버 블럭으로 vanilla `note_block` 상태를 사용하고, 리소스팩이
특정 note block state를 custom model로 매핑한다. 그런데 note block은 아래 블럭에
따라 `instrument` 상태가 바뀔 수 있다. 커스텀 블럭을 세로로 쌓거나 파괴하면 주변
블럭 갱신 때문에 note block state가 재계산되고, 그 순간 custom model 매핑이 빠져
바탕 note block이 보일 수 있다.

이 현상은 리소스팩 ZIP이나 atlas를 더 만져서 해결할 문제가 아니다.

권장 대응:

- `REAL_NOTE` 블럭은 세로 스택이 많은 건축 블럭으로 쓰지 않는다.
- 세로 스택이 필요한 solid block은 별도 테스트 후 더 적합한 ItemsAdder block type
  으로 전환한다.
- 전환은 전체가 아니라 한 블럭만 격리 테스트한다.
- 기존 배치 데이터가 있는 블럭 타입을 바로 바꾸면 월드에 놓인 블럭 ID/상태가
  흔들릴 수 있으므로 live 전체 변경은 금지한다.
