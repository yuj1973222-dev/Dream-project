package me.leeseol.town.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TerritoryTransitionTest {
    @Test
    public void detectsEnteringNationTerritory() {
        TerritoryTransition transition = TerritoryTransition.between(
                TerritorySnapshot.wilderness(),
                TerritorySnapshot.nation("nation_a", "Alpha")
        );

        assertTrue(transition.changed());
        assertTrue(transition.enteredNation());
        assertFalse(transition.enteredWilderness());
    }

    @Test
    public void detectsLeavingNationTerritory() {
        TerritoryTransition transition = TerritoryTransition.between(
                TerritorySnapshot.nation("nation_a", "Alpha"),
                TerritorySnapshot.wilderness()
        );

        assertTrue(transition.changed());
        assertFalse(transition.enteredNation());
        assertTrue(transition.enteredWilderness());
    }

    @Test
    public void usesNationColorForTerritoryDisplayLabel() {
        TerritorySnapshot snapshot = TerritorySnapshot.nation("nation_a", "Alpha", "&#8EC5FF");

        assertEquals("&#8EC5FFAlpha", snapshot.coloredLabel());
    }
}
