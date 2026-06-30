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
