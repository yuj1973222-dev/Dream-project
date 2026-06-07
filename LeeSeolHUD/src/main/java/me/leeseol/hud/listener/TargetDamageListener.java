package me.leeseol.hud.listener;

import me.leeseol.hud.LeeSeolHudPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class TargetDamageListener implements Listener {
    private final LeeSeolHudPlugin plugin;

    public TargetDamageListener(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = attacker(event.getDamager());
        if (attacker == null || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> plugin.targetHealthService().show(attacker, target));
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
