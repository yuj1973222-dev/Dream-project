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
