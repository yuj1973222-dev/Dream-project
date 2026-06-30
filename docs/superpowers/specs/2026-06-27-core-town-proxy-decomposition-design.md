# Core Plugin Decomposition Design

Date: 2026-06-27

## Purpose

LeeSeolTown, LeeSeolCore, and LeeSeolProxy own the highest-risk shared
contracts in the LeeSeol Network. This design keeps the existing plugin jars in
place first, splits internal responsibilities around stable contracts, and only
promotes stable areas to separate jars later.

The goal is to reduce regressions where adding one feature removes, hides, or
breaks another feature. The design must let future work stay inside one domain
unless a documented contract requires a consumer or bridge update.

## Scope

Primary plugins:

- LeeSeolTown
- LeeSeolCore
- LeeSeolProxy

Current strategy:

- Keep the current deploy targets and plugin names.
- Keep current commands, placeholders, plugin-message channels, shared data
  keys, and external bridge config stable.
- Split internal package/service responsibilities before splitting jars.
- Treat PLUGIN_INDEX.md as the current runtime contract map.

Out of scope:

- Immediate jar splitting.
- Remote deploys, service restarts, or live server changes.
- Replacing existing commands or player-facing contracts.
- Moving unrelated plugins into this redesign.

## Design Approach

Use contract-first internal decomposition:

```text
Command or Listener
-> Application Service
-> Domain Policy
-> Repository or Bridge Port
-> Contract Event or Display Update
```

Each internal area should answer three questions without reading its internals:

- What behavior does it own?
- Which contracts does it expose or consume?
- Which smoke test proves the contract still works?

Future jar splitting is allowed only after the internal area has stable tests
and a documented contract surface.

## Part 1: LeeSeolTown

LeeSeolTown remains the survival-side owner for party, town, nation, claim, war,
neutral-zone, and structure behavior. It is the highest-risk Paper contract owner
because it is consumed by placeholders, chat, scoreboard, BlueMap, WorldGuard,
Combat, Vault, and ItemsAdder.

Recommended internal areas:

| Area | Responsibility | Future jar split |
| --- | --- | --- |
| identity | Party, town, nation identity and chat mode basics | Low |
| nation | Nation creation, color, members, treasury, tax, upkeep, debt | Low |
| claim | Chunk claims, protection, claim price, territory transitions | Low |
| war | Invasion and total war state | Medium |
| neutralzone | Core content-based neutral-zone consumption and protection | Medium |
| structure | Nation core, outpost, supply depot, schem paste, undo | High |
| presentation | PlaceholderAPI, scoreboard, chat prefix, actionbar, BlueMap | High |
| storage | towns, nations, wars, claim, upkeep, debt data keys | Low |

Stable contracts:

- Commands: /party, /town, /village, /towny, /tc, /pc, /nc
- PlaceholderAPI: %leeseoltown_*%
- Shared data keys: towns, nations, wars, beacon-claim, upkeep.*, debt.*,
  functions-suspended
- Bridges: Vault, LeeSeolCore contents.yml, WorldGuard, BlueMap, ItemsAdder,
  WorldEdit
- Consumers: LeeSeolCombat same-affiliation checks, TAB/PAPI displays, chat,
  scoreboard, territory actionbar, map markers

Target flow for claim behavior:

```text
/party nation claim
-> ClaimApplicationService
-> ClaimPricePolicy + NationPermissionPolicy
-> VaultEconomyPort + TownRepository
-> TerritoryTransition
-> BlueMap, scoreboard, placeholder, and territory display refresh
```

The current TownService should stop being the place where unrelated Town rules
accumulate. New behavior should land in an application service and policy for
its domain, with TownService reduced toward orchestration and compatibility.

## Part 2: LeeSeolCore

LeeSeolCore is not a general shared library for every domain. It owns common
Paper server surfaces and the content-location contract used by other plugins.
Because it deploys to both survival and lobby, domain-specific behavior should
not be added to Core.

Recommended internal areas:

| Area | Responsibility | Future jar split |
| --- | --- | --- |
| bootstrap | Reload, command/listener registration, lifecycle | Low |
| serverinfo | /serverinfo and uptime/basic server information | Low |
| networkmove | Paper-side server movement request boundary | Medium |
| spawn | Survival spawn, return, respawn, void recovery | Medium |
| portal | Portal triggers, WorldEdit selection, teleport/server actions | Medium |
| launchpad | Local launch pad movement interaction | High |
| servermenu | Shift+F server menu and Paper-side movement UI | High |
| servernpc | Citizens server movement NPCs | High |
| content | /content, contents.yml, WorldGuard/BlueMap content region | Low |

Stable contracts:

- Commands: /serverinfo, /survivalspawn, /lscore, /leeseolcore, /content
- Shared surface: contents.yml
- Bridges: WorldGuard, WorldEdit, BlueMap, Citizens, BungeeCord plugin channel
- Consumers: LeeSeolTown neutral-zone source, WorldGuard/BlueMap content display,
  Paper-side movement menus and NPCs

Core owns location registration, not downstream domain rules. For example, Core
owns the content registry. Town decides whether a content entry should act as a
neutral zone.

Target flow for content behavior:

```text
/content command
-> ContentApplicationService
-> ContentRegistry
-> ContentRegionBridge(WorldGuard)
-> ContentMarkerBridge(BlueMap)
-> ContentChangedEvent or documented reload hook
```

