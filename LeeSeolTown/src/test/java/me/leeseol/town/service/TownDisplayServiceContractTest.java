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
