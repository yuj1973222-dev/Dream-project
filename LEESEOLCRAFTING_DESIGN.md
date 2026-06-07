# LeeSeolCrafting Design

작성일: 2026-06-04 KST

## 결론

`LeeSeolCrafting`은 신규 Paper 플러그인으로 분리한다.

이유:

- `LeeSeolEconomy`는 돈/Vault/상점/서버 메뉴가 주 책임이다.
- 제작, 가공, 분해, 강화, 수리는 별도 밸런스와 GUI가 필요하다.
- `LeeSeolQuest`, `LeeSeolRanks`, `LeeSeolEconomy`와 연동은 하되 각 플러그인의 데이터 구조를 직접 변경하지 않는다.

ItemsAdder 가구 클릭 연동은 이 플러그인에서 직접 구현하지 않는다. GUI/resource-pack 파트가 나중에 특정 가구 클릭 시 `/forge`, `/process` 같은 명령어 또는 API를 호출하는 방식으로 연결한다.

## 범위

1차 구현 범위:

- 제작 GUI
- 광물/재료 가공 GUI
- 아이템 분해 GUI
- 장비 수리 GUI
- config 기반 레시피
- Vault 경제 차감
- 랭크별 사용 제한
- 퀘스트 objective 연동용 이벤트/API

보류 범위:

- ItemsAdder 가구 직접 감지
- 커스텀 모델 리소스팩 생성
- AdvancedEnchantments 내부 설정 변경
- 복잡한 장비 강화 확률 시스템
- 웹/DB 기반 제작 로그

## 대상 서버

| 서버 | 적용 여부 | 이유 |
|---|---|---|
| survival | YES | 제작/가공/수리의 주 사용처 |
| lobby | OPTIONAL | 튜토리얼/미리보기 GUI가 필요할 때만 |
| newworld | NO | 현재 inactive |
| velocity | NO | Paper 전용 기능 |

초기 배포는 survival만 권장한다.

## 패키지 구조

```text
LeeSeolCrafting/
  pom.xml
  src/main/resources/
    plugin.yml
    config.yml
  src/main/java/me/leeseol/crafting/
    LeeSeolCraftingPlugin.java
    command/
      CraftingCommand.java
      CraftingAdminCommand.java
    gui/
      CraftingGui.java
      ProcessingGui.java
      DisassembleGui.java
      RepairGui.java
      CraftingHolder.java
    model/
      Recipe.java
      RecipeInput.java
      RecipeResult.java
      RecipeType.java
      RepairRule.java
    service/
      RecipeService.java
      CraftingService.java
      RepairService.java
      EconomyService.java
      RankRequirementService.java
    listener/
      CraftingGuiListener.java
    event/
      LeeSeolCraftSuccessEvent.java
      LeeSeolProcessSuccessEvent.java
      LeeSeolRepairSuccessEvent.java
    hook/
      CraftingPlaceholderExpansion.java
    util/
      Text.java
      ItemStacks.java
```

## Config 설계

```yaml
settings:
  enabled: true
  allowed-worlds:
    - world
  log-success: false

gui:
  crafting:
    title: "&0제작대"
    size: 54
  processing:
    title: "&0가공"
    size: 54
  disassemble:
    title: "&0분해"
    size: 54
  repair:
    title: "&0수리"
    size: 27

repair:
  enabled: true
  base-cost: 1000
  cost-per-damage: 15
  max-cost: 100000
  allowed-types:
    - SWORD
    - AXE
    - PICKAXE
    - SHOVEL
    - HOE
    - HELMET
    - CHESTPLATE
    - LEGGINGS
    - BOOTS

recipes:
  iron_pickaxe_basic:
    type: crafting
    display-name: "&f초급 철 곡괭이"
    permission: ""
    required-rank: D
    cost: 5000
    success-rate: 1.0
    inputs:
      - material: IRON_INGOT
        amount: 3
      - material: STICK
        amount: 2
    result:
      material: IRON_PICKAXE
      amount: 1
      name: "&f초급 철 곡괭이"
      lore:
        - "&7기본 제작 장비"

  raw_iron_processing:
    type: processing
    display-name: "&f원철 가공"
    required-rank: PLAYER
    cost: 100
    success-rate: 1.0
    inputs:
      - material: RAW_IRON
        amount: 1
    result:
      material: IRON_INGOT
      amount: 1

  iron_tool_disassemble:
    type: disassemble
    display-name: "&f철 도구 분해"
    cost: 0
    success-rate: 1.0
    inputs:
      - material: IRON_PICKAXE
        amount: 1
    result:
      material: IRON_NUGGET
      amount: 9
```

## 명령어

| 명령어 | 권한 | 역할 |
|---|---|---|
| `/craftmenu` | `leeseolcrafting.use` | 제작 GUI 열기 |
| `/forge` | `leeseolcrafting.use` | 제작 GUI 열기, 가구 클릭 연동용 기본 명령 |
| `/process` | `leeseolcrafting.use` | 가공 GUI 열기 |
| `/disassemble` | `leeseolcrafting.use` | 분해 GUI 열기 |
| `/repair` | `leeseolcrafting.use` | 수리 GUI 열기 |
| `/lscrafting reload` | `leeseolcrafting.admin` | 설정 리로드 |
| `/lscrafting recipe list` | `leeseolcrafting.admin` | 레시피 목록 확인 |
| `/lscrafting recipe give <player> <recipeId>` | `leeseolcrafting.admin` | 결과 아이템 테스트 지급 |

