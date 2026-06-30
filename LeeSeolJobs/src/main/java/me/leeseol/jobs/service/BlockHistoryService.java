package me.leeseol.jobs.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.Block;

public final class BlockHistoryService {
    private final Map<String, Long> placedBlocks = new HashMap<>();

    public void markPlaced(Block block, long rememberMillis) {
        if (block == null || rememberMillis <= 0L) {
            return;
        }
        cleanup();
        placedBlocks.put(key(block.getLocation()), System.currentTimeMillis() + rememberMillis);
    }

    public boolean isPlayerPlaced(Block block) {
        if (block == null) {
            return false;
        }
        cleanup();
        Long until = placedBlocks.get(key(block.getLocation()));
        return until != null && until > System.currentTimeMillis();
    }

    public void remove(Block block) {
        if (block != null) {
            placedBlocks.remove(key(block.getLocation()));
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = placedBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private static String key(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
