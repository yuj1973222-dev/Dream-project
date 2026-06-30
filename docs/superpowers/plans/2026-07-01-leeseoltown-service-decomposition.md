# LeeSeolTown Service Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split `LeeSeolTown` internal responsibilities into focused services while keeping existing commands, placeholders, listeners, storage keys, and deploy target stable.

**Architecture:** Keep `TownService` as the compatibility facade used by current commands, listeners, hooks, and structure services. Add focused services behind it, then move behavior one domain at a time with source-level contract tests and the existing Maven test suite after every slice.

**Tech Stack:** Java 21, Paper API 26.1.2, Bukkit/Paper plugin lifecycle, PlaceholderAPI, WorldEdit, JUnit 4, Maven.

---

## Scope

Primary plugin:

- `LeeSeolTown`

Allowed files:

- `LeeSeolTown/src/main/java/me/leeseol/town/service/*`
- `LeeSeolTown/src/main/java/me/leeseol/town/LeeSeolTownPlugin.java`
- `LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java` only if a compile-time constructor or accessor changes
- `LeeSeolTown/src/main/java/me/leeseol/town/listener/*` only if facade access remains compatible
- `LeeSeolTown/src/main/java/me/leeseol/town/hook/TownPlaceholderExpansion.java` only when display service becomes the placeholder source
- `LeeSeolTown/src/main/java/me/leeseol/town/structure/*` only where nation-core facade methods are affected
- `LeeSeolTown/src/test/java/me/leeseol/town/service/*`
- `LeeSeolTown/src/test/java/me/leeseol/town/hook/*`
- `PLUGIN_INDEX.md` only if package responsibilities or verification notes change

Do not:

- Rename commands, aliases, permissions, placeholders, data keys, or config keys.
- Split `LeeSeolTown` into multiple jars.
- Deploy, restart, or touch live server files.
- Rewrite unrelated plugins.

## File Structure

Create these focused service files:

- `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDomainQuery.java`
  Read-only lookup and relationship helpers over `TownStore`.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/TownConfirmationService.java`
  Shared short-lived confirmation state for destructive player actions.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDisplayService.java`
  Chat lines, prefixes, self info, identity refresh, and placeholder-facing display values.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/TownMembershipService.java`
  Party create, invite, accept, deny, leave, disband, transfer, and kick behavior.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/NationService.java`
  Nation creation, color, treasury, upkeep, debt, active-state checks, and member counts.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/ClaimService.java`
  Claim, claim price, unclaim, adjacency, build permissions, and nation-core claim registration.

- `LeeSeolTown/src/main/java/me/leeseol/town/service/WarService.java`
  War declaration, acceptance, surrender, protection, debt payment, expiry, and PVP/entry checks.

Keep this file as a facade:

- `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
  Owns service wiring and delegates existing public methods to focused services.

Add source contract tests:

- `LeeSeolTown/src/test/java/me/leeseol/town/service/TownServiceBoundaryContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/TownDomainQueryContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/TownMembershipServiceContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/NationServiceContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/ClaimServiceContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/WarServiceContractTest.java`
- `LeeSeolTown/src/test/java/me/leeseol/town/service/TownDisplayServiceContractTest.java`

These tests intentionally use source inspection for Bukkit-heavy methods because the current test suite only has JUnit 4 and no Bukkit mock framework. They protect the refactor boundary without adding a new test dependency.

## Preflight

- [ ] **Step 1: Verify branch and working tree**

Run:

```bash
git status -sb
```

Expected:

```text
## <feature-branch>
```

`git status --short` must be empty before code edits.

- [ ] **Step 2: Run baseline Town build**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

If Codex sandbox reports `AccessDeniedException` for `C:\Users\user\Tools\jdk-25\conf\security\java.security`, rerun the same Maven command with external execution approval.

---

### Task 1: Add Service Boundary Skeleton

**Files:**

- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/TownServiceBoundaryContractTest.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDomainQuery.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownConfirmationService.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDisplayService.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownMembershipService.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/NationService.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/ClaimService.java`
- Create: `LeeSeolTown/src/main/java/me/leeseol/town/service/WarService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`

- [ ] **Step 1: Write failing boundary test**

Create `TownServiceBoundaryContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownServiceBoundaryContractTest {
    @Test
    public void keepsTownServiceAsFacadeForFocusedServices() throws IOException {
        String townService = source("TownService.java");

        for (String field : List.of(
                "private final TownDomainQuery domainQuery;",
                "private final TownConfirmationService confirmationService;",
                "private final TownDisplayService displayService;",
                "private final TownMembershipService membershipService;",
                "private final NationService nationService;",
                "private final ClaimService claimService;",
                "private final WarService warService;"
        )) {
            assertTrue("Missing facade field: " + field, townService.contains(field));
        }
    }

    @Test
    public void focusedServiceClassesExist() {
        for (String fileName : List.of(
                "TownDomainQuery.java",
                "TownConfirmationService.java",
                "TownDisplayService.java",
                "TownMembershipService.java",
                "NationService.java",
                "ClaimService.java",
                "WarService.java"
        )) {
            assertTrue("Missing service file: " + fileName,
                    Files.exists(servicePath(fileName)));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(servicePath(fileName));
    }

    private static Path servicePath(String fileName) {
        return Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName);
    }
}
```

- [ ] **Step 2: Run boundary test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownServiceBoundaryContractTest test
```

Expected:

```text
Missing facade field: private final TownDomainQuery domainQuery;
```

