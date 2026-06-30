package me.leeseol.hud.service;

import me.leeseol.hud.model.HudPreference;
import me.leeseol.hud.storage.HudStore;
import org.bukkit.entity.Player;

public final class PlayerHudStateService {
    private final HudStore store;

    public PlayerHudStateService(HudStore store) {
        this.store = store;
    }

    public boolean compass(Player player) {
        return store.preference(player.getUniqueId()).compass();
    }

    public void setCompass(Player player, boolean enabled) {
        HudPreference preference = store.preference(player.getUniqueId());
        preference.setCompass(enabled);
        store.save();
    }

    public boolean targetHealth(Player player) {
        return store.preference(player.getUniqueId()).targetHealth();
    }

    public void setTargetHealth(Player player, boolean enabled) {
        HudPreference preference = store.preference(player.getUniqueId());
        preference.setTargetHealth(enabled);
        store.save();
    }
}
