package me.leeseol.core.launchpad;

import me.leeseol.core.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class LaunchPadListener implements Listener {
    private final ConfigManager configManager;
    private final LaunchPadManager launchPadManager;

    public LaunchPadListener(ConfigManager configManager, LaunchPadManager launchPadManager) {
        this.configManager = configManager;
        this.launchPadManager = launchPadManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to) || !configManager.isWorldEnabled(to.getWorld())) {
            return;
        }

        launchPadManager.handleMove(event.getPlayer(), to);
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }
}