- [ ] **Step 3: Add service skeletons**

Create `TownDomainQuery.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.model.War;
import me.leeseol.town.model.WarStatus;
import me.leeseol.town.storage.TownStore;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class TownDomainQuery {
    private final TownStore store;

    public TownDomainQuery(TownStore store) {
        this.store = store;
    }

    public Town playerTown(Player player) {
        return player == null ? null : store.playerTown(player.getUniqueId());
    }

    public Town playerTown(UUID playerId) {
        return playerId == null ? null : store.playerTown(playerId);
    }

    public Nation playerNation(Player player) {
        Town town = playerTown(player);
        return town == null || town.nationId() == null ? null : store.nation(town.nationId());
    }

    public Town claimTown(ClaimKey claim) {
        return store.claimTown(claim);
    }

    public Nation nationForClaim(ClaimKey claim) {
        Town owner = store.claimTown(claim);
        return owner == null || owner.nationId() == null ? null : store.nation(owner.nationId());
    }

    public String nationIdForClaim(ClaimKey claim) {
        Nation nation = nationForClaim(claim);
        return nation == null ? null : nation.id();
    }

    public boolean nationHasOpenWar(Nation nation) {
        if (nation == null) {
            return false;
        }
        for (War war : store.wars()) {
            if ((war.status() == WarStatus.PENDING || war.status() == WarStatus.ACTIVE)
                    && war.involves(nation.id())) {
                return true;
            }
        }
        return false;
    }

    public Nation nationByName(String name) {
        String id = TownStore.idFromName(name);
        Nation nation = store.nation(id);
        if (nation != null) {
            return nation;
        }
        for (Nation candidate : store.nations()) {
            if (candidate.name().equalsIgnoreCase(name)) {
                return candidate;
            }
        }
        return null;
    }

    public War findWarBetween(String firstNationId, String secondNationId) {
        for (War war : store.wars()) {
            if (war.between(firstNationId, secondNationId)) {
                return war;
            }
        }
        return null;
    }

    public War findPendingWar(String attackerNationId, String defenderNationId) {
        for (War war : store.wars()) {
            if (war.status() == WarStatus.PENDING
                    && war.attackerNationId().equals(attackerNationId)
                    && war.defenderNationId().equals(defenderNationId)) {
                return war;
            }
        }
        return null;
    }

    public boolean isNationMember(UUID playerId, Nation nation) {
        if (playerId == null || nation == null) {
            return false;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null && town.members().contains(playerId)) {
                return true;
            }
        }
        return false;
    }

    public Nation nationForBeaconClaim(ClaimKey claim) {
        for (Nation nation : store.nations()) {
            if (claim.equals(nation.beaconClaim())) {
                return nation;
            }
        }
        return null;
    }

    public int nationMemberCount(Nation nation) {
        int count = 0;
        if (nation == null) {
            return count;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                count += town.members().size();
            }
        }
        return count;
    }

    public int nationClaimCount(Nation nation) {
        return nationClaims(nation).size();
    }

    public List<ClaimKey> nationClaims(Nation nation) {
        List<ClaimKey> claims = new ArrayList<>();
        if (nation == null) {
            return claims;
        }
        for (String townId : nation.townIds()) {
            Town town = store.town(townId);
            if (town != null) {
                claims.addAll(town.claims());
            }
        }
        return claims;
    }

    public boolean isAdjacentToNationClaim(Nation nation, ClaimKey claim) {
        return isNationClaim(nation, new ClaimKey(claim.world(), claim.x() + 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x() - 1, claim.z()))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() + 1))
                || isNationClaim(nation, new ClaimKey(claim.world(), claim.x(), claim.z() - 1));
    }

    public boolean isNationClaim(Nation nation, ClaimKey claim) {
        Town owner = store.claimTown(claim);
        return owner != null && owner.nationId() != null && owner.nationId().equals(nation.id());
    }

    public Collection<Town> towns() {
        return store.towns();
    }

    public Collection<Nation> nations() {
        return store.nations();
    }
}
```

Create `TownConfirmationService.java`:

```java
package me.leeseol.town.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.town.LeeSeolTownPlugin;
import org.bukkit.entity.Player;

public final class TownConfirmationService {
    private static final long CONFIRM_MILLIS = 30_000L;

    private final LeeSeolTownPlugin plugin;
    private final Map<UUID, PendingConfirmation> pending = new HashMap<>();

    public TownConfirmationService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean confirm(Player player, String kind, String id, String messageKey, String command, String name) {
        long now = System.currentTimeMillis();
        PendingConfirmation existing = pending.get(player.getUniqueId());
        if (existing != null
                && existing.kind().equals(kind)
                && existing.id().equals(id)
                && existing.expiresAt() >= now) {
            pending.remove(player.getUniqueId());
            return true;
        }

        pending.put(player.getUniqueId(), new PendingConfirmation(kind, id, now + CONFIRM_MILLIS));
        player.sendMessage(plugin.msg(messageKey)
                .replace("%name%", name)
                .replace("%command%", command));
        return false;
    }

    private record PendingConfirmation(String kind, String id, long expiresAt) {
    }
}
```

Create `TownMembershipService.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class TownMembershipService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final TownConfirmationService confirmations;
    private final TownDisplayService display;

    public TownMembershipService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                                 TownConfirmationService confirmations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.confirmations = confirmations;
        this.display = display;
    }
}
```

