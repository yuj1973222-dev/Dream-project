# Survival Nation War System Design

Date: 2026-06-14
Target server: survival
Status: approved direction, not yet implemented

## Product Direction

The expedition server is the lobby and network hub. The current development target is
only the survival server.

The survival server is a semi-vanilla nation-war server. Its purpose is not to replace
Minecraft with unrelated content, but to strengthen existing Minecraft activities:
exploration, gathering, building, trading, fighting, and defending. Players should form
nations, compete over territory and resources, and create a resource cycle through
war, repair, preparation, and diplomacy.

War must be a playable system, not an uncontrolled conflict tool. The design must:

- create real resource loss and economic sinks;
- prevent strong nations from farming weak nations;
- avoid forcing casual members into constant frustration;
- prevent spawn teleport abuse during combat;
- avoid server-heavy world scanning;
- preserve enough building freedom for player creativity;
- prevent invulnerable fortress patterns around war objectives.

## Implementation Strategy

Development will be modular. The full design is documented now, but implementation
will be split into phases.

Phase 1 is the beta war core. It proves that wars can be declared, objectives can be
captured or raided, and a winner can be decided.

Phase 2 adds economy, surrender, and karma pressure. It proves that war creates
resource circulation without becoming money generation or weak-nation farming.

Phase 3 adds mercenaries, allied intervention, and illegal intervention handling. It
proves that third-party influence is either official and accountable or unofficial and
costly.

## War Model

Wars are declared wars, not always-on raiding. A war starts only after cost, protection,
cooldown, and target conditions are met.

The victory model combines war points and objective control.

War tiers:

- Border War: low-risk war around border objectives.
- Invasion War: medium-risk war that can affect exposed border territory and supply
  depots.
- Total War: high-risk war that can pressure capital-level objectives after strict
  conditions are met.

The system should make war possible, but not effortless. Stronger war tiers require
higher cost, stronger cooldowns, and greater karma risk.

## War Declaration Conditions

The chosen model is attacker cost plus defender protection.

The attacker must pay a war cost and, where configured, a war deposit. The defender is
protected by rules such as new-nation protection, recent-war cooldown, online activity
conditions, and power-difference checks.

Weak nations should not be repeatedly attacked for surrender money. Strong nations
that repeatedly target weak nations receive karma pressure and increasing war costs.

## War Tiers And Regions

War regions are tier-based.

Border War:

- region: border outpost and surrounding chunks;
- main objective: capture the border outpost;
- damage scope: objective area and allowed siege blocks;
- expected resource impact: low.

Invasion War:

- region: selected exposed border/frontline territory and supply depot area;
- main objectives: border outpost capture and supply depot raid;
- damage scope: limited strategic area;
- expected resource impact: medium.

Total War:

- region: capital pressure area and required strategic objectives;
- main objective: capital core pressure after prior objectives are satisfied;
- damage scope: high but still rule-bound;
- expected resource impact: high.

Attack targets are not freely chosen by the attacker. The defender's registered
strategic structure layout defines valid candidates, and the system restricts the
attacker to eligible candidates. This prevents arbitrary capital sniping while still
forcing large nations to expose more objectives as they grow.

## Strategic Structures

Beta uses three strategic structures:

- Capital Core
- Border Outpost
- Supply Depot

Small nations need fewer required structures. As a nation grows, required structures
increase. This gives larger nations more benefits and more exposed war targets.

Future release structures such as war flags, defense towers, trade posts, gates, or
logistics bases are out of beta implementation scope, but the data model must allow
new structure types without replacing the war core.

## Structure Detection

The plugin must not detect entire buildings. It detects registered anchors.

Each strategic structure has a stored anchor coordinate:

```text
structure_id
structure_type
anchor_type
anchor_item_id
world
x
y
z
owner_nation
status
core_radius
siege_radius_chunks
```

Beta anchor mode:

- internal logic supports anchor types;
- beta anchors use vanilla blocks with plugin metadata or saved coordinates;
- examples include beacon, lodestone, or bell.

