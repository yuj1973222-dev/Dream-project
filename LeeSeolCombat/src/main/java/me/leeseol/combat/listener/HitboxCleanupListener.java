package me.leeseol.combat.listener;

import me.leeseol.combat.LeeSeolCombatPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class HitboxCleanupListener implements Listener {
    private final LeeSeolCombatPlugin plugin;

    public HitboxCleanupListener(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.combatCloneManager().cleanupStaleHitboxes(event.getChunk());
    }
}
