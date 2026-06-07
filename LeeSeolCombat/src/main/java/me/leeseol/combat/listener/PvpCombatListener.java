package me.leeseol.combat.listener;

import me.leeseol.combat.LeeSeolCombatPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class PvpCombatListener implements Listener {
    private final LeeSeolCombatPlugin plugin;

    public PvpCombatListener(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || isNpc(victim)) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim) || isNpc(attacker)) {
            return;
        }
        if (!plugin.combatConfig().isCombatWorld(victim) || !plugin.combatConfig().isCombatWorld(attacker)) {
            return;
        }
        plugin.combatTagManager().tag(victim, attacker);
    }

    private static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static boolean isNpc(Entity entity) {
        return entity.hasMetadata("NPC");
    }
}
