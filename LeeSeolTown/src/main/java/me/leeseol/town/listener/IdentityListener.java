package me.leeseol.town.listener;

import me.leeseol.town.service.TownService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class IdentityListener implements Listener {
    private final TownService townService;

    public IdentityListener(TownService townService) {
        this.townService = townService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        townService.updateIdentity(event.getPlayer());
    }
}
