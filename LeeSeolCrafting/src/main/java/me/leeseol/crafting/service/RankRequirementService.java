package me.leeseol.crafting.service;

import java.util.List;
import org.bukkit.entity.Player;

public final class RankRequirementService {
    private static final List<String> ORDER = List.of("PLAYER", "D", "C", "B", "A", "S");

    public boolean allowed(Player player, String requiredRank) {
        if (player.hasPermission("leeseolcrafting.bypass.rank")) {
            return true;
        }
        int required = index(requiredRank);
        if (required <= 0) {
            return true;
        }
        return currentIndex(player) >= required;
    }

    private int currentIndex(Player player) {
        for (int index = ORDER.size() - 1; index >= 1; index--) {
            String rank = ORDER.get(index);
            if (player.hasPermission("leeseolranks.rank." + rank.toLowerCase())) {
                return index;
            }
        }
        return 0;
    }

    private int index(String rank) {
        if (rank == null || rank.isBlank()) {
            return 0;
        }
        for (int index = 0; index < ORDER.size(); index++) {
            if (ORDER.get(index).equalsIgnoreCase(rank)) {
                return index;
            }
        }
        return 0;
    }
}
