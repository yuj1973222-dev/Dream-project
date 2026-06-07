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

## 2026-06-08 실제 수치 기반 후속 검토

이번 후속 검토는 `/opt/minecraft/server/plugins/AdvancedEnchantments/`의 라이브
설정을 읽기 전용으로 내려받아 진행했다. 설정은 변경하지 않았다.

확인 파일:

- `enchantments.yml`
- `groups.yml`
- `config.yml`
- `enchantmentTable.yml`
- `anvil.yml`
- `menus/enchanter.yml`

### 획득 경로와 비용

| 경로 | 현재 설정 | 밸런스 의미 |
|---|---|---|
| `/enchanter` | 권한 제한 꺼짐, 확인 GUI 켜짐 | 모든 일반 플레이어가 EXP로 무작위 책 구매 가능 |
| 일반 `SIMPLE` | `400 EXP` | 초반 접근성이 높음 |
| 고급 `UNIQUE` | `800 EXP` | 초반~중반 접근 가능 |
| 엘리트 `ELITE` | `2500 EXP` | 중반 접근 가능 |
| 궁극 `ULTIMATE` | `5000 EXP` | 중후반 접근 가능 |
| 전설 `LEGENDARY` | `25000 EXP` | 고비용 |
| 신화 `FABLED` | `40000 EXP` | 고비용 |
| 인챈트 테이블 | 커스텀 인챈트 확률 `45%` | `/enchanter` 외 획득 루트가 열려 있음 |
| 테이블 그룹 확률 | `SIMPLE 50`, `UNIQUE 40`, `ELITE 30`, `ULTIMATE 20`, `LEGENDARY 10` | 저등급뿐 아니라 엘리트/궁극도 자연 획득 가능 |
| 커스텀 책 테이블 획득 | `3%` | 책 형태 획득은 낮음 |

현재 구조에서는 PVP 고위험 인챈트가 “비싸서 통제된다”고 보기 어렵다. 특히
`forcefield`는 `SIMPLE`이라 400 EXP 무작위 책 풀에 들어가고, `explosive`,
`reflect`, `berserk`, `telepathy`는 `UNIQUE`라 800 EXP 풀에 들어간다.

### HIGH 위험 인챈트 실제 수치 판정

| 인챈트 | 현재 핵심 수치 | 판정 | 권장 방향 |
|---|---|---|---|
| `explosive` | 활, `TNT @Victim`, 확률 `20~60%`, 쿨다운 `7~9s`, `UNIQUE` | 즉시 제한 후보 | 영토/던전 보호 검증 전까지 비활성 후보. 최소한 PVP 전장 전에는 획득 제한 필요 |
| `reflect` | 방어구, `CANCEL_EVENT` + 받은 피해만큼 반사, 확률 `3~7%`, 쿨다운 `5s`, `UNIQUE` | 즉시 제한 후보 | PVP 판정이 불투명하므로 비활성 후보. 몹 방어용으로도 전투 로그와 킬 판정 검증 필요 |
| `greatsword` | 검, 활 든 대상 조건, 피해 증가 `45~205`, 확률 `15~55%`, 쿨다운 `2s`, `ELITE` | 즉시 제한 후보 | 수치가 과격하고 쿨다운이 짧음. 실제 피해 산식 확인 전까지 비활성 또는 대폭 하향 |
| `forcefield` | 검, 밀쳐내기 `1.0~3.0`, 확률 `30~54%`, 쿨다운 `12~13s`, `SIMPLE` | 접근성 문제 | 초반 책 풀에서 빼거나 확률/최대 레벨 하향. 단순 편의 인챈트가 아니라 PVP 거리 제어 인챈트로 취급 |
| `shockwave` | 흉갑, 체력 5 이하에서 밀쳐내기 `0.5~2.5`, 확률 `10~50%`, 쿨다운 `4s`, `ELITE` | 하향 후보 | 생존 역전용으로는 가능하나 4초마다 50%는 과함. 최대 확률과 쿨다운 조정 필요 |
| `blind` | 검, 실명 `60~100 ticks`, 확률 `17~33%`, 쿨다운 `5s`, `ELITE` | PVP 불쾌감 높음 | 최대 증폭을 낮추고 쿨다운을 늘리거나 PVP 전용 금지 후보로 분리 |
| `paralyze` | 검/도끼, 번개 + 감속 + 채굴피로 `60~100 ticks`, 확률 `7~16%`, 쿨다운 `3s`, `ELITE` | 제어 과다 | 쿨다운이 짧음. 번개 효과의 피해/시각 효과를 별도 테스트하고 PVP에서는 제한 권장 |
| `frozen` | 방어구, 공격자 감속 `40~60 ticks`, 확률 `19~26%`, 쿨다운 `6s`, `ELITE` | 조건부 유지 | 방어형 제어로는 유지 가능하나 Slow II 이상 체감이 큼. 고레벨 하향 검토 |
| `vampire` | 검, 2초 후 `1~6 HP` 회복, 확률 `7~15%`, 쿨다운 `4s`, `ELITE` | 하향 후보 | 장비 격차 확대 가능. 회복량 또는 쿨다운 조정 후 던전/PVE 중심으로 유지 검토 |
| `cleave` | 도끼, 광역 피해 `1~3`, 반경 `2~7`, 확률 `4~15%`, 쿨다운 `8~14s`, `ULTIMATE` | 전장/던전 분리 필요 | 반경 7은 큰 전투에서 영향이 큼. PVP 허용 전 최대 반경을 낮추는 후보 |
| `reaper` | 도끼, 위더+실명 `35~60 ticks`, 확률 `8~14%`, 쿨다운 `10~16s`, `ELITE` | 조건부 하향 | 쿨다운은 비교적 낫지만 실명 중복이 문제. `blind`와 같이 PVP 체감 검토 |
| `berserk` | 검/도끼, 힘 + 채굴피로 `60~100 ticks`, 확률 `4~20%`, 쿨다운 `4~8s`, `UNIQUE` | 하향 후보 | 고레벨 힘 효과가 자주 돌 수 있음. PVP 전에는 고레벨 확률/쿨다운 조정 권장 |

