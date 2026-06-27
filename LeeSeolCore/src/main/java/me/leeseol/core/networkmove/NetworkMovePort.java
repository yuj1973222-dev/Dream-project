package me.leeseol.core.networkmove;

import org.bukkit.entity.Player;

public interface NetworkMovePort {
    void requestMove(Player player, String targetServer);
}
