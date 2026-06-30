package me.leeseol.town.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;
import me.leeseol.town.model.ClaimKey;
import org.junit.Test;

public final class StructurePlacementRulesTest {
    private static final String NATION = "nation_a";

    @Test
    public void blocksPlacementDuringOpenWar() {
        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                normalStructure(),
                new ClaimKey("world", 0, 0),
                NATION,
                claim -> NATION,
                true,
                false
        );

        assertEquals(Optional.of(PlacementFailure.NATION_IN_WAR), failure);
    }

    @Test
    public void blocksNationCoreWhenItLeavesPlacementChunk() {
        StructureDefinition largeCore = new StructureDefinition(
                "nation_core", "Nation Core", "BEACON", "nation_core.schem",
                StructureCategory.NATION_CORE, "", 17, 10, 17, -8, 0, -8
        );

        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                largeCore,
                new ClaimKey("world", 0, 0),
                NATION,
                claim -> NATION,
                false,
                false
        );

        assertEquals(Optional.of(PlacementFailure.NATION_CORE_OUTSIDE_CHUNK), failure);
    }

    @Test
    public void blocksNormalStructureOutsideNationClaims() {
        Set<ClaimKey> owned = Set.of(new ClaimKey("world", 0, 0));

        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                normalStructure(),
                new ClaimKey("world", 0, 0),
                NATION,
                claim -> owned.contains(claim) ? NATION : null,
                false,
                false
        );

        assertEquals(Optional.of(PlacementFailure.OUTSIDE_NATION_TERRITORY), failure);
    }

    @Test
    public void allowsInitialNationCoreOnUnclaimedPlacementChunk() {
        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                nationCore(),
                new ClaimKey("world", 0, 0),
                NATION,
                claim -> null,
                false,
                false,
                true
        );

        assertTrue(failure.isEmpty());
    }

    @Test
    public void allowsAdminForcePlacement() {
        Optional<PlacementFailure> failure = StructurePlacementRules.validateTerritory(
                normalStructure(),
                new ClaimKey("world", 0, 0),
                "",
                claim -> null,
                true,
                true
        );

        assertTrue(failure.isEmpty());
    }

    private static StructureDefinition normalStructure() {
        return new StructureDefinition(
                "outpost", "Outpost", "CAMPFIRE", "outpost.schem",
                StructureCategory.NORMAL, "", 24, 10, 24, -12, 0, -12
        );
    }

    private static StructureDefinition nationCore() {
        return new StructureDefinition(
                "nation_core", "Nation Core", "BEACON", "nation_core.schem",
                StructureCategory.NATION_CORE, "", 15, 12, 15, -7, 0, -7
        );
    }
}