Release anchor mode:

- custom item anchors replace the visible object;
- examples include war flag, nation core, supply crate, and capital core;
- the internal structure and war logic remain the same.

Most checks are event-driven:

- block place near an anchor;
- block break near an anchor;
- explosion near an anchor;
- liquid flow into an anchor core radius;
- piston movement into an anchor core radius;
- player presence in a capture radius;
- supply chest interaction in a supply depot radius.

Periodic checks should be light and limited to registered structures, such as validating
anchor exposure and unstable-state conditions.

## Structure Capture And Raid Rules

Structure resolution is mixed by structure type.

Border Outpost:

- capture-gauge objective;
- attackers increase capture progress by controlling the radius;
- defenders contest or reverse progress by presence and combat control;
- capture grants war points and can unlock higher-tier pressure.

Supply Depot:

- raid and destruction objective;
- attackers can raid configured supply containers or damage the depot anchor;
- part of the value transfers as reward;
- part of the value is burned as an economic sink;
- raid results produce war points.

Capital Core:

- total-war-only special objective;
- not a simple instant-break win condition;
- pressure becomes available only after required objectives are satisfied;
- losing capital pressure should carry high national cost but should not delete the
  nation by default in beta.

## Siege And Anti-Fortress Rules

Defensive building is allowed. Invulnerable objective boxing is not.

Each structure has two enforcement zones:

- Core Radius: strict rules immediately around the anchor.
- Siege Radius: wider combat and fortification area.

Core Radius rules:

- the anchor must not be fully sealed;
- required exposure/access rules are validated;
- obsidian-style hard blocks, liquids, excessive piston/redstone movement, and other
  blocking patterns can be denied or marked invalid;
- invalid structures become unstable and cannot provide full war benefits.

Siege Radius rules:

- walls, traps, and defensive terrain are allowed;
- siege tools can damage configured hard blocks during active wars;
- block damage is tied to war state and valid participants.

Beta siege tool:

- one siege hammer item;
- implemented as a tagged vanilla item;
- works only during active war and in valid war regions;
- consumes durability or charges;
- allows efficient destruction of configured hard blocks.

Release siege tools:

- custom siege hammer;
- custom siege charge;
- hammer handles single-block pressure;
- charge handles limited-area pressure with strict balance rules.

## Repair And Restoration

War damage is not fully rolled back.

After a war, core strategic anchors and required minimum structure state can be
restored automatically so the next war remains possible. Surrounding walls, traps,
and consumed resources remain the nation's repair responsibility.

This preserves resource circulation while preventing a single war from permanently
breaking future gameplay.

## Economy And Surrender

Surrender must not create money. Rewards come only from existing money pools:

- attacker war deposit;
- defender nation treasury within configured limits;
- supply depot value;
- configured war stakes.

Money distribution uses a mixed model:

- Border War: mostly deposit-based.
- Invasion War: deposit plus limited defender treasury compensation.
- Total War: deposit plus treasury compensation plus high-value strategic loss.

Part of the value is always burned instead of paid to the attacker. This makes war an
economic sink rather than a money faucet.

Surrender is a damage-limiting action for the losing side, not a farming button for the
winning side.

## Karma

Karma is attached to both nation and nation leader.

Nation karma records the nation's war behavior. Leader karma prevents disbanding and
recreating nations to erase abusive history.

Karma pressure applies mainly to war access and war economics:

- increased declaration cost;
- increased cooldown;
- reduced access to high-tier wars;
- weak-nation attack restrictions;
- worse mercenary or ally conditions;
- public reputation indicators.

Severe low-karma states can also affect diplomacy, neutral-zone benefits, or selected
trade privileges. These effects must be limited so players are discouraged from abuse
without being blocked from normal play.

## Spawn And Escape Control

Normal spawn convenience remains available outside combat and war pressure.

During combat or war pressure:

- combat-tagged players cannot use spawn escape;
- war regions and enemy objective zones deny instant teleport;
- configured teleport casting can be interrupted by damage, movement, or interaction;
- death and respawn during war follow war-specific rules instead of guaranteed safe
  escape.

