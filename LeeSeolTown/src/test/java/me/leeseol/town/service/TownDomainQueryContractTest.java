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
        String source = Files.readString(Path.of(
                "src", "main", "java", "me", "leeseol", "town", "service", "TownService.java"
        ));

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
        String source = Files.readString(Path.of(
                "src", "main", "java", "me", "leeseol", "town", "service", "TownService.java"
        ));

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
