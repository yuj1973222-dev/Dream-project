# ItemsAdder 블럭/리소스팩 적용 유의사항

작성일: 2026-06-06 KST

이 문서는 Crates and Stuff 같은 커스텀 블럭/가구 리소스를 다시 서버에 적용할 때
반복 실수를 막기 위한 작업 규칙이다. live 서버에 적용하기 전에 반드시 이 문서를
먼저 확인한다.

## 기준 문서

- ItemsAdder Blocks 공식 문서:
  <https://itemsadder.devs.beer/adding-content/blocks>
- ItemsAdder Block 기본 추가 문서:
  <https://itemsadder.devs.beer/plugin-usage/adding-content/block>
- ItemsAdder My First Block 초급 문서:
  <https://itemsadder.devs.beer/plugin-usage/beginners/my-first-block>
- ItemsAdder Resourcepack Merge 문서:
  <https://itemsadder.devs.beer/adding-content/merge-resourcepacks>

## 이번에 배운 핵심

- 설치 가능한 상자 모델은 단순 `PAPER` 커스텀 모델 아이템이 아니다.
- 설치 가능한 블럭은 ItemsAdder `specific_properties.block.placed_model` 구조로
  등록해야 한다.
- chest처럼 열리는 기능은 리소스팩/블럭 등록만으로 자동 제공되지 않는다.
  상자 열기 기능은 나중에 별도 플러그인 리스너 또는 ItemsAdder 상호작용 설정으로
  구현한다.
- `/iazip` 결과물을 기존 정상 리소스팩에 단순 ZIP 병합하면 안 된다.
  기존 font/model/atlas override가 덮여 TAB 로고, HUD, 랭크 이미지, 기존 아이템
  텍스쳐가 깨질 수 있다.
- live 서버에 바로 시도하지 말고, known-good pack과 신규 ItemsAdder 생성 pack을
  오프라인에서 JSON 단위로 비교한 뒤 최소 병합해야 한다.

## My First Block 문서에서 확인한 필수 규칙

- 블록 추가 작업을 시작하기 전에 리소스팩 호스팅 방식을 먼저 정해야 한다.
  현재 서버는 Velocity가 단일 리소스팩 URL/SHA를 배포하고 `resourcepack`
  서비스가 `generated.zip`을 호스팅하는 구조다. 이 구조를 유지할 경우
  ItemsAdder `/iazip` 결과와 Velocity SHA를 반드시 같은 파일 기준으로 맞춘다.
- 블록 config는 `plugins/ItemsAdder/contents/<namespace>/configs/` 아래에 둔다.
  예: `contents/crates_and_stuff/configs/blocks.yml`
- 텍스쳐는 namespace 기준 경로에 둔다.
  예: `contents/crates_and_stuff/textures/block/common_crate.png`
- 대체 경로로 `contents/<namespace>/resourcepack/assets/<namespace>/textures/block/`
  도 가능하지만, 한 namespace 안에서 경로 방식을 섞지 않는다.
- `resource.material`은 `PAPER` 같은 설치 불가능한 바닐라 아이템을 사용한다.
  `STONE`, `DIRT` 같은 바닐라 블록을 material로 쓰면 설치 시 glitch가 생길 수 있다.
- 단순 2D/정육면체 블록은 `generate: true`와 `texture` 또는 `textures`를 사용해
  ItemsAdder가 모델을 자동 생성하게 둘 수 있다.
- Blockbench에서 가져온 3D 상자 모델은 자동 생성 대상이 아니므로
  `generate: false`와 `model_path: block/<model_name>` 방식으로 별도 모델 JSON을
  정확히 연결해야 한다.
- 실제 설치 가능한 블록이 되려면 `specific_properties.block.placed_model`이 필요하다.
- 인게임 지급 테스트는 `/iaget <item_id>` 기준으로 한다. `/ia` 메뉴에 보이는 것과
  실제 `/iaget` 지급, 설치, 렌더링은 각각 따로 검증한다.

## 기존 방식의 문제

이전 실패 방식:

1. Crates 모델을 아이템/furniture처럼 변환했다.
2. `/iazip`을 실행했다.
3. 기존 정상 ZIP에서 누락된 파일만 다시 복사하는 식으로 단순 병합했다.
4. SHA를 갱신하고 live 서버에 적용했다.

문제점:

- ItemsAdder가 생성한 `assets/minecraft/font/default.json`,
  `assets/minecraft/models/item/*.json`, atlas 관련 JSON 등이 기존 수동 리소스팩
  매핑을 덮었다.
- 파일이 ZIP 안에 존재해도 JSON provider/override 매핑이 바뀌면 클라이언트는
  기존 리소스를 렌더링하지 못한다.
- 그래서 새 Crates뿐 아니라 기존 정상 아이템까지 텍스쳐가 깨졌다.

