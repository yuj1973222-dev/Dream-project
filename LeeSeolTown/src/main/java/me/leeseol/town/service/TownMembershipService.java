package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.storage.TownStore;

public final class TownMembershipService {
    private final LeeSeolTownPlugin plugin;
    private final TownStore store;
    private final TownDomainQuery query;
    private final TownConfirmationService confirmations;
    private final TownDisplayService display;

    public TownMembershipService(LeeSeolTownPlugin plugin, TownStore store, TownDomainQuery query,
                                 TownConfirmationService confirmations, TownDisplayService display) {
        this.plugin = plugin;
        this.store = store;
        this.query = query;
        this.confirmations = confirmations;
        this.display = display;
    }
}
