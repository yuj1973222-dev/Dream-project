package me.leeseol.core.networkmove;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BungeeCordNetworkMovePort implements NetworkMovePort {
    public static final String CHANNEL = "BungeeCord";

    private final Plugin plugin;

    public BungeeCordNetworkMovePort(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void requestMove(Player player, String targetServer) {
        if (player == null || targetServer == null || targetServer.isBlank()) {
            return;
        }

        player.sendPluginMessage(plugin, CHANNEL, BungeeCordConnectPayload.connect(targetServer));
    }
}