## 공식 Blocks 방식으로 넣을 때의 기본 구조

예시:

```yaml
info:
  namespace: crates_and_stuff

items:
  common_crate:
    display_name: Common Crate
    permission: crates.common_crate
    resource:
      material: PAPER
      generate: false
      model_path: block/common_crate
    specific_properties:
      block:
        placed_model:
          type: REAL_NOTE
          break_particles: ITEM
        hardness: 2
        drop_when_mined: true
```

예상 파일 구조:

```text
plugins/ItemsAdder/contents/crates_and_stuff/configs/blocks.yml
plugins/ItemsAdder/contents/crates_and_stuff/models/block/common_crate.json
plugins/ItemsAdder/contents/crates_and_stuff/textures/block/common_crate_texture.png
```

주의:

- 일반 장식 블럭은 우선 `REAL_NOTE`로 시작한다.
- 투명 블럭이면 `REAL_TRANSPARENT`를 검토하지만 슬롯 제약이 있으므로 별도 확인한다.
- 방향 회전이 필요한 블럭은 `directional_mode`를 검토한다. 방향 블럭은 더 많은
  블럭 슬롯을 소비할 수 있다.
- `resource.material`은 설치 가능한 바닐라 블럭이 아니라 `PAPER` 같은 아이템
  재료를 사용한다.

## 리소스팩 병합 원칙

현재 서버는 Velocity가 단일 리소스팩을 배포한다.

유지해야 하는 기존 리소스:

- TAB 로고: `assets/expedition/...`
- BetterRanks 이미지: `assets/betterranks/...`
- LeeSeolHUD 나침반: `assets/leeseolhud/...`
- 투명 bossbar/ping 아이콘: `assets/minecraft/textures/gui/sprites/...`
- 기존 ItemsAdder 커스텀 아이템/GUI 이미지

금지:

- 기존 정상 `generated.zip`에 새 ZIP 파일을 단순 덮어쓰기.
- `/iazip` 결과물에 누락 파일만 대충 복사하기.
- `assets/minecraft/font/default.json`을 통째로 교체하기.
- `assets/minecraft/models/item/*.json` override를 통째로 교체하기.
- ItemsAdder `config.yml`을 넓은 문자열 치환으로 수정하기.
- live 서버에서 첫 테스트하기.

필수:

- known-good pack을 먼저 백업한다.
- ItemsAdder contents에 기존 수동 리소스도 공식 병합 구조로 포함한다.
  현재 TAB 로고는 `contents/expedition/`, HUD compass font JSON은
  `contents/leeseolhud/resourcepack/assets/leeseolhud/font/compass.json`로 보존한다.
- 병합이 필요하면 JSON 단위로 provider/override를 append/merge한다.
- 기존 provider/override가 삭제되지 않았는지 비교한다.
- `/iazip` 후 `unzip -t`로 ZIP 무결성을 확인한다.
- 내부 URL과 공개 URL SHA1이 Velocity 설정 SHA1과 같은지 확인한다.
- `resource-pack.zip.protect-file-from-unzip.protection_1/2/3`는 `/iazip` 검증 시
  false로 둔다.
  true이면 `generated.zip`이 intentionally protected 상태가 되어 `unzip -t` 검증이
  실패한다.
- 기존 수동 리소스가 generated.zip에만 있고 ItemsAdder source content에 없으면
  `/iazip` 때 사라진다. expedition TAB logo와 LeeSeolHUD font provider처럼 수동
  추가한 리소스는 전용 content로 편입한다. 현재 실제 사용 경로는
  `contents/expedition/`와 `contents/leeseolhud/resourcepack/.../compass.json`이다.

## 안전 적용 절차

1. 현재 상태 백업

```bash
stamp=$(date +%Y%m%d-%H%M%S)
sudo mkdir -p "/opt/minecraft/backups/itemsadder-block-apply-$stamp"
sudo cp -a /opt/minecraft/lobby/plugins/ItemsAdder "/opt/minecraft/backups/itemsadder-block-apply-$stamp/lobby-ItemsAdder"
sudo cp -a /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip "/opt/minecraft/backups/itemsadder-block-apply-$stamp/generated.zip"
sudo cp -a /opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties "/opt/minecraft/backups/itemsadder-block-apply-$stamp/resourcepack.properties"
```

2. live에 바로 넣지 말고 테스트 작업 디렉터리에서 content 구성

```text
/opt/minecraft/model-sources/crates_and_stuff/
/tmp/itemsadder-pack-test/
```

3. `common_crate` 1개만 먼저 등록

- 한 번에 여러 crate를 넣지 않는다.
- `shield`, `hat` 같은 착용/아이템 모델은 블럭 적용과 분리한다.

4. ItemsAdder 로드/빌드

```bash
sudo systemctl restart lobby
# 인게임 또는 RCON으로 /iareload, /iazip 실행
```

