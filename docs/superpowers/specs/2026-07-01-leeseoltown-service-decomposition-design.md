# LeeSeolTown Service Decomposition Design

Date: 2026-07-01

## Purpose

LeeSeolTown currently owns party, nation, claim, war, neutral-zone, structure,
chat, scoreboard, PlaceholderAPI, Vault, WorldGuard, BlueMap, ItemsAdder, and
WorldEdit behavior in one survival-side plugin. The current deploy target stays
as one jar, but the internal service boundary needs to be split so that adding a
feature in one domain does not hide, remove, or break another domain.

This design fixes the next implementation direction for LeeSeolTown. It does
not change player-facing commands, placeholders, data keys, permissions, or
server deploy targets.

## Confirmed Approach

Use domain service decomposition inside the existing `LeeSeolTown` plugin.

`TownService` remains as the compatibility facade for commands, listeners,
hooks, and other plugin consumers during the first refactor. New focused
services take ownership of behavior behind that facade.

Target flow:

```text
Command / Listener / Hook
-> TownService compatibility facade
-> Focused domain service
-> TownStore / bridge / policy
-> Existing display, marker, placeholder, or data contract
```

## Scope

Primary plugin:

- `LeeSeolTown`

Allowed internal consumers:

- `command/TownCommand.java`
- `listener/*`
- `hook/TownPlaceholderExpansion.java`
- `structure/*` only where nation-core registration or undo calls cross the
  service boundary

Bridge and config surfaces:

- Vault through `VaultEconomyHook`
- PlaceholderAPI through `%leeseoltown_*%`
- LeeSeolCore content neutral zones through `NeutralZoneContentSource`
- WorldGuard and BlueMap neutral/claim markers
- ItemsAdder and WorldEdit structure placement
- `config.yml` keys already used by Town

Out of scope:

- Splitting LeeSeolTown into multiple jars.
- Renaming commands, permissions, placeholders, or config keys.
- Changing data file shape for `towns`, `nations`, `wars`, claims, upkeep,
  debt, or function suspension.
- Remote deploy, server restart, or live data migration.
- Reworking unrelated plugins.

## Current Problem

`TownService` is doing too much. It currently mixes:

- party lifecycle and membership
- nation creation, color, treasury, upkeep, debt, and suspension
- claim purchase, unclaim, adjacency, neutral-zone checks, and claim pricing
- war declaration, acceptance, surrender, protection, debt, and cleanup
- chat formatting and affiliation/rank prefixes
- PlaceholderAPI-facing display values
- build and PVP rule checks used by listeners
- nation core registration and undo side effects
- broadcast, confirmation, and remote invite helpers

This makes future work risky because a change to war or structures can easily
touch claim, display, economy, and placeholder behavior in the same file.

## Target Services

### `TownMembershipService`

Owns party identity and membership:

- create party
- invite, join, accept, deny
- leave, disband, transfer leader, kick member
- membership lookup helpers for player and party
- confirmation state for party disband and transfer

It uses `TownStore` as the source of truth and calls display refresh through
the facade or a small display port.

### `NationService`

Owns nation identity and economy state:

- create and disband nation
- nation color keys and color availability
- PVP/build-protection toggles
- treasury deposit and treasury display
- upkeep display, payment, collection, grace period, and function suspension
- nation tax/upkeep calculations
- member count and nation party membership helpers

It keeps existing Vault and config behavior stable. It must not change stored
keys such as `treasury`, `upkeep-debt`, `last-upkeep-period`, `debt-*`, or
`functions-suspended`.

### `ClaimService`

Owns territory claims and territory ownership rules:

- claim chunk
- claim price display
- unclaim chunk
- adjacency rules
- neutral-zone claim blocking
- claim cost and zone multiplier use
- `claimTown`, `nationForClaim`, and `nationIdForClaim`
- build permission checks for claimed chunks

It preserves the `/party claim`, `/party claimprice`, `/party unclaim`, and
`/party nation claim*` behavior. It also remains the boundary used by claim
protection listeners.

### `WarService`

Owns war lifecycle:

- declare war with `invasion` or `total` mode
- accept, surrender, release protection, pay debt, finish war
- pending and active war lookup
- defender protection expiration
- war debt expiration and function suspension
- PVP and entry rules that depend on war state

It preserves `War`, `WarMode`, and `WarStatus` semantics and keeps existing
messages and debt behavior stable.

### `TownDisplayService`

Owns player-facing display strings and display refresh helpers:

- chat line formatting
- affiliation prefix
- rank prefix
- self info output
- placeholder-facing display values
- identity refresh and all-player identity refresh

The PlaceholderAPI expansion should eventually depend on this service instead
of reaching through `TownService` for formatting details. Placeholder names and
return values remain stable.

### `TownService`

Remains as a facade during the refactor:

- exposes the same public methods used by commands, listeners, hooks, and
  structures
