package me.leeseol.town.listener;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.NeutralZone;
import me.leeseol.town.service.NeutralZoneManager;
import me.leeseol.town.util.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NeutralZoneListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final NeutralZoneManager neutralZones;
    private final Map<UUID, String> currentZones = new HashMap<>();

    public NeutralZoneListener(LeeSeolTownPlugin plugin, NeutralZoneManager neutralZones) {
        this.plugin = plugin;
        this.neutralZones = neutralZones;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!neutralZones.blockBreakEnabled() || bypass(event.getPlayer())) {
            return;
        }
        NeutralZone zone = neutralZones.zoneAt(event.getBlock().getLocation());
        if (zone == null) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.msg("neutral-zone-protected").replace("%zone%", zone.id()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!neutralZones.blockPlaceEnabled() || bypass(event.getPlayer())) {
            return;
        }
        NeutralZone zone = neutralZones.zoneAt(event.getBlockPlaced().getLocation());
        if (zone == null) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.msg("neutral-zone-protected").replace("%zone%", zone.id()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!neutralZones.pvpEnabled() || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || bypass(attacker)) {
            return;
        }
        NeutralZone victimZone = neutralZones.zoneAt(victim.getLocation());
        NeutralZone attackerZone = neutralZones.zoneAt(attacker.getLocation());
        NeutralZone zone = victimZone != null ? victimZone : attackerZone;
        if (zone == null) {
            return;
        }
        event.setCancelled(true);
        attacker.sendMessage(plugin.msg("neutral-zone-pvp-denied").replace("%zone%", zone.id()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        NeutralZone zone = neutralZones.zoneAt(player.getLocation());
        if (zone == null) {
            currentZones.remove(player.getUniqueId());
        } else {
            currentZones.put(player.getUniqueId(), zone.id());
        }
        plugin.scoreboardService().loadInitialZone(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event)) {
            return;
        }
        Player player = event.getPlayer();
        NeutralZone zone = neutralZones.zoneAt(event.getTo());
        String previous = currentZones.get(player.getUniqueId());
        if (zone == null) {
            if (previous != null) {
                currentZones.remove(player.getUniqueId());
                String message = plugin.msgRaw("neutral-zone-exit-actionbar").replace("%zone%", previous);
                player.sendActionBar(Text.component(message));
                plugin.scoreboardService().refreshCurrentZone(player);
            }
            return;
        }
        if (zone.id().equals(previous)) {
            return;
        }
        if (previous != null) {
            String exitMessage = plugin.msgRaw("neutral-zone-exit-actionbar").replace("%zone%", previous);
            player.sendActionBar(Text.component(exitMessage));
        }
        currentZones.put(player.getUniqueId(), zone.id());
        plugin.scoreboardService().setNeutralZone(player, zone.id());
        String enterMessage = plugin.msgRaw("neutral-zone-enter-actionbar").replace("%zone%", zone.id());
        player.sendActionBar(Text.component(enterMessage));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentZones.remove(event.getPlayer().getUniqueId());
        plugin.scoreboardService().remove(event.getPlayer());
    }

    private boolean bypass(Player player) {
        return player.hasPermission("leeseoltown.neutral.bypass");
    }

    private boolean sameBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ();
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (!(damager instanceof Projectile projectile)) {
            return null;
        }
        ProjectileSource shooter = projectile.getShooter();
        return shooter instanceof Player player ? player : null;
    }
}
