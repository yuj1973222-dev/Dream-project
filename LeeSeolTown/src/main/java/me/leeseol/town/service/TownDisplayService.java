package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TownDisplayService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;

    public TownDisplayService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
    }

    public void updateIdentity(Player player) {
        String prefix = plugin.townService().rankPrefix(player) + plugin.townService().affiliationPrefix(player);
        String rawName = prefix + "&f" + player.getName();
        player.setDisplayName(me.leeseol.town.util.Text.color(rawName));
        player.setPlayerListName(me.leeseol.town.util.Text.color(rawName));
        net.kyori.adventure.text.Component name = me.leeseol.town.util.Text.component(rawName);
        player.displayName(name);
        player.playerListName(name);
    }

    public void updateAllIdentities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateIdentity(player);
        }
    }
}
