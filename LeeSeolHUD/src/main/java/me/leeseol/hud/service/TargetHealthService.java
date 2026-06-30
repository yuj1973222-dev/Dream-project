package me.leeseol.hud.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TargetHealthService {
    private final LeeSeolHudPlugin plugin;
    private final PlayerHudStateService stateService;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Long> expiresAt = new HashMap<>();
    private BukkitTask cleanupTask;

    public TargetHealthService(LeeSeolHudPlugin plugin, PlayerHudStateService stateService) {
        this.plugin = plugin;
        this.stateService = stateService;
    }

    public void start() {
        stop();
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 20L, 20L);
    }

    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        expiresAt.clear();
    }

    public void show(Player player, LivingEntity target) {
        if (!shouldShow(player, target)) {
            return;
        }
        double maxHealth = maxHealth(target);
        double health = Math.max(0.0D, Math.min(maxHealth, target.getHealth()));
        double ratio = maxHealth <= 0.0D ? 0.0D : health / maxHealth;
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored -> createBar());
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        bar.setColor(color(ratio));
        bar.setTitle(Text.color(format(target, health, maxHealth)));
        bar.setProgress(Math.max(0.0D, Math.min(1.0D, ratio)));
        bar.setVisible(true);
        long seconds = Math.max(1L, plugin.getConfig().getLong("target-health.show-seconds", 5L));
        expiresAt.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void remove(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        expiresAt.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (UUID uuid : new ArrayList<>(expiresAt.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || expiresAt.getOrDefault(uuid, 0L) <= now) {
                if (player != null) {
                    remove(player);
                } else {
                    BossBar bar = bars.remove(uuid);
                    expiresAt.remove(uuid);
                    if (bar != null) {
                        bar.removeAll();
                    }
                }
            }
        }
    }

    private BossBar createBar() {
        return Bukkit.createBossBar(
            "",
            BarColor.GREEN,
            parseStyle(plugin.getConfig().getString("target-health.bossbar.style", "SEGMENTED_10"), BarStyle.SEGMENTED_10)
        );
    }

    private boolean shouldShow(Player player, LivingEntity target) {
        return plugin.getConfig().getBoolean("settings.enabled", true)
            && plugin.getConfig().getBoolean("target-health.enabled", true)
            && player.hasPermission("leeseolhud.use")
            && player.hasPermission("leeseolhud.target-health")
            && stateService.targetHealth(player)
            && isWorldEnabled(player.getWorld(), "target-health.worlds")
            && isValidTarget(target);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || target.isDead() || !target.isValid()) {
            return false;
        }
        if (target instanceof ArmorStand) {
            return false;
        }
        return !plugin.getConfig().getBoolean("target-health.ignore-citizens-npcs", true) || !target.hasMetadata("NPC");
    }

    private String format(LivingEntity target, double health, double maxHealth) {
        String name = target.getName() == null || target.getName().isBlank() ? target.getType().name() : target.getName();
        return plugin.getConfig().getString("target-health.format", "&c%target% &f%health%/%max_health%")
            .replace("%target%", name)
            .replace("%health%", String.valueOf(Math.round(health)))
            .replace("%max_health%", String.valueOf(Math.round(maxHealth)));
    }

    private BarColor color(double ratio) {
        String key = ratio >= 0.7D ? "color-high" : ratio >= 0.3D ? "color-mid" : "color-low";
        BarColor fallback = ratio >= 0.7D ? BarColor.GREEN : ratio >= 0.3D ? BarColor.YELLOW : BarColor.RED;
        return parseColor(plugin.getConfig().getString("target-health.bossbar." + key, fallback.name()), fallback);
    }

    private double maxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? Math.max(1.0D, entity.getHealth()) : Math.max(1.0D, attribute.getValue());
    }

    private boolean isWorldEnabled(World world, String path) {
        if (world == null) {
            return false;
        }
        var worlds = plugin.getConfig().getStringList(path);
        return worlds.isEmpty() || worlds.stream().anyMatch(value -> world.getName().equalsIgnoreCase(value));
    }

    private static BarColor parseColor(String value, BarColor fallback) {
        try {
            return BarColor.valueOf((value == null ? "" : value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static BarStyle parseStyle(String value, BarStyle fallback) {
        try {
            return BarStyle.valueOf((value == null ? "" : value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
