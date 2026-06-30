package me.leeseol.town.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.town.LeeSeolTownPlugin;
import org.bukkit.entity.Player;

public final class TownConfirmationService {
    private static final long CONFIRM_MILLIS = 30_000L;

    private final LeeSeolTownPlugin plugin;
    private final Map<UUID, PendingConfirmation> pending = new HashMap<>();

    public TownConfirmationService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean confirm(Player player, String kind, String id, String messageKey, String command, String name) {
        long now = System.currentTimeMillis();
        PendingConfirmation existing = pending.get(player.getUniqueId());
        if (existing != null
                && existing.kind().equals(kind)
                && existing.id().equals(id)
                && existing.expiresAt() >= now) {
            pending.remove(player.getUniqueId());
            return true;
        }

        pending.put(player.getUniqueId(), new PendingConfirmation(kind, id, now + CONFIRM_MILLIS));
        player.sendMessage(plugin.msg(messageKey)
                .replace("%name%", name)
                .replace("%command%", command));
        return false;
    }

    private record PendingConfirmation(String kind, String id, long expiresAt) {
    }
}
