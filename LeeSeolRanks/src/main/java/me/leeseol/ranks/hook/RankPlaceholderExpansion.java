package me.leeseol.ranks.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RankPlaceholderExpansion extends PlaceholderExpansion {
    private final LeeSeolRanksPlugin plugin;

    public RankPlaceholderExpansion(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leeseolranks";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lee_seol";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        Player player = offlinePlayer == null ? null : offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        RankData data = plugin.rankStore().getOrCreate(player);
        Rank rank = data.rank();
        if (params.equalsIgnoreCase("rank")) {
            return rank.display();
        }
        if (params.equalsIgnoreCase("rank_lower")) {
            return rank.name().toLowerCase();
        }
        if (params.equalsIgnoreCase("image")) {
            return image(player, rank);
        }
        if (params.equalsIgnoreCase("prefix")) {
            return image(player, rank) + " ";
        }
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf(data.kills());
        }
        if (params.equalsIgnoreCase("next")) {
            Rank next = rank.next();
            return next == null ? "" : next.display();
        }
        return null;
    }

    private String image(Player player, Rank rank) {
        String parsed = PlaceholderAPI.setPlaceholders(player, rank.imagePlaceholder());
        return parsed.equals(rank.imagePlaceholder()) ? rank.display() : parsed;
    }
}
