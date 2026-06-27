package me.leeseol.proxy.queue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SurvivalQueue {
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public synchronized Registration enqueue(UUID playerId, String username) {
        int existingPosition = positionOf(playerId);
        if (existingPosition > 0) {
            return new Registration(false, existingPosition);
        }

        entries.put(playerId, new Entry(playerId, username));
        return new Registration(true, entries.size());
    }

    public synchronized Optional<Entry> peek() {
        return entries.values().stream().findFirst();
    }

    public synchronized Optional<Entry> poll() {
        Optional<Entry> next = peek();
        next.ifPresent(entry -> entries.remove(entry.playerId()));
        return next;
    }

    public synchronized void requeueAtBack(Entry entry) {
        entries.remove(entry.playerId());
        entries.put(entry.playerId(), entry);
    }

    public synchronized boolean remove(UUID playerId) {
        return entries.remove(playerId) != null;
    }

    public synchronized int positionOf(UUID playerId) {
        int position = 1;
        for (UUID queuedId : entries.keySet()) {
            if (queuedId.equals(playerId)) {
                return position;
            }
            position++;
        }
        return 0;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean contains(UUID playerId) {
        return entries.containsKey(playerId);
    }

    public synchronized List<Entry> snapshot() {
        return List.copyOf(entries.values());
    }

    public record Entry(UUID playerId, String username) {
    }

    public record Registration(boolean added, int position) {
    }
}
