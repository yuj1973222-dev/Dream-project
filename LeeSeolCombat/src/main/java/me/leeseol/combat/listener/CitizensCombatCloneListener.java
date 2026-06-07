package me.leeseol.combat.listener;

import me.leeseol.combat.LeeSeolCombatPlugin;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class CitizensCombatCloneListener implements Listener {
    private final LeeSeolCombatPlugin plugin;

    public CitizensCombatCloneListener(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcDamage(NPCDamageByEntityEvent event) {
        EntityDamageByEntityEvent baseEvent = event.getEvent() instanceof EntityDamageByEntityEvent damageEvent
                ? damageEvent
                : null;
        Player attacker = resolvePlayer(event.getDamager());
        double damage = baseEvent == null ? event.getDamage() : baseEvent.getFinalDamage();
        boolean handled = plugin.combatCloneManager().damageClone(event.getNPC(), attacker, damage);
        if (handled) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHitboxDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayer(event.getDamager());
        double damage = Math.max(1.0D, event.getFinalDamage());
        boolean handled = plugin.combatCloneManager().damageHitbox(event.getEntity(), attacker, damage);
        if (handled) {
            event.setCancelled(true);
        }
    }

    private static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player && !player.hasMetadata("NPC")) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player && !player.hasMetadata("NPC")) {
                return player;
            }
        }
        return null;
    }
}
