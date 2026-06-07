# AdvancedEnchantments Balance Review

작성일: 2026-06-04 KST

## 현재 상태

| 항목 | 상태 |
|---|---|
| 플러그인 | AdvancedEnchantments 9.23.0 |
| 서버 | survival |
| 한글화 | `locale/ko.yml` 존재, 이번 작업에서 수정하지 않음 |
| 주요 설정 파일 | `/opt/minecraft/server/plugins/AdvancedEnchantments/enchantments.yml` |
| 메뉴 파일 | `menus/enchanter.yml`, `menus/alchemist.yml`, `menus/tinkerer.yml` 등 |
| 커스텀 장비 | `armorSets/`, `customWeapons/` 존재 |

이번 문서는 밸런스 리뷰이며 설정을 변경하지 않았다.

## 읽기 전용 점검 결과

확인된 파일:

```text
anvil.yml
config.yml
enchantmentTable.yml
enchantments.yml
groups.yml
lootConfiguration.yml
mobHeads.yml
mobs.yml
pdata.yml
locale/ko.yml
menus/*.yml
armorSets/*.yml
customWeapons/*.yml
```

`enchantments.yml`에서 확인된 인챈트 예시:

```text
harvest, autoreel, gemify, planter, replanter, strike, impact,
jellylegs, wings, aquatic, smelting, experience, hasten, haste,
rebreather, glowing, decapitation, forcefield, famine, berserk,
reflect, explosive, frenzy, featherweight, poisonedhook, firehook,
molten, ravenous, veinminer, telepathy, enderslayer, deathpunch,
bonecrusher, immolate, slayer, hunter, beastslayer, bait, lucky,
permafrost, soulless, reaper, blind, allure, frozen, paralyze,
poison, virus, snare, springs, voodoo, wither, shockwave, vampire,
greatsword, bowmaster, hardened, rocketescape, trickster, timber,
distance, cleave
```

전체 인챈트 수가 많으므로, 바로 수정하지 않고 위험도 높은 그룹부터 분류한다.

## 밸런스 기준

| 위험도 | 기준 | 처리 |
|---|---|---|
| HIGH | PVP 승패를 직접 뒤집거나, 이동/제어/광역 피해를 크게 바꾸는 효과 | 우선 검토, 필요 시 비활성화 후보 |
| MEDIUM | 파밍 속도, 내구도, 자동화, 편의성을 크게 올리는 효과 | 경제 영향 계산 후 조정 |
| LOW | 수중 호흡, 발광, 낙하 완화 등 편의 중심 | 유지 후보 |

## 우선 검토 대상

