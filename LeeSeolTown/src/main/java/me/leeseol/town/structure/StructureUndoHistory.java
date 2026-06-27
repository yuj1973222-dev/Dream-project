package me.leeseol.town.structure;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StructureUndoHistory<T> {
    private final Map<UUID, T> records = new HashMap<>();

    public void remember(UUID playerId, T record) {
        records.put(playerId, record);
    }

    public Optional<T> get(UUID playerId) {
        return Optional.ofNullable(records.get(playerId));
    }

    public void remove(UUID playerId) {
        records.remove(playerId);
    }

    public Collection<T> records() {
        return List.copyOf(records.values());
    }
}
