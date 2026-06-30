package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class TownDisplayService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;

    public TownDisplayService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
    }
}
