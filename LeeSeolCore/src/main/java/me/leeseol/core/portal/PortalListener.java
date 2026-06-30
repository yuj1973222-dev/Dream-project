package me.leeseol.core.portal;

import me.leeseol.core.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PortalListener implements Listener {
    private final ConfigManager configManager;
    private final PortalManager portalManager;

    public PortalListener(ConfigManager configManager, PortalManager portalManager) {
        this.configManager = configManager;
        this.portalManager = portalManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to) || !configManager.isWorldEnabled(to.getWorld())) {
            return;
        }

        portalManager.handleMove(event.getPlayer(), to);
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }
}
