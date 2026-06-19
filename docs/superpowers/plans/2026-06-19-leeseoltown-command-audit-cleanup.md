# LeeSeolTown Command Audit Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `LeeSeolTown` command names match the current gameplay model before doing the larger class split.

**Architecture:** This is phase 1 of the command cleanup. Keep the current `TownCommand` class mostly intact, but move player-visible command vocabulary to canonical names: `/party` as the public root, nation territory under `/party nation ...`, and federation under the nation command group. Old aliases remain hidden compatibility routes where removal could break existing users.

**Tech Stack:** Java 21, Paper API, Maven, JUnit 4.

---

## File Structure

- Modify `LeeSeolTown/src/main/resources/plugin.yml`
  - Make `party` the registered root command.
  - Keep `town` as a compatibility alias.
  - Remove old public aliases `village` and `towny`.
- Modify `LeeSeolTown/src/main/java/me/leeseol/town/LeeSeolTownPlugin.java`
  - Register the command as `party` instead of `town`.
- Modify `LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java`
  - Remove root `claim`, `claimprice`, `unclaim` from help and tab completion.
  - Keep root handlers as hidden compatibility aliases to avoid sudden breakage.
  - Add `/party nation federation ...` as the canonical federation route.
  - Remove root `federation` from help and tab completion.
  - Keep root `federation` as hidden admin compatibility.
  - Keep `/party war ...` as canonical for now.
- Modify `LeeSeolTown/src/main/java/me/leeseol/town/command/NationClaimCommand.java`
  - Keep parser compatibility for `buy`, `purchase`, `price`, `cost`, and `claimcost`.
  - Do not expose those aliases in tab completion.
- Create `LeeSeolTown/src/test/java/me/leeseol/town/command/PluginCommandDescriptorTest.java`
  - Assert `plugin.yml` exposes `party` as root and does not expose `village` or `towny`.
- Modify `LeeSeolTown/src/test/java/me/leeseol/town/command/NationClaimCommandTest.java`
  - Assert hidden compatibility aliases still parse.

## Task 1: Guard The Plugin Descriptor

**Files:**
- Create: `LeeSeolTown/src/test/java/me/leeseol/town/command/PluginCommandDescriptorTest.java`

- [ ] **Step 1: Write the descriptor test**

```java
package me.leeseol.town.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class PluginCommandDescriptorTest {
    @Test
    public void exposesPartyAsCanonicalRootCommand() throws IOException {
        String pluginYml = resourceText("/plugin.yml");

        assertTrue(pluginYml.contains("commands:\n  party:"));
        assertTrue(pluginYml.contains("usage: /party"));
        assertTrue(pluginYml.contains("      - town"));
        assertFalse(pluginYml.contains("      - village"));
        assertFalse(pluginYml.contains("      - towny"));
    }

    private String resourceText(String path) throws IOException {
        try (InputStream input = PluginCommandDescriptorTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
```

- [ ] **Step 2: Run the failing descriptor test**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=PluginCommandDescriptorTest test
```

Expected: FAIL because `plugin.yml` currently exposes `town` as the root command and still lists `village` and `towny`.

- [ ] **Step 3: Update `plugin.yml`**

Change the command block to:

```yaml
commands:
  party:
    description: Manages parties, nations, claims, and chat.
    usage: /party
    aliases:
      - town
  tc:
    description: Sends or toggles party chat.
    usage: /tc [message]
    aliases:
      - pc
  nc:
    description: Sends or toggles nation chat.
    usage: /nc [message]
```

- [ ] **Step 4: Update command registration**

In `LeeSeolTown/src/main/java/me/leeseol/town/LeeSeolTownPlugin.java`, change:

```java
register("town", townCommand);
```

to:

```java
register("party", townCommand);
```

- [ ] **Step 5: Run the descriptor test**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=PluginCommandDescriptorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/resources/plugin.yml LeeSeolTown/src/main/java/me/leeseol/town/LeeSeolTownPlugin.java LeeSeolTown/src/test/java/me/leeseol/town/command/PluginCommandDescriptorTest.java
git commit -m "refactor: make party the canonical town command"
```

## Task 2: Keep Nation Claim Aliases Compatible But Hidden

