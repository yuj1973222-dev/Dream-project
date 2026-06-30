package me.leeseol.core.networkmove;

import java.util.Objects;
import org.bukkit.entity.Player;

public final class NetworkMoveService {
    private final NetworkMovePort movePort;

    public NetworkMoveService(NetworkMovePort movePort) {
        this.movePort = Objects.requireNonNull(movePort, "movePort");
    }

    public void move(Player player, String targetServer) {
        movePort.requestMove(player, targetServer);
    }

    public NetworkMovePort movePort() {
        return movePort;
    }
}
