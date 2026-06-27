package me.leeseol.town.structure;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.leeseol.town.LeeSeolTownPlugin;

public final class StructureUndoService {
    private final LeeSeolTownPlugin plugin;
    private final StructureUndoHistory<StructureUndoRecord> history = new StructureUndoHistory<>();

    public StructureUndoService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public void remember(StructureUndoRecord record) {
        history.remember(record.playerId(), record);
    }

    public Optional<UUID> playerIdByName(String playerName) {
        return history.records().stream()
                .filter(record -> record.playerName().equalsIgnoreCase(playerName))
                .map(StructureUndoRecord::playerId)
                .findFirst();
    }

    public List<String> playerNames() {
        return history.records().stream()
                .map(StructureUndoRecord::playerName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    public StructureUndoRecord undo(UUID playerId) {
        Optional<StructureUndoRecord> optional = history.get(playerId);
        if (optional.isEmpty()) {
            return null;
        }
        StructureUndoRecord record = optional.get();
        record.edit().undo();
        if (record.hasNationCoreRegistration()) {
            plugin.townService().undoNationCoreStructure(
                    record.nationId(),
                    record.nationCoreClaim(),
                    record.removeCreatedClaim()
            );
        }
        history.remove(playerId);
        return record;
    }
}
