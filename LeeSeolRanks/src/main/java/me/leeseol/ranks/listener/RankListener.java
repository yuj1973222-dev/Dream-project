package me.leeseol.ranks.listener;

import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import me.leeseol.ranks.service.RequirementResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RankListener implements Listener {
    private final LeeSeolRanksPlugin plugin;

    public RankListener(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        RankData data = plugin.rankStore().getOrCreate(event.getPlayer());
        data.setName(event.getPlayer().getName());
        plugin.permissionService().apply(event.getPlayer());
        plugin.permissionService().syncGlobalRank(event.getPlayer().getName(), data.rank());
        plugin.rankStore().save();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.permissionService().clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }

        RankData data = plugin.rankStore().getOrCreate(killer);
        data.setName(killer.getName());
        data.addKill();

        Rank next = data.rank().next();
        if (next == null) {
            plugin.rankStore().save();
            return;
        }

        int required = plugin.requiredKills(next);
        RequirementResult result = plugin.rankRequirementService().result(killer, data, next);
        if (result.met()) {
            plugin.message(killer, "rank-up-ready",
                    "%rank%", next.display(),
                    "%kills%", String.valueOf(data.kills()),
                    "%required%", String.valueOf(required));
        } else {
            plugin.message(killer, "kill-progress",
                    "%kills%", String.valueOf(data.kills()),
                    "%required%", String.valueOf(required));
        }
        plugin.rankStore().save();
    }
}
