package me.leeseol.combat.listener;

import me.leeseol.combat.LeeSeolCombatPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PvpRewardListener implements Listener {
    private final LeeSeolCombatPlugin plugin;

    public PvpRewardListener(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        plugin.pvpRewardService().handleDeath(event);
    }
}