5. ZIP 검증

```bash
sudo unzip -t /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip
sudo unzip -l /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip | grep -E 'crates_and_stuff|expedition|betterranks|leeseolhud|boss_bar|ping_'
sha1sum /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip
```

6. JSON 매핑 검증

확인 대상:

- `assets/minecraft/font/default.json`
- `assets/minecraft/models/item/paper.json`
- ItemsAdder가 생성한 custom block/model 관련 JSON
- Crates 모델 JSON의 texture path
- TAB 로고 `\uE301` provider
- HUD 나침반 font provider
- BetterRanks 이미지 provider

7. Velocity SHA 갱신

```bash
sha1sum /opt/minecraft/lobby/plugins/ItemsAdder/output/generated.zip
sudo nano /opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties
sudo systemctl restart resourcepack velocity
```

8. 공개 URL 검증

```bash
curl -fsS http://127.0.0.1:8163/generated.zip -o /tmp/pack-internal.zip
sha1sum /tmp/pack-internal.zip
```

PC에서도 공개 URL을 내려받아 같은 SHA인지 확인한다.

9. 인게임 검증 순서

먼저 기존 기능부터 확인한다.

1. TAB 로고가 보이는지
2. 랭크 이미지가 보이는지
3. HUD 나침반이 보이는지
4. 기존 아이템 텍스쳐가 깨지지 않았는지
5. `/ia`가 정상 작동하는지
6. `common_crate`가 `/ia`에 보이는지
7. `common_crate`를 배치했을 때 텍스쳐가 보이는지

Crates가 보이더라도 기존 리소스가 하나라도 깨지면 즉시 롤백한다.

## 롤백 기준

즉시 롤백해야 하는 경우:

- 기존 TAB 로고가 사라짐
- BetterRanks 이미지가 깨짐
- HUD 나침반이 유니코드로 보임
- 기존 아이템 텍스쳐가 보라/검정으로 깨짐
- `/ia` 명령어가 작동하지 않음
- `generated.zip` `unzip -t` 실패
- 공개 URL SHA와 Velocity SHA가 불일치

롤백 후에는 클라이언트의 `.minecraft/server-resource-packs` 캐시 삭제를 안내한다.

## 다음 Crates 재시도 방침

- 목표 1단계: 열기 기능 없이 `common_crate` 블럭 텍스쳐만 정상 표시.
  - 2026-06-06 기준 lobby에 `common_crate` 1개 제한 적용은 서버 측 검증을 통과했지만
    인게임에서 같은 깨짐 증상이 반복되어 즉시 롤백했다.
  - 당시 롤백 후 known-good SHA1: `914e3812c5169660684b1a713f7f096c78b55bcb`
  - 현재 DefaultPack 2.0.13 적용 후 리소스팩 SHA1:
    `2630a0cc5121193010725873906b58a7e1962600`
  - 성공 백업: `/opt/minecraft/backups/itemsadder-common-crate-safe-20260606-064054`
  - 실패 적용 백업: `/opt/minecraft/backups/itemsadder-common-crate-block-v3-20260606-054931`
  - 실패 증상: crate 모델이 거대한 검정/보라 오브젝트로 보이고 기존 ItemsAdder
    아이템들도 깨져 보임.
  - `.bbmodel`의 `vfx` group 0두께 plane 제거, 모델 좌표 0~16 범위 정규화,
    `/iazip` 완료 후 ZIP 무결성 통과까지 대기해도 인게임 문제는 해결되지 않았다.
  - 결론: `.bbmodel` 직접 변환 방식은 중단한다. 다음 시도는 Blockbench에서 정식
    Minecraft Java block/item model JSON으로 export한 파일이나 제작자가 제공하는
    ItemsAdder/Oraxen/ModelEngine 전용 배포본을 사용해야 한다.
  - 같은 방식으로 live 서버에 다시 적용하지 말 것.
- 다음 시도 전 필수 추가 검증:
  - `.bbmodel` 변환 시 `vfx`, zero-thickness plane, animation helper 요소 제거.
  - item-in-hand용 `display` transform 추가.
  - `assets/minecraft/models/item/paper.json`뿐 아니라 1.21+ overlay의
    `assets/minecraft/items/paper.json` 구조도 같이 확인.
  - Crates 적용 전후 기존 ItemsAdder item model override 개수가 줄지 않는지 비교.
  - live 서버가 아닌 별도 테스트 pack에서 먼저 클라이언트 렌더링 확인.
- 목표 2단계: 기존 리소스팩 요소가 전부 유지되는지 검증.
- 목표 3단계: 나머지 crate를 하나씩 추가.
- 목표 4단계: 상자 열기 GUI/저장소 기능은 별도 플러그인으로 구현.

절대 한 번에 전체 pack을 적용하지 않는다.
