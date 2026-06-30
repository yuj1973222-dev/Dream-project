package me.leeseol.town.structure;

import java.util.UUID;
import me.leeseol.town.model.ClaimKey;

public record StructureUndoRecord(
        UUID playerId,
        String playerName,
        String structureId,
        String structureName,
        String nationId,
        ClaimKey nationCoreClaim,
        boolean removeCreatedClaim,
        StructureUndoEdit edit,
        long createdAtMillis
) {
    public boolean hasNationCoreRegistration() {
        return nationId != null && nationCoreClaim != null;
    }
}