This prevents using `/spawn` or `/survivalspawn` to invalidate risk.

## Mercenaries And Allied Intervention

Official mercenary contracts are allowed.

Rules:

- mercenaries must be registered through the system;
- mercenaries receive war tags and war responsibility;
- mercenary deaths, rewards, karma effects, and cooldowns apply normally;
- mercenary limits are adjusted by power difference;
- stronger nations get worse mercenary access or higher costs;
- hiring cost includes a burned portion as an economic sink.

Allied nation intervention is allowed as an official war role. Allied participants share
war consequences such as deaths, rewards, cooldowns, surrender impact, and karma risk.

The official participant list locks after the preparation period. Minecraft cannot
physically prevent a third party from walking nearby, but the system can prevent late
participants from gaining legal war authority.

## Illegal Intervention

The design treats unofficial third-party influence as illegal intervention when it has
repeated war impact.

Examples:

- attacking war participants;
- healing or buffing war participants;
- supplying items during active war pressure;
- repairing or modifying war structures;
- interfering with siege or capture areas.

One-off accidental actions can be recorded without strong punishment. Repeated actions
cause automatic karma loss and can escalate into a diplomatic incident.

Escalation:

- personal intervention score;
- personal karma loss;
- source nation karma loss if the player belongs to a nation;
- display as an intervening nation after thresholds;
- defender gains a retaliation or war justification against the intervening nation.

## Data And Storage Boundaries

The war system should live primarily in `LeeSeolTown` because it already owns parties,
nations, claims, neutral zones, nation upkeep, and nation-related scoreboard context.

The implementation should keep clear internal boundaries:

- WarDeclarationService: declarations, costs, cooldowns, defender protection.
- WarSessionService: active war lifecycle, state, timers, participants.
- WarStructureService: registered structures and anchor validation.
- WarRegionService: tier-based war area checks.
- WarScoreService: points, captures, raids, kills, and win conditions.
- SiegeService: siege tool validation and hard-block damage rules.
- KarmaService: nation and leader karma changes.
- WarEconomyService: deposits, surrender, burns, compensation.
- InterventionService: illegal intervention detection and escalation.

Phase 1 does not need every service fully implemented, but the file boundaries should
avoid putting the entire system into one large manager.

## Beta Scope

Phase 1 beta war core includes:

- war tier model;
- war declaration skeleton with basic costs and cooldowns;
- structure registration for capital core, border outpost, and supply depot;
- vanilla anchor support with future custom anchor fields;
- border outpost capture gauge;
- supply depot raid/damage result;
- war points and basic win resolution;
- core-radius anti-fortress checks;
- siege hammer;
- spawn escape restrictions in combat and war regions;
- minimal admin commands for inspecting and resetting war state.

Phase 1 excludes:

- full karma penalty matrix;
- full surrender economy;
- full mercenary marketplace;
- allied nation intervention;
- automatic diplomatic incident workflow;
- custom anchor art and custom siege item art.

## Later Phases

Phase 2 adds:

- war deposits and surrender payment;
- burn ratios and treasury compensation caps;
- nation and leader karma;
- weak-nation protection;
- repeated-abuse penalties;
- public reputation display.

Phase 3 adds:

- official mercenary contracts;
- power-difference mercenary limits;
- allied nation intervention;
- illegal intervention scoring;
- diplomatic incident escalation;
- release-ready custom anchors and custom siege items.

## Success Criteria

The beta succeeds if:

- a nation can register required structures;
- a war can be declared only against valid targets;
- participants can fight over a border outpost;
- capture progress and supply depot damage affect war points;
- a winner can be resolved without manual judgment;
- core anchor boxing is blocked or marked unstable;
- siege hammer creates a resource-cost path through defenses;
- spawn teleport cannot be used to escape active combat;
- the system avoids broad world scans and only checks registered structures and
  relevant events.

The beta should not attempt to prove final balance. Balance should be tuned after real
player data exists.
