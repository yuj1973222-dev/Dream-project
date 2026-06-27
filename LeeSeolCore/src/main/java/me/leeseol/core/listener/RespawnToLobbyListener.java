package me.leeseol.core.listener;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class RespawnToLobbyListener implements Listener {
    private final LeeSeolCorePlugin plugin;

    public RespawnToLobbyListener(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("respawn-to-lobby.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        long delay = Math.max(1L, plugin.getConfig().getLong("respawn-to-lobby.delay-ticks", 20L));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> connectToLobby(player), delay);
    }

    private void connectToLobby(Player player) {
        if (!player.isOnline()) {
            return;
        }

        String targetServer = plugin.getConfig().getString("respawn-to-lobby.target-server", "lobby");
        if (targetServer == null || targetServer.isBlank()) {
            return;
        }

        String message = plugin.getConfig().getString("respawn-to-lobby.message", "");
        if (message != null && !message.isBlank()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        // Velocity 환경에서 동작 확인 필요: BungeeCord 호환 Connect subchannel을 사용한다.
        plugin.sendPlayerToServer(player, targetServer);
    }
}
