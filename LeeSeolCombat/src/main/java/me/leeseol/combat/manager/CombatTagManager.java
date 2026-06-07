package me.leeseol.combat.manager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.config.CombatConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CombatTagManager {
    private final LeeSeolCombatPlugin plugin;
    private final Map<UUID, Long> taggedUntil = new HashMap<>();

    public CombatTagManager(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public void tag(Player first, Player second) {
        CombatConfig config = plugin.combatConfig();
        if (!config.isCombatTagEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long until = now + config.combatDurationSeconds() * 1000L;
        tagOne(first, until, config, false);
        tagOne(second, until, config, false);
    }

    public void forceTag(Player first, Player second) {
        CombatConfig config = plugin.combatConfig();
        long now = System.currentTimeMillis();
        long until = now + config.combatDurationSeconds() * 1000L;
        tagOne(first, until, config, true);
        tagOne(second, until, config, true);
    }

    public boolean isTagged(UUID uuid) {
        Long until = taggedUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public void clear(UUID uuid) {
        taggedUntil.remove(uuid);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = taggedUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() > now) {
                continue;
            }
            iterator.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                plugin.message(player, "combat-ended");
            }
        }
    }

    public int activeCount() {
        return taggedUntil.size();
    }

    private void tagOne(Player player, long until, CombatConfig config, boolean forced) {
        if (player == null || (!forced && player.hasPermission("leeseolcombat.bypass"))) {
            return;
        }
        boolean wasTagged = isTagged(player.getUniqueId());
        taggedUntil.put(player.getUniqueId(), until);
        if (!wasTagged) {
            plugin.message(player, "combat-start", "%seconds%", String.valueOf(config.combatDurationSeconds()));
        } else if (config.notifyOnRefresh()) {
            plugin.message(player, "combat-refresh", "%seconds%", String.valueOf(config.combatDurationSeconds()));
        }
    }
}
