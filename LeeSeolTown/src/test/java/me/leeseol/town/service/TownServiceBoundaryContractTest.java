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
