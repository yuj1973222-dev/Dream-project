package me.leeseol.town.listener;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.service.TerritorySnapshot;
import me.leeseol.town.service.TerritoryTransition;
import me.leeseol.town.service.TownService;
import me.leeseol.town.util.Text;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NationRuleListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final TownService townService;
    private final Map<UUID, TerritorySnapshot> currentTerritories = new HashMap<>();

    public NationRuleListener(LeeSeolTownPlugin plugin, TownService townService) {
        this.plugin = plugin;
        this.townService = townService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        currentTerritories.put(event.getPlayer().getUniqueId(), snapshot(ClaimKey.from(event.getPlayer().getLocation())));
        plugin.scoreboardService().refreshCurrentZone(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event)) {
            return;
        }
        Player player = event.getPlayer();
        ClaimKey fromClaim = ClaimKey.from(event.getFrom());
        ClaimKey toClaim = ClaimKey.from(event.getTo());
        if (fromClaim.equals(toClaim)) {
            return;
        }
        if (townService.shouldBlockWarEntry(player, toClaim)) {
            event.setTo(event.getFrom());
            player.sendMessage(plugin.msg("war-entry-blocked"));
            return;
        }
        handleTerritoryChange(player, fromClaim, toClaim);
        if (!townService.shouldApplyBeaconFatigue(player, toClaim)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0, true, false, true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || townService.canPvp(attacker, victim)) {
            return;
        }
        event.setCancelled(true);
        attacker.sendMessage(plugin.msg("nation-pvp-disabled"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentTerritories.remove(event.getPlayer().getUniqueId());
    }

    private void handleTerritoryChange(Player player, ClaimKey fromClaim, ClaimKey toClaim) {
        if (fromClaim.equals(toClaim)) {
            return;
        }
        TerritorySnapshot previous = currentTerritories.computeIfAbsent(player.getUniqueId(), ignored -> snapshot(fromClaim));
        TerritorySnapshot current = snapshot(toClaim);
        TerritoryTransition transition = TerritoryTransition.between(previous, current);
        if (!transition.changed()) {
            return;
        }
        currentTerritories.put(player.getUniqueId(), current);
        plugin.scoreboardService().refreshCurrentZone(player);
        if (transition.enteredNation()) {
            player.sendActionBar(Text.component(plugin.msgRaw("territory-enter-actionbar")
                    .replace("%nation%", current.coloredLabel())));
        } else if (transition.enteredWilderness()) {
            player.sendActionBar(Text.component(plugin.msgRaw("territory-wilderness-actionbar")));
        }
    }

    private TerritorySnapshot snapshot(ClaimKey claim) {
        Nation nation = townService.nationForClaim(claim);
        return nation == null ? TerritorySnapshot.wilderness() : TerritorySnapshot.nation(nation.id(), nation.name(), nation.color().legacyPrefix());
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
