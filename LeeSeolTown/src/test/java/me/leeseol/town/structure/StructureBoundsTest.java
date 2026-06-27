package me.leeseol.town.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import me.leeseol.town.model.ClaimKey;
import org.junit.Test;

public final class StructureBoundsTest {
    @Test
    public void anchorsAtChunkCenter() {
        assertEquals(40, StructureBounds.anchorBlockX(2));
        assertEquals(-24, StructureBounds.anchorBlockZ(-2));
    }

    @Test
    public void detectsSingleChunkNationCoreBounds() {
        StructureDefinition core = new StructureDefinition(
                "nation_core", "Nation Core", "BEACON", "nation_core.schem",
                StructureCategory.NATION_CORE, "leeseoltown.structure.nation_core",
                15, 12, 15, -7, 0, -7
        );

        ClaimKey claim = new ClaimKey("world", 3, -4);
        Set<ClaimKey> affected = StructureBounds.affectedClaims(core, claim);

        assertEquals(Set.of(claim), affected);
        assertTrue(StructureBounds.fitsInsidePlacementChunk(core, claim));
    }

    @Test
    public void detectsMultiChunkNormalBounds() {
        StructureDefinition outpost = new StructureDefinition(
                "outpost", "Outpost", "CAMPFIRE", "outpost.schem",
                StructureCategory.NORMAL, "leeseoltown.structure.outpost",
                24, 10, 24, -12, 0, -12
        );

        Set<ClaimKey> affected = StructureBounds.affectedClaims(outpost, new ClaimKey("world", 0, 0));

        assertEquals(Set.of(
                new ClaimKey("world", -1, -1),
                new ClaimKey("world", -1, 0),
                new ClaimKey("world", -1, 1),
                new ClaimKey("world", 0, -1),
                new ClaimKey("world", 0, 0),
                new ClaimKey("world", 0, 1),
                new ClaimKey("world", 1, -1),
                new ClaimKey("world", 1, 0),
                new ClaimKey("world", 1, 1)
        ), affected);
    }
}
