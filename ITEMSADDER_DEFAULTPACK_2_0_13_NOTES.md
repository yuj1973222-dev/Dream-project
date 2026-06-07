# ItemsAdder DefaultPack 2.0.13 적용 기록

작성일: 2026-06-06 KST

## 현재 상태

- Live resource pack URL: `http://34.64.126.179:8163/generated.zip`
- Live SHA1: `2630a0cc5121193010725873906b58a7e1962600`
- 적용된 DefaultPack namespaces:
  - `iaalchemy`
  - `iafestivities`
  - `iageneric`
  - `iasurvival`
  - `iawearables`
  - `mcemojis`
  - `mcicons`
  - `twitteremojis`

## 문제 원인

DefaultPack 2.0.13을 단순히 `contents/`에 넣고 `/iareload`, `/iazip`만 실행하면
일부 모델이 보라/검정 missing texture로 보일 수 있다.

확인된 원인:

- ItemsAdder 4.0.17 beta가 생성한 model JSON이 `_b_<namespace>` 또는
  `_<namespace>` 텍스처 namespace를 참조했다.
- 하지만 해당 PNG 파일들이 `generated.zip` 안에 자동으로 포함되지 않았다.
- `short_texture_references: true` 상태에서는 `ia:숫자` 참조가 생겼지만
  `assets/ia/textures/*.png`가 누락됐다.
- `short_texture_references: false` 상태에서는 `_b_*` / `_*` 참조가 생겼지만
  해당 namespace의 PNG가 누락됐다.

## 성공한 적용 방식

1. `plugins/ItemsAdder/contents/`에 DefaultPack namespaces를 설치한다.
2. 기존 수동 리소스가 `/iazip` 중 사라지지 않도록 `contents/expedition`을 만들어
   TAB 로고 소스를 보존한다.
3. ItemsAdder config는 검증 가능한 ZIP을 위해 다음 상태로 둔다.
   - `resource-pack.zip.protect-file-from-unzip.protection_1: false`
   - `resource-pack.zip.protect-file-from-unzip.protection_2: false`
   - `resource-pack.zip.protect-file-from-unzip.protection_3: false`
   - `resource-pack.zip.protect-file-from-unzip.short_texture_references: false`
4. `/iareload` 실행 후 ItemsAdder 로드가 끝날 때까지 충분히 기다린다.
5. `/iazip`을 실행한다.
6. 생성된 `generated.zip`을 새 ZIP으로 다시 쓰면서 다음을 보정한다.
   - model JSON의 `_b_minecraft:*` / `_minecraft:*` texture reference는
     vanilla `minecraft:*`로 되돌린다.
   - `_b_<namespace>` / `_<namespace>` custom texture reference는
     `plugins/ItemsAdder/contents/**/resourcepack` 원본에서 찾아 같은 arcname으로
     `generated.zip`에 주입한다.
7. 전체 model JSON을 스캔해서 non-vanilla missing texture reference가 없는지
   확인한다.
8. ZIP 무결성, 기존 리소스, DefaultPack 핵심 리소스, hosted SHA를 확인한다.
9. 새 SHA1을 `/opt/minecraft/velocity/plugins/leeseolproxy/resourcepack.properties`에
   반영하고 `resourcepack`, `velocity`를 재시작한다.

## 최종 검증 결과

- ZIP SHA1:
  `2630a0cc5121193010725873906b58a7e1962600`
- Velocity SHA1:
  `2630a0cc5121193010725873906b58a7e1962600`
- Hosted download SHA1:
  `2630a0cc5121193010725873906b58a7e1962600`
- 기존 리소스 유지 확인:
  - expedition TAB logo `U+E301`
  - LeeSeolHUD compass `U+E340`
  - BetterRanks `admin/dev`
  - transparent bossbar/ping sprites
  - PixieStudios GUI texture
- DefaultPack 핵심 리소스 확인:
  - `assets/iaalchemy/models/item/mysterious_artifact.json`
  - `assets/iageneric/textures/item/bug_medal.png`
  - `assets/iasurvival/textures/item/ores/ruby.png`
  - `assets/mcicons/textures/item/icon_confirm.png`
- model JSON missing texture scan 통과.

## 2026-06-07 실패한 추가 보정

- 증상:
  - 일반 아이템, 모자, 장신구는 정상.
  - 블럭 아이콘, ItemsAdder GUI 아이콘, 방패가 손에 들거나 GUI에 있을 때
    missing texture로 보임.
  - 블럭은 설치 후 월드에서는 정상 렌더링됨.
- 시도:
  - 기존 ZIP을 백업한 뒤 `/iazip` 없이 `items.json` atlas만 보강했다.
  - `ia_overlay_modern_atlas`의 block atlas source와 private namespace PNG
    sprite를 item atlas에 병합했다.
- 결과:
  - 이미 설치되어 보이던 블럭 렌더링까지 깨졌다.
  - 들고 있는 아이템/블럭/방패 missing texture도 고쳐지지 않았다.
  - 즉시 롤백했다.
- 결론:
  - atlas 병합 방식은 현재 live pack에 맞지 않는다.
  - 다음 진단은 ItemsAdder가 생성한 item definition, custom_model_data 값,
    모델 parent/material 조합, 클라이언트 리소스팩 로그를 기준으로 진행한다.
- 백업:
  - `/opt/minecraft/backups/itemsadder-held-item-atlas-20260607-044505`

## 2026-06-07 두 번째 실패 보정

- 시도:
  - 클라이언트 로그의 `Unable to bake item model: minecraft:paper`,
    `Unable to bake item model: minecraft:shield`,
    `Multiple atlases used in model`을 기준으로 generated ZIP을 직접 수정했다.
  - `paper` custom item model은 item atlas 전용 `iaitem:*` 텍스처 복사본으로
    분리하고, shield model은 block atlas 전용 `_b_iasurvival:*`로 분리했다.
- 결과:
  - 기존과 같은 문제를 해결하지 못했다.
  - 방패는 깨진 텍스처 표시마저 사라졌다.
  - 즉시 롤백했다.
- 결론:
  - generated ZIP 내부 model JSON/atlas rewrite 방식은 live pack에서 더
    진행하지 않는다.
  - 다음 수정은 반드시 별도 test pack 또는 클라이언트 로그 기반 최소 재현으로
    검증한 뒤 적용한다.
- 백업:
  - `/opt/minecraft/backups/itemsadder-split-item-block-atlas-20260607-050836`

## 다음 작업 주의사항

DefaultPack이 live 상태에서 `/iazip`을 다시 실행하면 위 보정 과정이 다시 필요하다.
그렇지 않으면 DefaultPack 장비/아이콘 일부가 다시 missing texture로 깨질 수 있다.

앞으로 리소스팩 재생성 작업을 할 때는 다음 문서도 함께 확인한다.

- `RESOURCEPACK_IMAGE_HANDOFF.md`
- `ITEMSADDER_BLOCK_RESOURCEPACK_RULES.md`
