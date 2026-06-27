package me.leeseol.enchanting.service;

import org.bukkit.Material;
import org.bukkit.block.Block;

public final class BookshelfCounter {
    private final EnchantingConfig config;

    public BookshelfCounter(EnchantingConfig config) {
        this.config = config;
    }

    public int count(Block table) {
        if (table == null || table.getType() != Material.ENCHANTING_TABLE) {
            return 0;
        }
        if (config.requireClearInnerSpace() && !clearInnerSpace(table)) {
            return 0;
        }
        int count = 0;
        for (int y = 0; y <= 1; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                        continue;
                    }
                    if (config.bookshelf(table.getRelative(x, y, z).getType())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private boolean clearInnerSpace(Block table) {
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    Block block = table.getRelative(x, y, z);
                    if (!block.getType().isAir() && !block.isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
