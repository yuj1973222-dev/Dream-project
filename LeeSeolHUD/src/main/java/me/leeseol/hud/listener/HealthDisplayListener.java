package me.leeseol.hud.listener;

import me.leeseol.hud.LeeSeolHudPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public final class HealthDisplayListener implements Listener {
    private final LeeSeolHudPlugin plugin;

    public HealthDisplayListener(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.healthDisplayService().recordHeal(player, event.getAmount());
        }
    }
}