### 경제/파밍 인챈트 실제 수치 판정

| 인챈트 | 현재 핵심 수치 | LeeSeolJobs/Crafting 영향 | 권장 방향 |
|---|---|---|---|
| `veinminer` | 광맥 전체, `15%/35%/상시`, 쿨다운 `4s/3s/없음`, `ELITE`, 고대 잔해 포함 | 광질 일일 한도 `50000원`을 빠르게 채울 수 있음 | 3레벨 상시 발동은 제한 후보. 고대 잔해 제외, 쿨다운 추가, 최대 확률 하향 검토 |
| `harvest` | 3x3 수확, `13~100%`, 쿨다운 없음, `ULTIMATE` | 농사 보상 `8~12원`, 일일 한도 `30000원`을 빠르게 채움 | 고레벨 100% 유지 시 농사 루프가 급가속됨. 쿨다운 추가 또는 최대 확률 하향 |
| `autoreel` | 자동 릴, `25/50/75/상시`, 쿨다운 없음, `ULTIMATE` | 낚시 보상 쿨다운 `1000ms`가 방어막이지만 플레이 피로도를 크게 낮춤 | Jobs 쿨다운이 실제로 적용되는지 플레이어 검증 후 조정 |
| `telepathy` | 드랍 자동 회수, `40/60/80/상시`, 쿨다운 없음, `UNIQUE` | 직접 돈 생성은 아니지만 대량 채굴/수확 효율을 올림 | 단독으로는 유지 가능. `veinminer`, `harvest`와 조합 테스트 필요 |
| `timber` | 나무 전체 벌목, `15/35/55%`, 쿨다운 `5~6s`, `ULTIMATE` | Jobs 직접 보상 없음 | 유지 후보. 보호/클레임 구역에서 정상 차단되는지 확인 필요 |
| `planter` | 쉬프트 우클릭 자동 심기, 상시, 쿨다운 `3s/2s/없음`, `ULTIMATE` | 농사 반복 피로도 감소 | 3레벨 무쿨다운은 제한 후보. 1~2초 쿨다운 유지 권장 |
| `replanter` | 완전히 자란 작물 재파종, 상시, 쿨다운 없음, `UNIQUE` | 농사 자동화 보조 | 유지 후보. `harvest`와 결합한 보상 중복 검증 필요 |
| `smelting` | 광물 자동 제련, `33/66/100%`, 쿨다운 없음, `SIMPLE` | `LeeSeolCrafting`의 원철 가공 비용 `100원`과 역할 충돌 | 초반 SIMPLE 풀에서는 과함. 제작/가공 경제 확정 전까지 하향 또는 획득 제한 후보 |
| `experience` | 광물 채굴 시 EXP `2~5`, 확률 `15~75%`, 쿨다운 `1~5s`, `SIMPLE` | EXP가 `/enchanter` 화폐라 자기 강화 루프를 만듦 | SIMPLE 고레벨 풀 유지 주의. 최대 레벨 제한 또는 그룹 상향 검토 |

### 1차 적용 후보

실제 설정 변경은 아직 하지 않는다. 적용한다면 아래 순서가 안전하다.

1. PVP 안전 패치:
   - `explosive`, `reflect`, `greatsword`는 우선 획득 제한 또는 비활성 후보.
   - `forcefield`는 `SIMPLE` 풀에서 제외하거나 확률을 낮춘다.
   - `blind`, `paralyze`, `shockwave`는 쿨다운을 늘리고 고레벨 확률을 낮춘다.
2. 경제 안전 패치:
   - `veinminer` 3레벨 상시 발동 제거, 고대 잔해 제외 검토.
   - `harvest` 9레벨 100%와 무쿨다운 구조 조정.
   - `smelting`을 초반 `SIMPLE` 핵심 루프에서 빼거나 100% 레벨을 제한.
   - `experience`는 EXP 인플레이션 때문에 고레벨 획득 제한 검토.
3. 월드/콘텐츠 분리:
   - 던전용 광역 피해와 국가전/PVP용 광역 피해를 같은 설정으로 운영하지 않는다.
   - AdvancedEnchantments 조건만으로 월드/PVP 분리가 충분한지 먼저 확인한다.
   - 조건식으로 부족하면 별도 Paper 플러그인 훅이나 이벤트 차단 레이어를 설계한다.

### 다음 결정 지점

- 지금 바로 라이브 설정을 건드릴지, 아니면 플레이어 검증 후 적용할지 결정해야 한다.
- 최소 변경으로 시작한다면 PVP 안전 패치만 먼저 적용하고 survival 재시작/최근 로그를
  확인한다.
- 경제 패치는 `LeeSeolJobs` 실제 플레이어 보상 검증 뒤 적용하는 편이 안전하다.