## 권한

```text
leeseolcrafting.use
leeseolcrafting.admin
leeseolcrafting.bypass.rank
leeseolcrafting.bypass.cost
leeseolcrafting.bypass.world
leeseolcrafting.recipe.<recipeId>
```

## 데이터 저장

1차 구현에서는 별도 플레이어 데이터 저장을 만들지 않는다.

필요 시 나중에 아래 경로를 추가한다.

```text
/opt/minecraft/shared/crafting/data.yml
```

저장 후보:

- 제작 횟수
- 강화/수리 횟수
- 일일 제작 제한
- 레시피 해금 상태

현재는 레시피와 수리 규칙이 config 기반이면 충분하다.

## 경제 연동

우선순위:

1. Vault Economy API 사용
2. Vault provider가 없으면 기능 비활성화 경고 후 차감/지급 실패 처리
3. 직접 `LeeSeolEconomy` 내부 저장소에 접근하지 않음

돈 차감 순서:

1. 레시피 존재 확인
2. 월드/권한/랭크 조건 확인
3. 재료 보유 확인
4. 돈 보유 확인
5. 재료 차감
6. 돈 차감
7. 성공률 판정
8. 결과 지급 또는 실패 메시지

실패 확률이 있는 제작은 실패 시 돈/재료를 소모할지 config로 정한다.

```yaml
failure:
  consume-money: true
  consume-materials: true
```

## 랭크 연동

직접 `LeeSeolRanks` 데이터를 수정하지 않는다.

연동 방식:

- 우선 플레이어 권한으로 체크: `leeseolranks.rank.d`, `leeseolranks.rank.c` 등
- 추후 필요 시 `LeeSeolRanks` API를 추가해 직접 조회

`required-rank` 비교 순서:

```text
PLAYER < D < C < B < A < S
```

ADMIN/DEV는 `leeseolcrafting.bypass.rank` 또는 wildcard 권한으로 우회한다.

## Quest 연동

`LeeSeolCrafting`은 성공 시 Bukkit 이벤트를 발생시킨다.

```java
LeeSeolCraftSuccessEvent(player, recipeId, recipeType, result)
LeeSeolProcessSuccessEvent(player, recipeId, result)
LeeSeolRepairSuccessEvent(player, repairedItem, cost)
```

`LeeSeolQuest`는 나중에 이 이벤트를 받아 `craft-item` objective를 진행한다.

이 방식이면 `LeeSeolQuest`가 `LeeSeolCrafting` 내부 클래스를 강하게 의존하지 않아도 된다.

## GUI 동작

공통 규칙:

- InventoryHolder로 커스텀 GUI 여부 판별
- GUI 클릭은 전부 cancel
- 레시피 아이콘 클릭 시 상세/확정 GUI로 이동
- 확정 버튼 클릭 시 제작 실행
- 재료/돈/랭크 부족 시 실행하지 않고 메시지 출력
- 잘못된 Material/레시피는 로딩 시 경고 후 스킵

수리 GUI:

- 플레이어가 수리할 아이템을 지정 슬롯에 넣는다.
- 수리 비용을 GUI lore에 표시한다.
- 확정 버튼을 누르면 내구도를 회복하고 돈을 차감한다.
- 수리 불가능한 아이템은 반환한다.

## PlaceholderAPI

초기에는 필수 아님. 필요 시 추가한다.

| Placeholder | 의미 |
|---|---|
| `%leeseolcrafting_last_recipe%` | 마지막 제작 레시피 |
| `%leeseolcrafting_total_crafts%` | 누적 제작 횟수 |
| `%leeseolcrafting_repair_cost%` | 손에 든 아이템 예상 수리비 |

## 구현 순서

1. 플러그인 골격 생성
2. config 레시피 로더 작성
3. Vault economy hook 작성
4. 제작 GUI + 확정 GUI 작성
5. 재료/돈 차감 및 결과 지급 구현
6. 수리 GUI 구현
7. reload/admin 명령어 구현
8. 성공 이벤트 발행
9. survival 서버에만 배포
10. 플레이어 테스트 후 Quest objective 연동

## 검증 기준

| 항목 | 기준 |
|---|---|
| 서버 로딩 | `LeeSeolCrafting enabled` 후 오류 없음 |
| 레시피 로딩 | 유효 레시피 수 요약 로그 출력 |
| 잘못된 Material | 서버 크래시 없이 해당 레시피 스킵 |
| 제작 성공 | 재료/돈 차감 후 결과 지급 |
| 제작 실패 | config 기준으로 재료/돈 처리 |
| 돈 부족 | 재료 차감 없이 실패 |
| 재료 부족 | 돈 차감 없이 실패 |
| 랭크 부족 | 돈/재료 차감 없이 실패 |
| 수리 성공 | 내구도 회복, 돈 차감 |
| reload | config 변경 반영 |

## 다음 결정 필요

구현 전에 확정할 항목:

1. 수리 비용 공식
2. 제작 실패 시 재료/돈 소모 여부
3. 제작 GUI 첫 화면 분류
4. 레시피 해금 시스템을 1차에 넣을지 여부
5. survival만 배포할지, lobby에도 미리보기용으로 배포할지 여부
