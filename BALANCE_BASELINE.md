# BALANCE_BASELINE.md

Current balance baseline for the expedition survival server. Use this as the first
reference before changing economy, ranks, crafting, combat, loot, or custom
enchantments.

## Current Priority

Balance comes before broad feature work, additional items, and design polish.

The first balance target is survival progression:

1. How fast a player earns money.
2. How much money/risk is required to rank up.
3. Which systems remove money or valuable items from the economy.
4. Which PvP and enchantment effects are safe before larger PvP content opens.

## Current Numeric Baseline

| Area | Current value | Balance note |
| --- | --- | --- |
| Starting money | `10000` won | Same as C-rank money requirement. This makes early money requirements weak unless kills/playtime are the real gates. |
| Jobs daily cap | Mining `50000`, farming `30000`, fishing `30000` | A player can earn up to `110000` won/day before rank multipliers if all loops are used. |
| Jobs rank multipliers | PLAYER `1.0`, D `1.05`, C `1.10`, B `1.20`, A `1.35`, S `1.50` | Higher ranks increase income, so late rank money targets need to account for faster earning. |
| Rank-up money | D `0`, C `10000`, B `50000`, A `150000`, S `500000` | Money is a holding requirement, not consumed. This does not act as a money sink. |
| Rank-up kills | D `10`, C `20`, B `30`, A `50`, S `100` | PvP/PvE kill source needs clear rules before using kills as a serious progression gate. |
| Rank-up playtime | D `0`, C `120m`, B `300m`, A `600m`, S `1200m` | Playtime is currently the clearest anti-rush gate. |
| Tutorial reward | `1000` won | Reasonable as an onboarding helper; not a major economy source by itself. |
| Crafting iron pickaxe | `5000` won + materials | Large early sink compared with starting money. Good if this item is intentionally premium. |
| Raw iron processing | `100` won per raw iron | Not profitable if the resulting ingot is sold at current shop sell value. |
| Repair | Base `1000`, `15` per damage, max `100000` | Potential strong money sink. Vanilla anvil repair is not blocked. |
| PVP reward | `1` point per kill, same target cooldown `600s` | Low direct economy risk while points have no large money conversion. |

## Shop Reference Values

| Item | Buy | Sell | Note |
| --- | ---: | ---: | --- |
| Oak log x16 | `500` | `150` | Sell is 30% of buy. |
| Iron ingot x8 | `1200` | `400` | Sell is 50 won per ingot. |
| Bread x16 | `300` | `60` | Sell is low; mostly convenience food. |
| Diamond x1 | `5000` | `1800` | Diamond mining also pays Jobs money, so real diamond income is sell value plus Jobs reward. |

## Current Risk Findings

- The economy has high possible daily inflow and relatively few confirmed sinks.
- Rank-up money currently checks possession only. If rank progression should feel
  expensive, rank-up should either consume money or require stronger non-money gates.
- Starting money equals the C-rank money requirement, so the C money gate is not a
  real gate for new players.
- Jobs mining can pay both direct Jobs money and later shop sale value from gathered
  materials. This is acceptable only if the combined value is intentional.
- Crafting repair may become the main money sink, but vanilla anvil repair is still
  allowed, so players can bypass the custom repair sink in many cases.
- AdvancedEnchantments has several high-risk effects before serious PvP:
  `explosive`, `reflect`, `greatsword`, and early-access `forcefield`.
- Farming and mining automation enchantments such as `harvest`, `veinminer`,
  `smelting`, and `experience` can multiply Jobs income or weaken Crafting's role.

## First Balance Direction

Do not add major new item sources until these are decided:

1. Target daily income for early, mid, and late players.
2. Whether rank-up money should be consumed or only checked.
3. Whether Jobs daily caps should be lower before launch testing.
4. Whether vanilla anvil repair stays as a cheaper alternative or custom repair should
   become the intended repair path.
5. Which AdvancedEnchantments are restricted before PvP and nation-war content opens.

## Conservative First Tuning Candidates

These are candidates, not applied changes.

| Area | Candidate | Reason |
| --- | --- | --- |
| Ranks | Make rank-up money consumed, or raise money requirements if it remains possession-only. | Prevent money from being a one-time passive checkpoint. |
| Jobs | Measure real hourly income first; lower daily caps only if players cap too quickly. | Current caps may be fine if active play takes long enough. |
| Crafting | Keep raw iron processing at `100` unless ingot sale/buy values change. | It is not a money exploit at current shop values. |
| Repair | Decide whether to block vanilla anvil repair before relying on repair as a money sink. | Otherwise the custom repair economy can be optional. |
| AdvancedEnchantments | Restrict `explosive`, `reflect`, `greatsword`; move or nerf `forcefield`. | These are high-risk before PvP rules and protection behavior are proven. |
| Automation enchants | Limit `veinminer`, `harvest`, `smelting`, and `experience` after Jobs/Crafting tests. | They can multiply income or bypass processing value. |

## Measurement Checklist

Use online player tests to collect balance data, not just plugin verification.

- Mine for 10 minutes as PLAYER rank and record Jobs money plus sellable item value.
- Farm for 10 minutes with normal manual harvesting and record Jobs money plus crops.
- Fish for 10 minutes and record default/treasure reward frequency.
- Repair one heavily damaged iron, diamond, and netherite tool and compare custom
  repair cost to vanilla anvil behavior.
- Check `/rank progress` after 1 hour of ordinary play.
- Test whether AdvancedEnchantments automation effects trigger Jobs rewards more than
  expected.