Core and Proxy boundary:

```text
LeeSeolCore = Paper-side interaction surface
LeeSeolProxy = network routing policy owner
```

Core may request movement. Core must not decide queue, maintenance, fallback, or
backend availability policy.

Target flow for movement requests:

```text
Paper UI/NPC/Portal/Command
-> Core NetworkMovePort
-> BungeeCord-compatible Connect request or documented route request
-> Proxy routing policy
```

Core must not own:

- Queue order or capacity decisions
- Maintenance or fallback decisions
- Velocity backend availability policy
- Resource-pack offers
- network.properties, queue.properties, or resourcepack.properties

## Part 3: LeeSeolProxy

LeeSeolProxy is the Velocity-side network policy owner. It decides how players
move between backend servers and owns network-wide resource-pack offers. It must
not read Paper plugin internals or implement Paper-side UI behavior.

Recommended internal areas:

| Area | Responsibility | Future jar split |
| --- | --- | --- |
| bootstrap | Velocity lifecycle, command/channel/service registration | Low |
| network | Backend names, maintenance, fallback, network.properties | Low |
| route | /lobby, /survival, /servers command routing | Medium |
| queue | Survival queue order, enter, leave, queue.properties | Medium |
| limbo | LeeSeolLobby leeseol:queue plugin-message contract | Medium |
| resourcepack | Velocity pack offer, URL, hash, force, prompt | Medium |
| config | Properties loading, defaults, validation | High |
| contract | Queue message encoding, backend constants, smoke fixtures | High |

Stable contracts:

- Commands: /servers, /lobby, /survival
- Plugin channel: leeseol:queue
- Config files: network.properties, queue.properties, resourcepack.properties
- Bridges: Velocity backend registry, LeeSeolLobby limbo handling, resourcepack
  service, ItemsAdder-generated pack artifact
- Consumers: LeeSeolLobby queue/limbo, Paper movement commands/menus through
  network movement requests

Proxy owns:

- Server routing policy
- Queue and limbo policy
- Fallback and maintenance decisions
- Velocity resource-pack offer
- Network config files

Proxy must not own:

- Paper GUI, NPC, portal, or launchpad implementation
- LeeSeolCore contents.yml
- Survival spawn or return logic
- Town claim, war, or nation rules
- Lobby internal movement rules

Target flow for survival movement:

```text
/survival or Core movement request
-> RouteService
-> NetworkPolicyService
-> QueueRouteService
-> leeseol:queue message to Lobby when needed
-> connect player to survival when allowed
```

Target flow for lobby movement:

```text
/lobby
-> RouteService
-> leave queue if queued
-> connect player to lobby
```

Target flow for fallback:

```text
KickedFromServerEvent
-> NetworkPolicyService
-> maintenance or fallback decision
-> redirect to lobby or disconnect with message
```

Target flow for resource packs:

```text
PostLoginEvent
-> ResourcePackOfferService
-> resourcepack.properties
-> Velocity resource-pack offer
```

Core and Proxy shared rules:

- Core is the Paper-side movement requester.
- Proxy is the network policy decision maker.
- Core must not know queue, fallback, maintenance, or resource-pack policy.
- Proxy must not know Core UI internals.
- leeseol:queue is a Proxy-Lobby contract, not a Core contract.
- Resource-pack offers are owned only by Proxy.

## Implementation Order

1. Add or clarify internal boundaries without changing player-facing behavior.
2. Split LeeSeolCore movement access behind a NetworkMovePort-style boundary.
3. Split LeeSeolProxy internals into network, route, queue, limbo, resourcepack,
   config, and contract areas.
4. Split LeeSeolTown internals by identity, nation, claim, war, neutralzone,
   structure, presentation, and storage.
5. Update PLUGIN_INDEX.md only when current code contracts actually change.
6. Consider jar splitting only after contract smoke tests and internal tests are
   stable for the candidate area.

## Contract Smoke Gates

Run only the rows affected by the actual implementation change.

Town:

- /party me
- /party claimprice
- /party nation claimprice
- /party nation deposit
- /party nation upkeep
- /party war declare <nation> invasion
- /party diag
- PlaceholderAPI parse for %leeseoltown_*%
- scoreboard/chat/territory display smoke
- structure place and undo smoke when structure code changes

Core:

- /content list
- /content tp
- /survivalspawn
- /leeseolcore servernpc list
- Portal/menu/NPC/launchpad movement request smoke when those surfaces change

Proxy:

- Velocity plugin load
- /servers
- /survival
- /lobby
- queue/limbo enter and leave roundtrip
- kicked fallback
- resource-pack offer

Cross-plugin:

- Core movement request does not bypass Proxy routing policy.
- Proxy-Lobby leeseol:queue message remains compatible.
- Town neutral-zone behavior remains compatible with Core contents.yml.
- Combat same-affiliation reward blocking remains compatible with Town
  placeholders.

## Completion Criteria

The decomposition is complete for a plugin when:

- The main plugin class only wires services and lifecycle.
- Domain rules live in named services or policies.
- External systems are reached through narrow bridge classes.
- The plugin row and integration rows in PLUGIN_INDEX.md still match the code.
- The matching contract smoke gates pass.
- No new runtime contract exists without a documented owner and smoke test.
