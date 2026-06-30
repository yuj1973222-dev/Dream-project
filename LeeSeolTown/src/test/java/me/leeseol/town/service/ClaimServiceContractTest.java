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