- delegates domain behavior to the focused services
- keeps compatibility while implementation moves behind the facade
- shrinks over time until it is mainly orchestration and read-only facade

This keeps the blast radius small. Callers can be moved later after the domain
services have tests.

## Stable Contracts

Commands:

- `/town`, `/party`, `/village`, `/towny`
- `/tc`, `/pc`
- `/nc`

Permissions:

- `leeseoltown.use`
- `leeseoltown.claim`
- `leeseoltown.chat`
- `leeseoltown.admin`
- `leeseoltown.neutral.bypass`
- `leeseoltown.structure.*`

PlaceholderAPI:

- `%leeseoltown_affiliation%`
- `%leeseoltown_rank%`
- `%leeseoltown_has_party%`
- `%leeseoltown_has_town%`
- `%leeseoltown_town%`
- `%leeseoltown_party%`
- `%leeseoltown_nation%`
- `%leeseoltown_nation_color%`
- `%leeseoltown_nation_color_hex%`
- `%leeseoltown_nation_type%`

Shared data:

- `towns`
- `nations`
- `wars`
- town member and claim indexes rebuilt by `TownStore`
- `beacon-claim`
- `treasury`
- `upkeep-debt`
- `last-upkeep-period`
- `debt-creditor-nation-id`
- `debt-amount`
- `debt-deadline`
- `functions-suspended`

External bridges:

- Vault
- PlaceholderAPI
- LeeSeolCore content neutral zones
- WorldGuard
- BlueMap
- ItemsAdder
- WorldEdit

Consumers:

- `TownCommand`
- `NationClaimCommand`
- `ChannelChatCommand`
- `ClaimProtectionListener`
- `NationRuleListener`
- `NeutralZoneListener`
- `StructurePlacementListener`
- `TownChatListener`
- `TownPlaceholderExpansion`
- `StructureUndoService`
- `LeeSeolCombat` through same-affiliation and nation PVP expectations
- TAB and other display plugins through PlaceholderAPI

## Data Flow

Claim purchase:

```text
/party nation claim
-> TownService.claimChunk
-> ClaimService.claimChunk
-> NationService.ensureNationActive
-> VaultEconomyHook withdraw
-> TownStore claim update
-> BlueMapNationClaimMarkers refresh
-> TownDisplayService identity/zone refresh where needed
```

War declaration:

```text
/party war declare <nation> <mode>
-> TownService.declareWar
-> WarService.declareWar
-> NationService active/manage checks
-> TownStore war update
-> nation broadcasts
```

Placeholder request:

```text
PlaceholderAPI %leeseoltown_nation%
-> TownPlaceholderExpansion
-> TownService facade
-> TownDisplayService / read-only membership and nation lookup
```

Structure nation core placement:

```text
StructurePlacementListener
-> TownService.canPlaceNationCoreStructure
-> ClaimService and NationService checks
-> WorldEditStructurePaster
-> TownService.registerNationCoreStructure
-> ClaimService records beacon claim
```

## Error Handling

- Player-facing validation must keep the current message keys.
- Domain services should return boolean success for existing command paths until
  callers are moved to richer result objects.
- No exception should escape a command or event listener for expected validation
  failures.
- WorldEdit/ItemsAdder structure failures stay isolated to structure placement
  and undo handling.
- Storage save failures remain logged through existing plugin logging behavior.

## Testing

Required before and after each implementation slice:

- `mvn -f LeeSeolTown/pom.xml test package`

Contract smoke tests to preserve:

- `TownPlaceholderContractTest`
- `NationClaimCommandTest`
- `WarModeTest`
- `NeutralZoneContentSourceTest`
- `TerritoryTransitionTest`
- structure rule and undo tests
- feature diagnostic tests

New tests should be added around the first extracted service before behavior is
moved. Each extraction should prove:

- public command-facing behavior still returns the same success/failure result
- affected data keys are unchanged
- placeholders keep the same names and values
- claim, war, or structure consumers can still call the existing facade method

## Implementation Order

1. Extract read-only lookup helpers that are used across services.
2. Extract `TownMembershipService` with tests while keeping facade methods.
3. Extract `NationService` for nation color, treasury, upkeep, and suspension.
4. Extract `ClaimService` for claim price, claim/unclaim, adjacency, and build
   checks.
5. Extract `WarService` for war lifecycle and debt expiration.
6. Extract `TownDisplayService` for chat, prefix, self info, and placeholders.
7. Update `PLUGIN_INDEX.md` only if package responsibilities or verification
   steps change.

Each step should compile and pass the Town test suite before the next step.

## Acceptance Criteria

- LeeSeolTown still builds as one Paper plugin jar.
- Existing commands, aliases, permissions, placeholders, data keys, and bridge
  config remain compatible.
- `TownService` remains callable by existing commands, listeners, hooks, and
  structure services.
- The first implementation plan contains small TDD tasks and commits per
  domain slice.
- No live server, deploy, restart, or data migration is required for the
  refactor itself.
