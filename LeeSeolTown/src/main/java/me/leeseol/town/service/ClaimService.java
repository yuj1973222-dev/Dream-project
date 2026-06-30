package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class ClaimService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final NationService nations;
    private final TownDisplayService display;

    public ClaimService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                        NationService nations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.nations = nations;
        this.display = display;
    }
}