**Files:**
- Modify: `LeeSeolTown/src/test/java/me/leeseol/town/command/NationClaimCommandTest.java`
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/command/NationClaimCommand.java`

- [ ] **Step 1: Extend the alias parser test**

Replace `parsesNationClaimAliases` with:

```java
@Test
public void parsesNationClaimAliases() {
    assertEquals(NationClaimCommand.CLAIM, NationClaimCommand.parse("claim"));
    assertEquals(NationClaimCommand.CLAIM, NationClaimCommand.parse("buy"));
    assertEquals(NationClaimCommand.CLAIM, NationClaimCommand.parse("purchase"));
    assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("claimprice"));
    assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("claimcost"));
    assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("price"));
    assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("cost"));
    assertEquals(NationClaimCommand.UNCLAIM, NationClaimCommand.parse("unclaim"));
}
```

- [ ] **Step 2: Run the test**

Run:

```bash
mvn -f LeeSeolTown/pom.xml -Dtest=NationClaimCommandTest test
```

Expected: PASS if the existing parser already supports all compatibility aliases. If it fails, update `NationClaimCommand.parse`.

- [ ] **Step 3: Ensure parser includes all compatibility aliases**

The parser should be:

```java
static NationClaimCommand parse(String input) {
    if (input == null) {
        return null;
    }
    return switch (input.toLowerCase(Locale.ROOT)) {
        case "claim", "buy", "purchase" -> CLAIM;
        case "claimprice", "claimcost", "price", "cost" -> PRICE;
        case "unclaim" -> UNCLAIM;
        default -> null;
    };
}
```

- [ ] **Step 4: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/command/NationClaimCommand.java LeeSeolTown/src/test/java/me/leeseol/town/command/NationClaimCommandTest.java
git commit -m "test: cover nation claim command aliases"
```

## Task 3: Hide Root Claim Commands From Help And Tabs

**Files:**
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java`

- [ ] **Step 1: Keep root handlers for compatibility**

Keep this root switch behavior unchanged for now:

```java
case "claim" -> playerOnly(sender, player -> townService.claimChunk(player));
case "claimprice", "claimcost" -> playerOnly(sender, player -> townService.sendClaimPrice(player));
case "unclaim" -> playerOnly(sender, player -> townService.unclaimChunk(player));
```

These hidden routes avoid breaking players who still type the old commands.

- [ ] **Step 2: Remove root claim lines from help**

In `sendHelp`, remove these lines:

```java
sender.sendMessage("/party claim");
sender.sendMessage("/party claimprice");
sender.sendMessage("/party unclaim");
```

Keep these canonical lines:

```java
sender.sendMessage("/party nation claim");
sender.sendMessage("/party nation claimprice");
sender.sendMessage("/party nation unclaim");
```

- [ ] **Step 3: Remove root claim commands from first-level tab completion**

Change:

```java
List<String> options = new ArrayList<>(List.of("create", "invite", "accept", "deny", "join", "leave", "disband", "transfer", "kick", "claim", "claimprice", "unclaim", "info", "me", "chat", "nation", "war"));
```

to:

```java
List<String> options = new ArrayList<>(List.of("create", "invite", "accept", "deny", "join", "leave", "disband", "transfer", "kick", "info", "me", "chat", "nation", "war"));
```

- [ ] **Step 4: Hide non-canonical nation claim aliases from tab completion**

Change:

```java
return filter(List.of("create", "disband", "pvp", "build", "treasury", "upkeep", "deposit", "claim", "buy", "claimprice", "price", "unclaim"), args[1]);
```

to:

```java
return filter(List.of("create", "disband", "pvp", "build", "treasury", "upkeep", "deposit", "claim", "claimprice", "unclaim", "federation"), args[1]);
```

- [ ] **Step 5: Run tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java
git commit -m "refactor: show nation claims only under nation commands"
```

## Task 4: Move Federation Under Nation Commands

**Files:**
- Modify: `LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java`

- [ ] **Step 1: Keep root federation as hidden compatibility**

Keep the root switch route:

```java
case "federation" -> federation(sender, args);
```

Do not include `federation` in first-level tab completion or normal help after this task.

- [ ] **Step 2: Add nested nation federation handling**

At the start of `nation(CommandSender sender, String[] args)`, after the `disband` block and before `NationClaimCommand.parse`, add:

