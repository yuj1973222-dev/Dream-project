package me.leeseol.jobs.service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.jobs.model.JobType;
import org.bukkit.entity.Player;

public final class CooldownService {
    private final Map<JobType, Map<UUID, Long>> cooldowns = new EnumMap<>(JobType.class);

    public CooldownService() {
        for (JobType type : JobType.values()) {
            cooldowns.put(type, new HashMap<>());
        }
    }

    public boolean checkAndSet(Player player, JobType type, long cooldownMillis) {
        if (player.hasPermission("leeseoljobs.bypass.cooldown") || cooldownMillis <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        Map<UUID, Long> byPlayer = cooldowns.get(type);
        long last = byPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMillis) {
            return false;
        }
        byPlayer.put(player.getUniqueId(), now);
        return true;
    }
}
