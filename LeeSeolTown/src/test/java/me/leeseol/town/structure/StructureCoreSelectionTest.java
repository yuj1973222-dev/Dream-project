package me.leeseol.town.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class StructureCoreSelectionTest {
    @Test
    public void untaggedItemsAdderCoreRequiresGuiSelection() {
        assertNull(StructureCoreSelection.selectedStructureId(null, "leeseolwar:capital_core"));
    }

    @Test
    public void persistentSelectionWinsAfterGuiChoice() {
        assertEquals("nation_core", StructureCoreSelection.selectedStructureId("nation_core", "leeseolwar:capital_core"));
    }
}
