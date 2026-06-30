package me.leeseol.core.content;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ContentBlueMapMarkerPolicyTest {
    @Test
    public void exposesOnlyCasinoAndDungeonContent() {
        assertFalse(ContentBlueMapMarkerPolicy.visibleOnBlueMap(ContentType.NEUTRAL));
        assertTrue(ContentBlueMapMarkerPolicy.visibleOnBlueMap(ContentType.CASINO));
        assertTrue(ContentBlueMapMarkerPolicy.visibleOnBlueMap(ContentType.DUNGEON));
    }

    @Test
    public void createsStableMarkerIds() {
        assertEquals("content_casino_gold_room", ContentBlueMapMarkerPolicy.markerId(ContentType.CASINO, "gold_room"));
        assertEquals("content_dungeon_first_gate", ContentBlueMapMarkerPolicy.markerId(ContentType.DUNGEON, "first_gate"));
    }
}
