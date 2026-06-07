package me.leeseol.combat.service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.model.PvpRecord;
import me.leeseol.combat.storage.PvpPointStore;
import me.leeseol.combat.util.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PvpRewardService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LeeSeolCombatPlugin plugin;
    private final PvpPointStore store;
    private final Map<String, Long> lastRewardAt = new HashMap<>();
    private final NamespacedKey trophyKey;

    public PvpRewardService(LeeSeolCombatPlugin plugin, PvpPointStore store) {
        this.plugin = plugin;
        this.store = store;
        this.trophyKey = new NamespacedKey(plugin, "pvp_trophy");
    }

    public void handleDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (!eligible(killer, victim)) {
            return;
        }

        long now = System.currentTimeMillis();
        String cooldownKey = killer.getUniqueId() + ":" + victim.getUniqueId();
        long last = lastRewardAt.getOrDefault(cooldownKey, 0L);
        long cooldownMillis = plugin.combatConfig().pvpRepeatKillCooldownSeconds() * 1000L;
        if (cooldownMillis > 0L && last + cooldownMillis > now) {
            long left = Math.max(1L, (last + cooldownMillis - now + 999L) / 1000L);
            plugin.message(killer, "pvp-repeat-cooldown", "%seconds%", String.valueOf(left));
            return;
        }

        lastRewardAt.put(cooldownKey, now);
        PvpRecord record = store.getOrCreate(killer);
        record.setName(killer.getName());
        record.addKill();
        record.addPoints(plugin.combatConfig().pvpPointsPerKill());
        store.save();

        if (plugin.combatConfig().pvpTrophyEnabled()) {
            event.getDrops().add(trophy(killer, victim));
        }

        plugin.message(killer, "pvp-reward",
            "%victim%", victim.getName(),
            "%points%", String.valueOf(plugin.combatConfig().pvpPointsPerKill()),
            "%total%", String.valueOf(record.points()));
    }

    public PvpRecord record(OfflinePlayer player) {
        return store.getOrCreate(player);
    }

    public int recordCount() {
        return store.all().size();
    }

    public void save() {
        store.save();
    }

    private boolean eligible(Player killer, Player victim) {
        if (!plugin.combatConfig().pvpRewardsEnabled()) {
            return false;
        }
        if (killer == null || victim == null || killer.equals(victim)) {
            return false;
        }
        if (killer.hasMetadata("NPC") || victim.hasMetadata("NPC")) {
            return false;
        }
        if (!plugin.combatConfig().isPvpRewardWorld(killer) || !plugin.combatConfig().isPvpRewardWorld(victim)) {
            return false;
        }
        if (plugin.combatConfig().pvpIgnoreSameTownOrNation() && sameTownOrNation(killer, victim)) {
            plugin.message(killer, "pvp-same-affiliation");
            return false;
        }
        return true;
    }

    private ItemStack trophy(Player killer, Player victim) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(victim);
            meta = skullMeta;
        }
        if (meta != null) {
            meta.setDisplayName(Text.color(format(plugin.getConfig().getString("pvp-rewards.trophy.name", "&c%victim%의 증표"), killer, victim)));
            List<String> lore = plugin.getConfig().getStringList("pvp-rewards.trophy.lore").stream()
                .map(line -> Text.color(format(line, killer, victim)))
                .toList();
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(trophyKey, PersistentDataType.STRING, victim.getUniqueId().toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String format(String input, Player killer, Player victim) {
        return input
            .replace("%killer%", killer.getName())
            .replace("%victim%", victim.getName())
            .replace("%time%", TIME_FORMAT.format(LocalDateTime.now()));
    }

    private boolean sameTownOrNation(Player killer, Player victim) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")
                || !plugin.getServer().getPluginManager().isPluginEnabled("LeeSeolTown")) {
            return false;
        }
        String killerNation = placeholder(killer, "%leeseoltown_nation%");
        String victimNation = placeholder(victim, "%leeseoltown_nation%");
        if (!killerNation.isBlank() && killerNation.equalsIgnoreCase(victimNation)) {
            return true;
        }
        String killerTown = placeholder(killer, "%leeseoltown_town%");
        String victimTown = placeholder(victim, "%leeseoltown_town%");
        return !killerTown.isBlank() && killerTown.equalsIgnoreCase(victimTown);
    }

    private String placeholder(Player player, String placeholder) {
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, placeholder);
            return result == null ? "" : result.toString().trim();
        } catch (ReflectiveOperationException exception) {
            return "";
        }
    }
}
