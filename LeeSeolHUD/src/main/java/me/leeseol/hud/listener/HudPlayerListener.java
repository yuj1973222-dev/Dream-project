package me.leeseol.hud.listener;

import me.leeseol.hud.LeeSeolHudPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HudPlayerListener implements Listener {
    private final LeeSeolHudPlugin plugin;

    public HudPlayerListener(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.compassHudService().update(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.compassHudService().remove(event.getPlayer());
        plugin.targetHealthService().remove(event.getPlayer());
        plugin.healthDisplayService().clear(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.compassHudService().update(event.getPlayer());
            plugin.targetHealthService().remove(event.getPlayer());
        });
    }
}
