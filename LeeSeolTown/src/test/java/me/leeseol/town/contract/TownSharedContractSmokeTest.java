package me.leeseol.town.contract;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public final class TownSharedContractSmokeTest {
    @Test
    public void keepsCommandAliasAndPermissionSurface() throws IOException {
        String pluginYml = resource("plugin.yml");

        assertContains("plugin.yml", pluginYml, List.of(
                "main: me.leeseol.town.LeeSeolTownPlugin",
                "- Vault",
                "- LeeSeolEconomy",
                "- LeeSeolCore",
                "- PlaceholderAPI",
                "- ItemsAdder",
                "- WorldEdit",
                "- WorldGuard",
                "- BlueMap",
                "  town:",
                "      - party",
                "      - village",
                "      - towny",
                "  tc:",
                "      - pc",
                "  nc:",
                "  leeseoltown.use:",
                "  leeseoltown.claim:",
                "  leeseoltown.chat:",
                "  leeseoltown.admin:",
                "  leeseoltown.neutral.bypass:",
                "  leeseoltown.structure.admin:",
                "  leeseoltown.structure.undo:",
                "  leeseoltown.structure.nation_core:",
                "  leeseoltown.structure.outpost:",
                "  leeseoltown.structure.supply_depot:"
        ));
    }

    @Test
    public void keepsFacadeMethodsConsumedByHooksListenersAndOtherPlugins() throws IOException {
        String townService = javaSource("service", "TownService.java");

        assertContains("TownService.java", townService, List.of(
                "public Town playerTown(Player player)",
                "public Nation playerNation(Player player)",
                "public Collection<Town> towns()",
                "public Collection<Nation> nations()",
                "public boolean canBuild(Player player, ClaimKey claim)",
                "public boolean shouldCancelNationBeaconPlace(Player player, ClaimKey claim)",
                "public boolean shouldApplyBeaconFatigue(Player player, ClaimKey claim)",
                "public boolean shouldBlockWarEntry(Player player, ClaimKey claim)",
                "public boolean canPvp(Player attacker, Player victim)",
                "public Nation nationForClaim(ClaimKey claim)",
                "public String nationIdForClaim(ClaimKey claim)",
                "public boolean nationHasOpenWar(Nation nation)",
                "public Town claimTown(ClaimKey claim)",
                "public String affiliationPrefix(Player player)",
                "public String rankPrefix(Player player)",
                "public Component chatLine(String formatPath, Player player, Component message)",
                "public void updateIdentity(Player player)",
                "public void updateAllIdentities()",
                "public void collectDueUpkeep(boolean force)",
                "public long dailyNationUpkeep(Nation nation)"
        ));
    }

    @Test
    public void keepsPlaceholderParametersForDisplayConsumers() throws IOException {
        String expansion = javaSource("hook", "TownPlaceholderExpansion.java");

        assertContains("TownPlaceholderExpansion.java", expansion, List.of(
                "return \"leeseoltown\";",
                "params.equalsIgnoreCase(\"affiliation\")",
                "params.equalsIgnoreCase(\"rank\")",
                "params.equalsIgnoreCase(\"has_party\")",
                "params.equalsIgnoreCase(\"has_town\")",
                "params.equalsIgnoreCase(\"town\")",
                "params.equalsIgnoreCase(\"party\")",
                "params.equalsIgnoreCase(\"nation\")",
                "params.equalsIgnoreCase(\"nation_color\")",
                "params.equalsIgnoreCase(\"nation_color_hex\")",
                "params.equalsIgnoreCase(\"nation_type\")"
        ));
    }

    @Test
    public void keepsSharedTownDataKeysAndWarModeSurface() throws IOException {
        String store = javaSource("storage", "TownStore.java");
        String nation = javaSource("model", "Nation.java");
        String warMode = javaSource("model", "WarMode.java");

        assertContains("TownStore.java", store, List.of(
                "loadTowns(data.getConfigurationSection(\"towns\"));",
                "loadNations(data.getConfigurationSection(\"nations\"));",
                "loadWars(data.getConfigurationSection(\"wars\"));",
                "data.set(base + \"beacon-claim\"",
                "data.set(base + \"upkeep.last-period\"",
                "data.set(base + \"upkeep.debt\"",
                "data.set(base + \"debt.creditor\"",
                "data.set(base + \"debt.amount\"",
                "data.set(base + \"debt.deadline\"",
                "data.set(base + \"functions-suspended\"",
                "WarMode.parseOrDefault(section.getString(base + \"mode\"))",
                "data.set(base + \"mode\", war.mode().id())"
        ));
        assertContains("Nation.java", nation, List.of(
                "public double upkeepDebt()",
                "public void setUpkeepDebt(double upkeepDebt)",
                "public String debtCreditorNationId()",
                "public double debtAmount()",
                "public long debtDeadline()",
                "public boolean functionsSuspended()",
                "public void setFunctionsSuspended(boolean functionsSuspended)"
        ));
        assertContains("WarMode.java", warMode, List.of(
                "INVASION(\"invasion\"",
                "TOTAL(\"total\"",
                "public static WarMode parse(String input)",
                "public static WarMode parseOrDefault(String input)"
        ));
    }

    @Test
    public void keepsExternalBridgeAndStructureSurface() throws IOException {
        String config = resource("config.yml");
        String plugin = javaSource("", "LeeSeolTownPlugin.java");
        String neutralZones = javaSource("service", "NeutralZoneManager.java");
        String coreItemService = javaSource("structure", "StructureCoreItemService.java");

        assertContains("config.yml", config, List.of(
                "data-file: \"/opt/minecraft/shared/town/data.yml\"",
                "core-content-file: \"plugins/LeeSeolCore/contents.yml\"",
                "region-prefix: \"leeseol_neutral_\"",
                "marker-set-id: \"leeseol-neutral-zones\"",
                "marker-set-id: \"leeseol-nation-claims\"",
                "allowed-core-blocks:",
                "\"leeseolwar:capital_core\"",
                "\"leeseolwar:border_outpost_core\"",
                "\"leeseolwar:supply_depot_core\"",
                "    nation_core:",
                "    outpost:",
                "    supply_depot:",
                "permission: \"leeseoltown.structure.nation_core\"",
                "permission: \"leeseoltown.structure.outpost\"",
                "permission: \"leeseoltown.structure.supply_depot\""
        ));
        assertContains("LeeSeolTownPlugin.java", plugin, List.of(
                "new VaultEconomyHook(this)",
                "new NeutralZoneManager(this)",
                "new WorldGuardNeutralZoneRegions(this)",
                "new BlueMapNeutralZoneMarkers(this, neutralZoneManager)",
                "new BlueMapNationClaimMarkers(this)",
                "new StructureCoreItemService(this)",
                "new WorldEditStructurePaster(this)",
                "registerOutgoingPluginChannel(this, \"BungeeCord\")"
        ));
        assertContains("NeutralZoneManager.java", neutralZones, List.of(
                "usingCoreContent()",
                "plugins/LeeSeolCore/contents.yml",
                "refreshBlueMapMarkers()",
                "syncWorldGuardRegion(zone)"
        ));
        assertContains("StructureCoreItemService.java", coreItemService, List.of(
                "new NamespacedKey(plugin, \"structure_core\")",
                "new NamespacedKey(plugin, \"structure_id\")",
                "dev.lone.itemsadder.api.CustomStack",
                "getNamespacedID",
                "getNamespacedId"
        ));
    }

    @Test
    public void keepsTownCommandSmokeSurface() throws IOException {
        String command = javaSource("command", "TownCommand.java");

        assertContains("TownCommand.java", command, List.of(
                "case \"create\" -> create(sender, args);",
                "case \"invite\" -> invite(sender, args);",
                "case \"accept\" -> accept(sender, args);",
                "case \"deny\", \"reject\" -> deny(sender, args);",
                "case \"join\" -> join(sender, args);",
                "case \"leave\" -> playerOnly(sender, player -> townService.leaveTown(player));",
                "case \"disband\" -> playerOnly(sender, player -> townService.disbandTown(player));",
                "case \"transfer\" -> transfer(sender, args);",
                "case \"kick\" -> kick(sender, args);",
                "case \"claim\" -> playerOnly(sender, player -> townService.claimChunk(player));",
                "case \"claimprice\", \"claimcost\" -> playerOnly(sender, player -> townService.sendClaimPrice(player));",
                "case \"unclaim\" -> playerOnly(sender, player -> townService.unclaimChunk(player));",
                "case \"me\", \"status\"",
                "case \"chat\" -> chat(sender, args);",
                "case \"nation\" -> nation(sender, args);",
                "case \"war\" -> war(sender, args);",
                "case \"structure\" -> structure(sender, args);",
                "case \"diag\", \"diagnose\" -> diag(sender, args);",
                "case \"reload\" -> reload(sender);",
                "townService.depositNationTreasury(player, amount)",
                "townService.payNationUpkeep(player)",
                "townService.declareWar(player, args[2], mode)",
                "townService.payWarDebt(player)",
                "townService.finishWar(player, args[2], args[3])",
                "List.of(\"invasion\", \"total\")",
                "\"/party diag [fix]\""
        ));
    }

    private static String javaSource(String packageName, String fileName) throws IOException {
        Path base = Path.of("src", "main", "java", "me", "leeseol", "town");
        Path path = packageName.isBlank() ? base.resolve(fileName) : base.resolve(packageName).resolve(fileName);
        return normalized(path);
    }

    private static String resource(String fileName) throws IOException {
        return normalized(Path.of("src", "main", "resources", fileName));
    }

    private static String normalized(Path path) throws IOException {
        return Files.readString(path).replace("\r\n", "\n");
    }

    private static void assertContains(String sourceName, String source, List<String> snippets) {
        for (String snippet : snippets) {
            assertTrue(sourceName + " missing contract snippet: " + snippet, source.contains(snippet));
        }
    }
}