Create `NationService.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class NationService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final TownConfirmationService confirmations;
    private final TownDisplayService display;

    public NationService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                         TownConfirmationService confirmations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.confirmations = confirmations;
        this.display = display;
    }
}
```

Create `ClaimService.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class ClaimService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final NationService nations;
    private final TownDisplayService display;

    public ClaimService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                        NationService nations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.nations = nations;
        this.display = display;
    }
}
```

Create `WarService.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class WarService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final NationService nations;
    private final TownDisplayService display;

    public WarService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                      NationService nations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.nations = nations;
        this.display = display;
    }
}
```

Create `TownDisplayService.java`:

```java
package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class TownDisplayService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;

    public TownDisplayService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
    }
}
```

- [ ] **Step 4: Wire skeleton services into `TownService`**

Modify `TownService` fields and constructor:

```java
private final TownDomainQuery domainQuery;
private final TownConfirmationService confirmationService;
private final TownDisplayService displayService;
private final TownMembershipService membershipService;
private final NationService nationService;
private final ClaimService claimService;
private final WarService warService;

public TownService(LeeSeolTownPlugin plugin, TownStore store) {
    this.plugin = plugin;
    this.store = store;
    this.domainQuery = new TownDomainQuery(store);
    this.confirmationService = new TownConfirmationService(plugin);
    this.displayService = new TownDisplayService(plugin, store, domainQuery);
    this.membershipService = new TownMembershipService(plugin, store, domainQuery, confirmationService, displayService);
    this.nationService = new NationService(plugin, store, domainQuery, confirmationService, displayService);
    this.claimService = new ClaimService(plugin, store, domainQuery, nationService, displayService);
    this.warService = new WarService(plugin, store, domainQuery, nationService, displayService);
}
```

If any service constructor differs later, update only this constructor and the matching source contract test.

- [ ] **Step 5: Run skeleton boundary test**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownServiceBoundaryContractTest test
```

Expected:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 6: Run full Town test package**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 7: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service \
        LeeSeolTown/src/test/java/me/leeseol/town/service/TownServiceBoundaryContractTest.java
git commit -m "refactor(town): add service boundary skeleton"
```

---

### Task 2: Extract Read-Only Domain Queries

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDomainQuery.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/TownDomainQueryContractTest.java`

- [ ] **Step 1: Write failing source contract test**

Create `TownDomainQueryContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownDomainQueryContractTest {
    @Test
    public void townServiceDelegatesReadOnlyLookupsToDomainQuery() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", "TownService.java"));

        for (String delegate : List.of(
                "return domainQuery.nationForClaim(claim);",
                "return domainQuery.nationIdForClaim(claim);",
                "return domainQuery.nationHasOpenWar(nation);",
                "return domainQuery.claimTown(claim);",
                "return domainQuery.playerTown(player);",
                "return domainQuery.playerNation(player);",
                "return domainQuery.towns();",
                "return domainQuery.nations();"
        )) {
            assertTrue("Missing domain query delegation: " + delegate, source.contains(delegate));
        }
    }

    @Test
    public void oldPrivateLookupMethodsAreNotLeftInTownService() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", "TownService.java"));

        for (String method : List.of(
                "private Nation nationByName",
                "private War findWarBetween",
                "private War findPendingWar",
                "private boolean isNationMember",
                "private int nationMemberCount",
                "private List<ClaimKey> nationClaims",
                "private boolean isAdjacentToNationClaim",
                "private boolean isNationClaim"
        )) {
            assertFalse("Lookup helper still lives in TownService: " + method, source.contains(method));
        }
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownDomainQueryContractTest test
```

Expected failure mentions missing `domainQuery` delegation.

- [ ] **Step 3: Move lookup helpers to `TownDomainQuery`**

Move these existing private methods from `TownService` to `TownDomainQuery`:

```text
nationByName
findWarBetween
findPendingWar
isNationMember
nationForBeaconClaim
isAdjacentToNationClaim
isNationClaim
nationClaimCount
nationClaims
nationMemberCount
```

Keep the same logic. Replace direct `store` access with the `TownDomainQuery` field `store`.

- [ ] **Step 4: Replace facade read methods in `TownService`**

Replace public read-only methods in `TownService` with this delegation pattern:

```java
public Nation nationForClaim(ClaimKey claim) {
    return domainQuery.nationForClaim(claim);
}

public String nationIdForClaim(ClaimKey claim) {
    return domainQuery.nationIdForClaim(claim);
}

public boolean nationHasOpenWar(Nation nation) {
    return domainQuery.nationHasOpenWar(nation);
}

public Town claimTown(ClaimKey claim) {
    return domainQuery.claimTown(claim);
}

public Town playerTown(Player player) {
    return domainQuery.playerTown(player);
}

public Nation playerNation(Player player) {
    return domainQuery.playerNation(player);
}

public Collection<Town> towns() {
    return domainQuery.towns();
}

public Collection<Nation> nations() {
    return domainQuery.nations();
}
```

Update remaining internal calls in `TownService`:

```text
nationByName(name) -> domainQuery.nationByName(name)
findWarBetween(firstNationId, secondNationId) -> domainQuery.findWarBetween(firstNationId, secondNationId)
findPendingWar(attackerNationId, defenderNationId) -> domainQuery.findPendingWar(attackerNationId, defenderNationId)
isNationMember(playerId, nation) -> domainQuery.isNationMember(playerId, nation)
nationForBeaconClaim(claim) -> domainQuery.nationForBeaconClaim(claim)
isAdjacentToNationClaim(nation, claim) -> domainQuery.isAdjacentToNationClaim(nation, claim)
isNationClaim(nation, claim) -> domainQuery.isNationClaim(nation, claim)
nationClaimCount(nation) -> domainQuery.nationClaimCount(nation)
nationClaims(nation) -> domainQuery.nationClaims(nation)
nationMemberCount(nation) -> domainQuery.nationMemberCount(nation)
```

- [ ] **Step 5: Run focused and full tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownDomainQueryContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/TownDomainQuery.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/TownDomainQueryContractTest.java
git commit -m "refactor(town): extract domain query helpers"
```

