package me.leeseol.town.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import me.leeseol.town.util.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TownPlaceholderExpansion extends PlaceholderExpansion {
    private final LeeSeolTownPlugin plugin;

    public TownPlaceholderExpansion(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leeseoltown";
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

        Town town = plugin.townService().playerTown(player);
        Nation nation = plugin.townService().playerNation(player);

        if (params.equalsIgnoreCase("affiliation")) {
            return Text.color(plugin.townService().affiliationPrefix(player));
        }
        if (params.equalsIgnoreCase("rank")) {
            return Text.color(plugin.townService().rankPrefix(player));
        }
        if (params.equalsIgnoreCase("has_party") || params.equalsIgnoreCase("has_town")) {
            return town == null ? "false" : "true";
        }
        if (params.equalsIgnoreCase("town") || params.equalsIgnoreCase("party")) {
            return town == null ? "" : town.name();
        }
        if (params.equalsIgnoreCase("nation")) {
            return nation == null ? "" : nation.name();
        }
        if (params.equalsIgnoreCase("nation_color")) {
            return nation == null ? "" : nation.color().displayName();
        }
        if (params.equalsIgnoreCase("nation_color_hex")) {
            return nation == null ? "" : nation.color().hex();
        }
        if (params.equalsIgnoreCase("nation_type")) {
            return nation == null ? "" : nation.color().displayName();
        }
        return null;
    }
}