| 인챈트 | 예상 적용/효과 | 위험도 | 판정 | 이유 |
|---|---|---|---|---|
| `forcefield` | 근접 접근 제어 | HIGH | REVIEW | PVP에서 접근 자체를 막을 수 있음 |
| `paralyze` | 행동 제한 | HIGH | REVIEW | 이동/공격 제어는 PVP 체감이 큼 |
| `frozen` | 이동 제한 | HIGH | REVIEW | PVP/던전 모두에서 군중제어 과다 가능 |
| `blind` | 시야 방해 | HIGH | REVIEW | PVP 불쾌감이 커질 수 있음 |
| `vampire` | 흡혈 | HIGH | REVIEW | 장비 격차를 크게 키울 수 있음 |
| `greatsword` | 공격력 강화 계열 | HIGH | REVIEW | PVP 피해량 상한 확인 필요 |
| `cleave` | 광역 피해 | HIGH | REVIEW | 국가전/던전에서 과도할 수 있음 |
| `explosive` | 폭발/광역 파괴 계열 | HIGH | REVIEW | 영토 보호/던전 보호와 충돌 가능 |
| `shockwave` | 넉백/광역 효과 | HIGH | REVIEW | PVP 위치전 밸런스 영향 큼 |
| `reaper` | 처치/흡수 계열 가능성 | HIGH | REVIEW | 처치 보상과 연쇄 전투 영향 확인 필요 |
| `berserk` | 피해량/상태 강화 | HIGH | REVIEW | 고랭크 PVP에서 폭증 가능 |
| `reflect` | 피해 반사 | HIGH | REVIEW | PVP 판정이 직관적이지 않을 수 있음 |
| `veinminer` | 광물 대량 채굴 | MEDIUM | LIMIT | 초반 경제 인플레이션 위험 |
| `telepathy` | 자동 회수 | MEDIUM | LIMIT | 드랍/청소/파밍 루프 영향 |
| `timber` | 나무 대량 채집 | MEDIUM | LIMIT | 자원 수급 속도 증가 |
| `harvest` | 농사 자동화 | MEDIUM | LIMIT | LeeSeolJobs 보상과 중복 가능 |
| `autoreel` | 낚시 편의 | MEDIUM | LIMIT | 낚시 돈 수급과 중복 가능 |
| `planter` 계열 | 자동 심기 | MEDIUM | LIMIT | 농사 자동화 악용 가능 |
| `smelting` | 자동 제련 | MEDIUM | REVIEW | LeeSeolCrafting 가공 시스템과 역할 충돌 |
| `experience` | 경험치 증가 | MEDIUM | REVIEW | 랭크/성장 재화와 충돌 가능 |
| `jellylegs` | 낙하 완화 | LOW | KEEP | PVP 직접 영향 낮음 |
| `aquatic` | 수중 편의 | LOW | KEEP | 특수 상황 편의 중심 |
| `rebreather` | 호흡 편의 | LOW | KEEP | 직접 경제/PVP 영향 낮음 |
| `glowing` | 발광 | LOW | KEEP | 편의성 중심 |

## 서버 콘텐츠별 영향

### 초반 경제

주의 대상:

- `veinminer`
- `timber`
- `harvest`
- `autoreel`
- `planter`
- `telepathy`
- `smelting`

이 인챈트들은 `LeeSeolJobs`의 광질/농사/낚시 보상과 결합되면 돈 생성량을 크게 올릴 수 있다.

권장:

- 초반 랭크에서는 제한
- 보상 쿨다운 또는 일일 보상 제한과 함께 설계
- 설치 후 재채굴 악용 방지와 같이 검토

### PVP

주의 대상:

- `forcefield`
- `paralyze`
- `frozen`
- `blind`
- `vampire`
- `reflect`
- `greatsword`
- `cleave`
- `shockwave`

권장:

- 국가전 전까지는 일부 비활성 또는 고랭크 전용
- 던전 전용/PVP 금지 구분 검토
- 피해량, 발동 확률, 쿨다운을 먼저 확인

### 던전

주의 대상:

- 광역 피해
- 흡혈
- 군중제어
- 자동 회수

권장:

- 던전 파밍용 장비와 PVP 장비를 분리할지 결정
- 던전에서만 허용할 인챈트와 금지할 인챈트 분류 필요

### 제작/수리

`smelting`, `experience`, 자동화 계열은 `LeeSeolCrafting`의 가공/수리/제작 루프와 역할이 겹칠 수 있다.

권장:

- `LeeSeolCrafting` 1차 구현 전에는 AdvancedEnchantments 수치 수정 금지
- 제작 시스템 설계 후 “제작으로 얻는 가치”와 “인챈트로 얻는 가치”를 분리

## 수정 원칙

1. 한글화 파일은 건드리지 않는다.
2. 전체 설정 초기화 금지.
3. 바로 삭제하지 않고 비활성화/확률 조정/랭크 제한부터 검토.
4. 변경 전 `/opt/minecraft/server/plugins/AdvancedEnchantments/` 백업.
5. PVP 고위험 인챈트부터 소규모 변경.
6. 변경 후 survival만 재시작하고 오류 확인.

## 다음 검토 작업

1. `enchantments.yml`에서 HIGH 위험도 인챈트의 실제 효과, 발동 확률, 쿨다운 확인.
2. PVP 금지 후보와 던전 전용 후보 분리.
3. LeeSeolJobs 설계 후 파밍 자동화 인챈트의 경제 영향 재계산.
4. LeeSeolCrafting 설계 후 `smelting` 등 가공 역할 중복 여부 결정.
