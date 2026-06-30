package me.leeseol.town.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public final class StructureUndoHistoryTest {
    @Test
    public void remembersOnlyLatestUndoPerPlayer() {
        StructureUndoHistory<String> history = new StructureUndoHistory<>();
        UUID playerId = UUID.randomUUID();

        history.remember(playerId, "first");
        history.remember(playerId, "second");

        assertEquals(Optional.of("second"), history.get(playerId));
    }

    @Test
    public void removesUndoAfterSuccessfulUse() {
        StructureUndoHistory<String> history = new StructureUndoHistory<>();
        UUID playerId = UUID.randomUUID();

        history.remember(playerId, "undo");
        assertEquals(Optional.of("undo"), history.get(playerId));
        history.remove(playerId);

        assertTrue(history.get(playerId).isEmpty());
    }
}
