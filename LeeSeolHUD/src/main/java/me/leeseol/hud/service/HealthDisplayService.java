package me.leeseol.hud.service;

import me.leeseol.hud.LeeSeolHudPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HealthDisplayService {
    private final LeeSeolHudPlugin plugin;
    private final Map<UUID, HealPulse> heals = new ConcurrentHashMap<>();

    public HealthDisplayService(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    public void recordHeal(Player player, double requestedAmount) {
        if (!plugin.getConfig().getBoolean("below-name-health.healing.enabled", true)) {
            return;
        }
        double maxHealth = maxHealth(player);
        double appliedAmount = Math.min(Math.max(0.0D, requestedAmount), Math.max(0.0D, maxHealth - player.getHealth()));
        if (appliedAmount <= 0.0D) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long durationMillis = Math.max(1L, plugin.getConfig().getLong("below-name-health.healing.duration-ticks", 60L)) * 50L;
        heals.compute(uuid, (ignored, current) -> {
            if (current == null || current.expiresAt <= now) {
                return new HealPulse(appliedAmount, now + durationMillis);
            }
            return new HealPulse(current.amount + appliedAmount, now + durationMillis);
        });
    }

    public String healSuffix(Player player) {
        HealPulse pulse = heals.get(player.getUniqueId());
        if (pulse == null) {
            return "";
        }
        long now = System.currentTimeMillis();
        if (pulse.expiresAt <= now) {
            heals.remove(player.getUniqueId());
            return "";
        }

        long durationMillis = Math.max(1L, plugin.getConfig().getLong("below-name-health.healing.duration-ticks", 60L)) * 50L;
        double remaining = Math.max(0.0D, Math.min(1.0D, (double) (pulse.expiresAt - now) / durationMillis));
        String color = colorFor(remaining);
        int amount = Math.max(1, (int) Math.round(pulse.amount));
        String format = plugin.getConfig().getString("below-name-health.healing.format", " %color%+%amount%");
        return format
            .replace("%color%", color)
            .replace("%amount%", String.valueOf(amount));
    }

    public void clear(Player player) {
        heals.remove(player.getUniqueId());
    }

    private String colorFor(double remaining) {
        if (remaining > 0.66D) {
            return plugin.getConfig().getString("below-name-health.healing.color-high", "&a");
        }
        if (remaining > 0.33D) {
            return plugin.getConfig().getString("below-name-health.healing.color-mid", "&2");
        }
        return plugin.getConfig().getString("below-name-health.healing.color-low", "&7");
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0D : Math.max(1.0D, attribute.getValue());
    }

    private record HealPulse(double amount, long expiresAt) {
    }
}
