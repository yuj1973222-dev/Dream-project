package me.leeseol.town.listener;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.service.TownService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public final class NationRuleListener implements Listener {
    private final LeeSeolTownPlugin plugin;
    private final TownService townService;

    public NationRuleListener(LeeSeolTownPlugin plugin, TownService townService) {
        this.plugin = plugin;
        this.townService = townService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event)) {
            return;
        }
        Player player = event.getPlayer();
        ClaimKey toClaim = ClaimKey.from(event.getTo());
        if (townService.shouldBlockWarEntry(player, toClaim)) {
            event.setTo(event.getFrom());
            player.sendMessage(plugin.msg("war-entry-blocked"));
            return;
        }
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
