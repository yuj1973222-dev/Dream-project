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
        String store = Files.readString(Path.of(
                "src", "main", "java", "me", "leeseol", "town", "storage", "TownStore.java"
        ));

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
    public void nationImplementationNoLongerLivesInFacadeSection() throws IOException {
        String facade = source("TownService.java");
        String nationFacadeSection = facade.substring(
                facade.indexOf("public boolean createNation"),
                facade.indexOf("public boolean declareWar")
        );

        for (String snippet : List.of(
                "new Nation(id, name, color",
                "nation.setTreasury",
                "nation.setUpkeepDebt",
                "nation.setFunctionsSuspended",
                "nation.setBuildProtectionEnabled",
                "nation.setPvpEnabled"
        )) {
            assertFalse("Nation implementation still in facade section: " + snippet,
                    nationFacadeSection.contains(snippet));
        }
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "me", "leeseol", "town", "service", fileName));
    }
}