```java
if (args.length >= 2 && args[1].equalsIgnoreCase("federation")) {
    nationFederation(sender, args);
    return;
}
```

- [ ] **Step 3: Add `nationFederation` helper**

Add this method near `federation`:

```java
private void nationFederation(CommandSender sender, String[] args) {
    if (args.length >= 3 && args[2].equalsIgnoreCase("disband")) {
        playerOnly(sender, player -> townService.disbandNation(player));
        return;
    }
    if (args.length < 5 || !args[2].equalsIgnoreCase("create")) {
        sender.sendMessage("/party nation federation create <name> <party1> [party2] [party3] [...]");
        sender.sendMessage("/party nation federation disband");
        return;
    }
    playerOnly(sender, player -> townService.createFederation(player, args[3], Arrays.asList(args).subList(4, args.length)));
}
```

- [ ] **Step 4: Update admin help**

Replace:

```java
sender.sendMessage("/party federation create <name> <party1> [party2] [party3] [...]");
sender.sendMessage("/party federation disband");
```

with:

```java
sender.sendMessage("/party nation federation create <name> <party1> [party2] [party3] [...]");
sender.sendMessage("/party nation federation disband");
```

- [ ] **Step 5: Update federation tab completion**

Remove the old root federation tab completion block:

```java
if (args.length == 2 && args[0].equalsIgnoreCase("federation")) {
    return filter(List.of("create", "disband"), args[1]);
}
if (args.length >= 4 && args[0].equalsIgnoreCase("federation") && args[1].equalsIgnoreCase("create")) {
    return townService.towns().stream().map(Town::name).filter(name -> starts(name, args[args.length - 1])).toList();
}
```

Add nested tab completion in its place:

```java
if (args.length == 3 && args[0].equalsIgnoreCase("nation") && args[1].equalsIgnoreCase("federation")) {
    return filter(List.of("create", "disband"), args[2]);
}
if (args.length >= 5
        && args[0].equalsIgnoreCase("nation")
        && args[1].equalsIgnoreCase("federation")
        && args[2].equalsIgnoreCase("create")) {
    return townService.towns().stream().map(Town::name).filter(name -> starts(name, args[args.length - 1])).toList();
}
```

- [ ] **Step 6: Run tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add LeeSeolTown/src/main/java/me/leeseol/town/command/TownCommand.java
git commit -m "refactor: move federation commands under nation"
```

## Task 5: Build The Plugin

**Files:**
- No source edits expected.

- [ ] **Step 1: Run LeeSeolTown tests**

Run:

```bash
mvn -f LeeSeolTown/pom.xml test
```

Expected: PASS.

- [ ] **Step 2: Build LeeSeolTown jar**

Run:

```bash
mvn -f LeeSeolTown/pom.xml package
```

Expected: PASS and `LeeSeolTown/target/LeeSeolTown-0.1.0.jar` exists.

- [ ] **Step 3: Verify plugin descriptor in jar**

Run:

```bash
jar tf LeeSeolTown/target/LeeSeolTown-0.1.0.jar | Select-String 'plugin.yml'
```

Expected: output contains `plugin.yml`.

## Task 6: Decide Whether To Deploy

**Files:**
- No source edits expected unless deployment scripts need an existing project-specific path fix.

- [ ] **Step 1: Ask before deployment**

If tests and build pass, ask the user whether to deploy to survival now.

- [ ] **Step 2: If approved, deploy only LeeSeolTown**

Use the existing LeeSeolTown deployment flow. Before replacing the remote jar, make a timestamped backup. Restart only the `minecraft` service.

- [ ] **Step 3: Verify only the changed service**

Check:

```bash
sudo systemctl is-active minecraft
sudo journalctl -u minecraft --since "5 minutes ago" --no-pager
```

Expected:

- `minecraft` is active.
- recent log reaches `Done`.
- no new `LeeSeolTown` `ERROR`, `Exception`, or `Could not load` lines.

## Follow-up Plan

After this phase is complete and verified, create a separate plan for phase 2:

- split `TownCommand` into command groups;
- keep the canonical command vocabulary from this phase;
- add tests around any new parser/helper classes;
- do not touch other plugins in the same implementation pass.
