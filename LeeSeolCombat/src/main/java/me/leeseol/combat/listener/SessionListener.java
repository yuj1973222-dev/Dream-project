package me.leeseol.combat.listener;

import me.leeseol.combat.LeeSeolCombatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.GameMode;

public final class SessionListener implements Listener {
    private final LeeSeolCombatPlugin plugin;

    public SessionListener(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.isDead() || player.getHealth() <= 0.0D) {
            return;
        }
        if (player.hasPermission("leeseolcombat.bypass")) {
            return;
        }
        boolean tagged = plugin.combatTagManager().isTagged(player.getUniqueId());
        if (tagged && plugin.combatConfig().combatLogoutKill()) {
            // Combat logout is an immediate death penalty, not a logout corpse.
            // Normal survival quits below still create the Citizens corpse clone.
            plugin.combatTagManager().clear(player.getUniqueId());
            plugin.combatCloneManager().punishCombatLogout(player);
            return;
        }
        plugin.combatCloneManager().spawnLogoutClone(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.combatConfig().removeCloneOnReturn()) {
            plugin.combatCloneManager().removeClone(player.getUniqueId());
        }
        boolean pendingDeath = plugin.pendingDeathStore().consume(player.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (plugin.combatConfig().respawnDeadOnJoin() && player.isDead()) {
                player.spigot().respawn();
            }
            if (pendingDeath) {
                plugin.message(player, "pending-death");
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.combatCloneManager().applyForcedDeathState(player);
                    }
                }, 2L);
            } else if (plugin.combatConfig().respawnDeadOnJoin() && player.getHealth() <= 0.0D) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.combatCloneManager().applyForcedDeathState(player);
                    }
                }, 2L);
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            if (plugin.combatConfig().spectatorCloneEnabled()) {
                plugin.combatCloneManager().spawnSpectatorClone(player);
            }
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR && plugin.combatConfig().removeCloneOnReturn()) {
            plugin.combatCloneManager().removeClone(player.getUniqueId());
        }
    }
}
