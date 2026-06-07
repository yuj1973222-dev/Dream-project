package me.leeseol.core.listener;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinListener implements Listener {
    private final LeeSeolCorePlugin plugin;

    public JoinListener(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        if (!plugin.getConfig().getBoolean("welcome-message-enabled", false)) {
            return;
        }
        String message = plugin.getConfig()
                .getString("welcome-message", "&aWelcome, {player}.")
                .replace("{player}", event.getPlayer().getName());

        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}
