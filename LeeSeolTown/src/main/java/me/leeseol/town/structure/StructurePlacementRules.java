package me.leeseol.town.structure;

import java.util.Optional;
import java.util.function.Function;
import me.leeseol.town.model.ClaimKey;

public final class StructurePlacementRules {
    private StructurePlacementRules() {
    }

    public static Optional<PlacementFailure> validateTerritory(
            StructureDefinition definition,
            ClaimKey placementClaim,
            String playerNationId,
            Function<ClaimKey, String> ownerNationLookup,
            boolean nationHasOpenWar,
            boolean adminForce
    ) {
        return validateTerritory(
                definition,
                placementClaim,
                playerNationId,
                ownerNationLookup,
                nationHasOpenWar,
                adminForce,
                false
        );
    }

    public static Optional<PlacementFailure> validateTerritory(
            StructureDefinition definition,
            ClaimKey placementClaim,
            String playerNationId,
            Function<ClaimKey, String> ownerNationLookup,
            boolean nationHasOpenWar,
            boolean adminForce,
            boolean allowUnclaimedNationCore
    ) {
        if (adminForce) {
            return Optional.empty();
        }
        if (playerNationId == null || playerNationId.isBlank()) {
            return Optional.of(PlacementFailure.NOT_IN_NATION);
        }
        if (nationHasOpenWar) {
            return Optional.of(PlacementFailure.NATION_IN_WAR);
        }
        if (definition.nationCore() && !StructureBounds.fitsInsidePlacementChunk(definition, placementClaim)) {
            return Optional.of(PlacementFailure.NATION_CORE_OUTSIDE_CHUNK);
        }
        String placementOwner = ownerNationLookup.apply(placementClaim);
        boolean unclaimedInitialCore = allowUnclaimedNationCore
                && definition.nationCore()
                && placementOwner == null;
        if (!playerNationId.equals(placementOwner) && !unclaimedInitialCore) {
            return Optional.of(PlacementFailure.PLACEMENT_CHUNK_NOT_OWNED);
        }
        for (ClaimKey claim : StructureBounds.affectedClaims(definition, placementClaim)) {
            String owner = ownerNationLookup.apply(claim);
            if (unclaimedInitialCore && placementClaim.equals(claim) && owner == null) {
                continue;
            }
            if (!playerNationId.equals(owner)) {
                return Optional.of(PlacementFailure.OUTSIDE_NATION_TERRITORY);
            }
        }
        return Optional.empty();
    }
}