---

### Task 3: Extract Membership Service

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownMembershipService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/TownMembershipServiceContractTest.java`

- [ ] **Step 1: Write failing membership contract test**

Create `TownMembershipServiceContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownMembershipServiceContractTest {
    @Test
    public void membershipMethodsMoveBehindFacade() throws IOException {
        String facade = serviceSource("TownService.java");
        String service = serviceSource("TownMembershipService.java");

        for (String method : List.of(
                "public boolean createTown(Player player, String name)",
                "public boolean invite(Player sender, String targetName)",
                "public boolean joinTown(Player player, String townName)",
                "public boolean acceptInvite(Player player, String townName)",
                "public boolean denyInvite(Player player, String townName)",
                "public boolean leaveTown(Player player)",
                "public boolean disbandTown(Player player)",
                "public boolean transferLeader(Player player, String targetName)",
                "public boolean kickMember(Player player, String targetName)"
        )) {
            assertTrue("Membership service missing method: " + method, service.contains(method));
            assertTrue("Facade missing public method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "return membershipService.createTown(player, name);",
                "return membershipService.invite(sender, targetName);",
                "return membershipService.joinTown(player, townName);",
                "return membershipService.acceptInvite(player, townName);",
                "return membershipService.denyInvite(player, townName);",
                "return membershipService.leaveTown(player);",
                "return membershipService.disbandTown(player);",
                "return membershipService.transferLeader(player, targetName);",
                "return membershipService.kickMember(player, targetName);"
        )) {
            assertTrue("Facade missing membership delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void membershipImplementationNoLongerLivesInFacade() throws IOException {
        String facade = serviceSource("TownService.java");

        for (String snippet : List.of(
                "new Town(id, name, player.getUniqueId()",
                "town.invites().add",
                "town.inviteNames().add",
                "town.members().remove",
                "town.setLeader"
        )) {
            assertFalse("Membership implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    private static String serviceSource(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownMembershipServiceContractTest test
```

Expected failure mentions missing membership service methods.

- [ ] **Step 3: Move membership methods**

Move these methods from `TownService` to `TownMembershipService` with their current bodies:

```text
createTown
invite
joinTown
acceptInvite
denyInvite
leaveTown
disbandTown
transferLeader
kickMember
```

Move these helper methods too if only membership uses them after extraction:

```text
requireStrictLeaderTown
requireLeaderTown
onlineMembers(Town town)
onlineMembers(Collection<UUID> memberIds)
inviteMessage
resolveOfflineUuid
sendRemoteInvite
sendRemoteMessage
inviteJson
plainJson
json
```

Use `display.updateIdentity(player)` instead of `updateIdentity(player)`.

Use `confirmations.confirm(player, kind, id, messageKey, command, name)` instead of `confirmDisband` or `confirmAction` for membership methods:

```java
if (!confirmations.confirm(player, "town", town.id(), "disband-town-warning", "/party disband", town.name())) {
    return true;
}
```

- [ ] **Step 4: Replace `TownService` membership methods with delegates**

Use this exact facade style:

```java
public boolean createTown(Player player, String name) {
    return membershipService.createTown(player, name);
}

public boolean invite(Player sender, String targetName) {
    return membershipService.invite(sender, targetName);
}

public boolean joinTown(Player player, String townName) {
    return membershipService.joinTown(player, townName);
}

public boolean acceptInvite(Player player, String townName) {
    return membershipService.acceptInvite(player, townName);
}

public boolean denyInvite(Player player, String townName) {
    return membershipService.denyInvite(player, townName);
}

public boolean leaveTown(Player player) {
    return membershipService.leaveTown(player);
}

public boolean disbandTown(Player player) {
    return membershipService.disbandTown(player);
}

public boolean transferLeader(Player player, String targetName) {
    return membershipService.transferLeader(player, targetName);
}

public boolean kickMember(Player player, String targetName) {
    return membershipService.kickMember(player, targetName);
}
```

- [ ] **Step 5: Run focused and full tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownMembershipServiceContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/TownMembershipService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownConfirmationService.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/TownMembershipServiceContractTest.java
git commit -m "refactor(town): extract membership service"
```

---

### Task 4: Extract Nation Service

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/NationService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/NationServiceContractTest.java`

- [ ] **Step 1: Write failing nation contract test**

Create `NationServiceContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class NationServiceContractTest {
    @Test
    public void nationMethodsMoveBehindFacade() throws IOException {
        String facade = source("TownService.java");
        String service = source("NationService.java");

        for (String method : List.of(
                "public boolean createNation(Player player, String name, String colorKey, List<String> extraPartyNames)",
                "public List<String> nationColorKeys()",
                "public boolean disbandNation(Player player)",
                "public boolean setNationPvp(Player player, boolean enabled)",
                "public boolean setNationBuildProtection(Player player, boolean enabled)",
                "public boolean depositNationTreasury(Player player, double amount)",
                "public boolean sendNationTreasury(Player player)",
                "public boolean sendNationUpkeep(Player player)",
                "public boolean payNationUpkeep(Player player)",
                "public void collectDueUpkeep(boolean force)",
                "public long dailyNationUpkeep(Nation nation)",
                "public boolean ensureNationActive(Player player, Nation nation)",
                "public boolean canManageNation(Player player, Town town, Nation nation)",
                "public Nation requireNationLeader(Player player, Town town)"
        )) {
            assertTrue("Nation service missing method: " + method, service.contains(method));
            assertTrue("Facade missing method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "return nationService.createNation(player, name, colorKey, extraPartyNames);",
                "return nationService.nationColorKeys();",
                "return nationService.disbandNation(player);",
                "return nationService.setNationPvp(player, enabled);",
                "return nationService.setNationBuildProtection(player, enabled);",
                "return nationService.depositNationTreasury(player, amount);",
                "return nationService.sendNationTreasury(player);",
                "return nationService.sendNationUpkeep(player);",
                "return nationService.payNationUpkeep(player);",
                "nationService.collectDueUpkeep(force);",
                "return nationService.dailyNationUpkeep(nation);",
                "return nationService.ensureNationActive(player, nation);",
                "return nationService.canManageNation(player, town, nation);",
                "return nationService.requireNationLeader(player, town);"
        )) {
            assertTrue("Facade missing nation delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void nationStorageKeysRemainStable() throws IOException {
        String store = Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "storage", "TownStore.java"));

        for (String key : List.of(
                "upkeep.last-period",
                "upkeep.debt",
                "debt.creditor",
                "debt.amount",
                "debt.deadline",
                "functions-suspended",
                "treasury"
        )) {
            assertTrue("Missing stable nation storage key: " + key, store.contains(key));
        }
    }

    @Test
    public void nationImplementationNoLongerLivesInFacade() throws IOException {
        String facade = source("TownService.java");

        for (String snippet : List.of(
                "new Nation(id, name, color",
                "nation.setTreasury",
                "nation.setUpkeepDebt",
                "nation.setFunctionsSuspended",
                "nation.setBuildProtectionEnabled",
                "nation.setPvpEnabled"
        )) {
            assertFalse("Nation implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=NationServiceContractTest test
```

Expected failure mentions missing `NationService` methods.

- [ ] **Step 3: Move nation methods**

Move these methods from `TownService` to `NationService` with current behavior:

```text
createNation
nationColorKeys
usedNationColorKeys
disbandNation
setNationPvp
setNationBuildProtection
depositNationTreasury
sendNationTreasury
sendNationUpkeep
payNationUpkeep
requireNationLeader
ensureNationActive
canManageNation
nationTax
collectDueUpkeep
dailyNationUpkeep
inUpkeepGrace
safeAdd
```

Use `domainQuery` for these calls:

```text
domainQuery.playerTown(player)
domainQuery.nationByName(name)
domainQuery.nationMemberCount(nation)
domainQuery.nationClaimCount(nation)
domainQuery.nationClaims(nation)
domainQuery.nations()
```

Use `display.broadcastNation(nation, message)` after Task 6. Until Task 6 is complete, keep a package-private broadcast helper in `NationService` copied from current `TownService`.

Use `confirmations.confirm(player, "nation", nation.id(), "disband-nation-warning", "/party nation disband", nation.name())` for nation disband:

```java
if (!confirmations.confirm(player, "nation", nation.id(), "disband-nation-warning", "/party nation disband", nation.name())) {
    return true;
}
```

- [ ] **Step 4: Replace `TownService` nation methods with delegates**

Use this facade pattern:

```java
public boolean createNation(Player player, String name, String colorKey, List<String> extraPartyNames) {
    return nationService.createNation(player, name, colorKey, extraPartyNames);
}

public List<String> nationColorKeys() {
    return nationService.nationColorKeys();
}

public boolean disbandNation(Player player) {
    return nationService.disbandNation(player);
}

public boolean setNationPvp(Player player, boolean enabled) {
    return nationService.setNationPvp(player, enabled);
}

public boolean setNationBuildProtection(Player player, boolean enabled) {
    return nationService.setNationBuildProtection(player, enabled);
}

public boolean depositNationTreasury(Player player, double amount) {
    return nationService.depositNationTreasury(player, amount);
}

public boolean sendNationTreasury(Player player) {
    return nationService.sendNationTreasury(player);
}

public boolean sendNationUpkeep(Player player) {
    return nationService.sendNationUpkeep(player);
}

public boolean payNationUpkeep(Player player) {
    return nationService.payNationUpkeep(player);
}

public void collectDueUpkeep(boolean force) {
    nationService.collectDueUpkeep(force);
}

public long dailyNationUpkeep(Nation nation) {
    return nationService.dailyNationUpkeep(nation);
}
```

Keep `canManageNation`, `ensureNationActive`, and `requireNationLeader` public or package-private as required by `ClaimService` and `WarService`.

- [ ] **Step 5: Run focused and full tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=NationServiceContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/NationService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/NationServiceContractTest.java
git commit -m "refactor(town): extract nation service"
```

---

### Task 5: Extract Claim Service

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/ClaimService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/ClaimServiceContractTest.java`

- [ ] **Step 1: Write failing claim contract test**

Create `ClaimServiceContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class ClaimServiceContractTest {
    @Test
    public void claimMethodsMoveBehindFacade() throws IOException {
        String facade = source("TownService.java");
        String service = source("ClaimService.java");

        for (String method : List.of(
                "public boolean claimChunk(Player player)",
                "public boolean sendClaimPrice(Player player)",
                "public boolean unclaimChunk(Player player)",
                "public boolean canBuild(Player player, ClaimKey claim)",
                "public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim)",
                "public boolean canPlaceNationCoreStructure(Player player, ClaimKey claim)",
                "public boolean registerNationCoreStructure(Player player, ClaimKey claim)",
                "public void undoNationCoreStructure(String nationId, ClaimKey claim, boolean removeCreatedClaim)",
                "public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim)",
                "public boolean shouldBlockWarEntry(Player player, ClaimKey claim)"
        )) {
            assertTrue("Claim service missing method: " + method, service.contains(method));
            assertTrue("Facade missing method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "return claimService.claimChunk(player);",
                "return claimService.sendClaimPrice(player);",
                "return claimService.unclaimChunk(player);",
                "return claimService.canBuild(player, claim);",
                "return claimService.shouldCancelNationBeaconPlace(player, claim);",
                "return claimService.canPlaceNationCoreStructure(player, claim);",
                "return claimService.registerNationCoreStructure(player, claim);",
                "claimService.undoNationCoreStructure(nationId, claim, removeCreatedClaim);",
                "return claimService.shouldApplyBeaconFatigue(player, claim);",
                "return claimService.shouldBlockWarEntry(player, claim);"
        )) {
            assertTrue("Facade missing claim delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void claimImplementationNoLongerLivesInFacade() throws IOException {
        String facade = source("TownService.java");

        for (String snippet : List.of(
                "town.claims().add(claim)",
                "town.claims().remove(claim)",
                "nation.setBeaconClaim(claim)",
                "plugin.chunkClaimCost(claim",
                "plugin.neutralZones().claimBlockedBy(claim)"
        )) {
            assertFalse("Claim implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=ClaimServiceContractTest test
```

Expected failure mentions missing claim service methods.

- [ ] **Step 3: Move claim methods**

Move these methods from `TownService` to `ClaimService`:

```text
claimChunk
sendClaimPrice
unclaimChunk
canBuild
shouldCancelNationBeaconPlace
canPlaceNationCoreStructure
registerNationCoreStructure
undoNationCoreStructure
shouldApplyBeaconFatigue
shouldBlockWarEntry
giveNationBeacon
```

Use `nationService.ensureNationActive(player, nation)` and `nationService.canManageNation(player, town, nation)`.

Use `domainQuery` for:

```text
claimTown
nationForClaim
nationIdForClaim
nationHasOpenWar
isAdjacentToNationClaim
isNationMember
nationForBeaconClaim
nationClaimCount
```

Keep these public facade methods in `TownService` because listeners and structure services already call them:

```text
claimChunk
sendClaimPrice
unclaimChunk
canBuild
shouldCancelNationBeaconPlace
canPlaceNationCoreStructure
registerNationCoreStructure
undoNationCoreStructure
shouldApplyBeaconFatigue
shouldBlockWarEntry
```

- [ ] **Step 4: Replace `TownService` claim methods with delegates**

Use this facade style:

```java
public boolean claimChunk(Player player) {
    return claimService.claimChunk(player);
}

public boolean sendClaimPrice(Player player) {
    return claimService.sendClaimPrice(player);
}

public boolean unclaimChunk(Player player) {
    return claimService.unclaimChunk(player);
}

public boolean canBuild(Player player, ClaimKey claim) {
    return claimService.canBuild(player, claim);
}

public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim) {
    return claimService.shouldCancelNationBeaconPlace(player, claim);
}

public boolean canPlaceNationCoreStructure(Player player, ClaimKey claim) {
    return claimService.canPlaceNationCoreStructure(player, claim);
}

public boolean registerNationCoreStructure(Player player, ClaimKey claim) {
    return claimService.registerNationCoreStructure(player, claim);
}

public void undoNationCoreStructure(String nationId, ClaimKey claim, boolean removeCreatedClaim) {
    claimService.undoNationCoreStructure(nationId, claim, removeCreatedClaim);
}

public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim) {
    return claimService.shouldApplyBeaconFatigue(player, claim);
}

public boolean shouldBlockWarEntry(Player player, ClaimKey claim) {
    return claimService.shouldBlockWarEntry(player, claim);
}
```

- [ ] **Step 5: Run focused and full tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=ClaimServiceContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/ClaimService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/ClaimServiceContractTest.java
git commit -m "refactor(town): extract claim service"
```

---

### Task 6: Extract War Service

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/WarService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/WarServiceContractTest.java`

- [ ] **Step 1: Write failing war contract test**

Create `WarServiceContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class WarServiceContractTest {
    @Test
    public void warMethodsMoveBehindFacade() throws IOException {
        String facade = source("TownService.java");
        String service = source("WarService.java");

        for (String method : List.of(
                "public boolean declareWar(Player player, String targetNationName)",
                "public boolean declareWar(Player player, String targetNationName, WarMode mode)",
                "public boolean acceptWar(Player player, String attackerNationName)",
                "public boolean surrenderWar(Player player, String enemyNationName)",
                "public boolean releaseWarProtection(Player player, String enemyNationName)",
                "public boolean payWarDebt(Player player)",
                "public boolean finishWar(Player player, String winnerName, String loserName)",
                "public boolean canPvp(Player attacker, Player victim)",
                "public void processExpiredWarState()"
        )) {
            assertTrue("War service missing method: " + method, service.contains(method));
            assertTrue("Facade missing method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "return warService.declareWar(player, targetNationName);",
                "return warService.declareWar(player, targetNationName, mode);",
                "return warService.acceptWar(player, attackerNationName);",
                "return warService.surrenderWar(player, enemyNationName);",
                "return warService.releaseWarProtection(player, enemyNationName);",
                "return warService.payWarDebt(player);",
                "return warService.finishWar(player, winnerName, loserName);",
                "return warService.canPvp(attacker, victim);",
                "warService.processExpiredWarState();"
        )) {
            assertTrue("Facade missing war delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void warImplementationNoLongerLivesInFacade() throws IOException {
        String facade = source("TownService.java");

        for (String snippet : List.of(
                "new War(War.id(",
                "war.setStatus(WarStatus.ACTIVE)",
                "war.setDefenderProtectionActive",
                "war.setProtectionUntil",
                "surrenderer.setDebtAmount"
        )) {
            assertFalse("War implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=WarServiceContractTest test
```

Expected failure mentions missing war service methods.

- [ ] **Step 3: Move war methods**

Move these methods from `TownService` to `WarService`:

```text
declareWar(Player, String)
declareWar(Player, String, WarMode)
acceptWar
surrenderWar
releaseWarProtection
payWarDebt
finishWar
canPvp
applySurrenderPayment
clearDebt
isWarProtectedAgainst
processExpiredWarState
hasExpiredWarDebt
```

Make `processExpiredWarState()` public or package-private in `WarService` so `TownService.reload()` can call it if needed.

Use `domainQuery` for:

```text
playerTown
playerNation
nationByName
findWarBetween
findPendingWar
nationForClaim
```

Use `nationService.requireNationLeader(player, town)` and `nationService.ensureNationActive(player, nation)`.

- [ ] **Step 4: Replace `TownService` war methods with delegates**

Use this facade style:

```java
public boolean declareWar(Player player, String targetNationName) {
    return warService.declareWar(player, targetNationName);
}

public boolean declareWar(Player player, String targetNationName, WarMode mode) {
    return warService.declareWar(player, targetNationName, mode);
}

public boolean acceptWar(Player player, String attackerNationName) {
    return warService.acceptWar(player, attackerNationName);
}

public boolean surrenderWar(Player player, String enemyNationName) {
    return warService.surrenderWar(player, enemyNationName);
}

public boolean releaseWarProtection(Player player, String enemyNationName) {
    return warService.releaseWarProtection(player, enemyNationName);
}

public boolean payWarDebt(Player player) {
    return warService.payWarDebt(player);
}

public boolean finishWar(Player player, String winnerName, String loserName) {
    return warService.finishWar(player, winnerName, loserName);
}

public boolean canPvp(Player attacker, Player victim) {
    return warService.canPvp(attacker, victim);
}
```

Where `TownService` currently calls `processExpiredWarState()`, replace with:

```java
warService.processExpiredWarState();
```

- [ ] **Step 5: Run focused and full tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=WarServiceContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/WarService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/WarServiceContractTest.java
git commit -m "refactor(town): extract war service"
```

---

### Task 7: Extract Display Service and Placeholder Boundary

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownDisplayService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/hook/TownPlaceholderExpansion.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownMembershipService.java` when broadcast calls move to `TownDisplayService`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/NationService.java` when broadcast calls move to `TownDisplayService`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/ClaimService.java` when display refresh calls move to `TownDisplayService`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/WarService.java` when broadcast calls move to `TownDisplayService`
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/service/TownDisplayServiceContractTest.java`

- [ ] **Step 1: Write failing display contract test**

Create `TownDisplayServiceContractTest.java`:

```java
package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownDisplayServiceContractTest {
    @Test
    public void displayMethodsMoveBehindFacade() throws IOException {
        String facade = source("TownService.java");
        String service = source("TownDisplayService.java");

        for (String method : List.of(
                "public void setChatMode(Player player, ChatMode mode)",
                "public void sendTownChat(Player player, Component message)",
                "public void sendNationChat(Player player, Component message)",
                "public void broadcastGlobalChat(Player player, Component message)",
                "public Component chatLine(String formatPath, Player player, Component message)",
                "public String affiliationPrefix(Player player)",
                "public String rankPrefix(Player player)",
                "public void updateIdentity(Player player)",
                "public void updateAllIdentities()",
                "public void sendSelfInfo(Player player)",
                "public String info(Town town)"
        )) {
            assertTrue("Display service missing method: " + method, service.contains(method));
            assertTrue("Facade missing method: " + method, facade.contains(method));
        }

        for (String delegate : List.of(
                "displayService.setChatMode(player, mode);",
                "displayService.sendTownChat(player, message);",
                "displayService.sendNationChat(player, message);",
                "displayService.broadcastGlobalChat(player, message);",
                "return displayService.chatLine(formatPath, player, message);",
                "return displayService.affiliationPrefix(player);",
                "return displayService.rankPrefix(player);",
                "displayService.updateIdentity(player);",
                "displayService.updateAllIdentities();",
                "displayService.sendSelfInfo(player);",
                "return displayService.info(town);"
        )) {
            assertTrue("Facade missing display delegate: " + delegate, facade.contains(delegate));
        }
    }

    @Test
    public void displayImplementationNoLongerLivesInFacade() throws IOException {
        String facade = source("TownService.java");

        for (String snippet : List.of(
                "PlaceholderAPI.setPlaceholders",
                "plugin.scoreboardService().updateIdentity",
                "plugin.getConfig().getString(\"chat.",
                "prefix.nation",
                "rank-image."
        )) {
            assertFalse("Display implementation still in facade: " + snippet, facade.contains(snippet));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownDisplayServiceContractTest test
```

Expected failure mentions missing display service methods.

- [ ] **Step 3: Move display methods**

Move these methods from `TownService` to `TownDisplayService`:

```text
setChatMode
sendTownChat
sendNationChat
broadcastGlobalChat
chatLine
affiliationPrefix
rankPrefix
rankImage
updateIdentity
updateAllIdentities
sendSelfInfo
info
onlineNationMembers
broadcastTown
broadcastMembers
broadcastNation
```

Use `domainQuery` for:

```text
playerTown
playerNation
nationMemberCount
```

Keep `PlainTextComponentSerializer` static field in `TownDisplayService`.

- [ ] **Step 4: Replace `TownService` display methods with delegates**

Use this facade style:

```java
public void setChatMode(Player player, ChatMode mode) {
    displayService.setChatMode(player, mode);
}

public void sendTownChat(Player player, Component message) {
    displayService.sendTownChat(player, message);
}

public void sendNationChat(Player player, Component message) {
    displayService.sendNationChat(player, message);
}

public void broadcastGlobalChat(Player player, Component message) {
    displayService.broadcastGlobalChat(player, message);
}

public Component chatLine(String formatPath, Player player, Component message) {
    return displayService.chatLine(formatPath, player, message);
}

public String affiliationPrefix(Player player) {
    return displayService.affiliationPrefix(player);
}

public String rankPrefix(Player player) {
    return displayService.rankPrefix(player);
}

public void updateIdentity(Player player) {
    displayService.updateIdentity(player);
}

public void updateAllIdentities() {
    displayService.updateAllIdentities();
}

public void sendSelfInfo(Player player) {
    displayService.sendSelfInfo(player);
}

public String info(Town town) {
    return displayService.info(town);
}
```

- [ ] **Step 5: Keep PlaceholderAPI contract stable**

Do not rename placeholder parameters in `TownPlaceholderExpansion`.

If moving direct display calls, use this pattern:

```java
if (params.equalsIgnoreCase("affiliation")) {
    return Text.color(plugin.townService().affiliationPrefix(player));
}
if (params.equalsIgnoreCase("rank")) {
    return Text.color(plugin.townService().rankPrefix(player));
}
```

The existing `TownPlaceholderContractTest` must still pass.

- [ ] **Step 6: Run focused and contract tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=TownDisplayServiceContractTest,TownPlaceholderContractTest test
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 7: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/TownDisplayService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java \
        LeeSeolTown/src/main/java/me/leeseol/town/hook/TownPlaceholderExpansion.java \
        LeeSeolTown/src/test/java/me/leeseol/town/service/TownDisplayServiceContractTest.java
git commit -m "refactor(town): extract display service"
```

---

### Task 8: Shrink Facade Imports and Verify Contract Map

**Files:**

- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java`
- Modify if needed: `PLUGIN_INDEX.md`

- [ ] **Step 1: Remove unused imports and dead private helpers from `TownService`**

After Tasks 2-7, `TownService` should no longer need these imports unless a delegate signature still requires them:

```java
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
```

Keep only imports needed for public facade method signatures and service wiring.

- [ ] **Step 2: Run compile check**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: Check `PLUGIN_INDEX.md`**

Run:

```bash
rg -n "LeeSeolTown|service/|structure/|contract" PLUGIN_INDEX.md
```

If the LeeSeolTown row does not mention the new focused service package responsibilities, update only the LeeSeolTown row. Keep it short:

```text
service/ split: TownService facade, TownDomainQuery, TownMembershipService,
NationService, ClaimService, WarService, TownDisplayService
```

Do not add process logs, dates, failed attempts, or development history.

- [ ] **Step 4: Run final checks**

Run:

```bash
git diff --check
git status --short
mvn -f LeeSeolTown/pom.xml test package
```

Expected:

```text
git diff --check: no output
git status --short: only intended files before commit
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

If `PLUGIN_INDEX.md` changed:

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java PLUGIN_INDEX.md
git commit -m "docs: update town service contract map"
```

If only `TownService.java` cleanup changed:

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/service/TownService.java
git commit -m "refactor(town): clean facade imports"
```

---

## Final Verification

Run:

```bash
mvn -f LeeSeolTown/pom.xml test package
git diff --check
git status --short
```

Expected:

```text
BUILD SUCCESS
git diff --check: no output
git status --short: empty
```

Do not deploy or restart services as part of this plan.

## Self-Review

Spec coverage:

- `TownMembershipService`: Task 3.
- `NationService`: Task 4.
- `ClaimService`: Task 5.
- `WarService`: Task 6.
- `TownDisplayService`: Task 7.
- `TownService` facade compatibility: Tasks 1-8.
- Stable placeholders, commands, data keys, bridge config: contract tests plus final `PLUGIN_INDEX.md` check.

Type consistency:

- All focused services use `LeeSeolTownPlugin`, `TownStore`, and `TownDomainQuery`.
- Services that need player-facing confirmation use `TownConfirmationService`.
- Claim and war services call public methods on `NationService` instead of duplicating active/manage checks.
- Callers continue using `TownService` facade methods.

Known implementation constraint:

- The current test stack has JUnit 4 only and no Bukkit mock framework. Use source contract tests for Bukkit-heavy delegation and keep pure behavior tests for model/policy classes.
