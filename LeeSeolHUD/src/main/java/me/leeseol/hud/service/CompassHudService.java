package me.leeseol.hud.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.model.CompassDirection;
import me.leeseol.hud.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class CompassHudService {
    private final LeeSeolHudPlugin plugin;
    private final PlayerHudStateService stateService;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, String> lastTitles = new HashMap<>();
    private final Map<UUID, Double> lastProgress = new HashMap<>();
    private BukkitTask task;

    public CompassHudService(LeeSeolHudPlugin plugin, PlayerHudStateService stateService) {
        this.plugin = plugin;
        this.stateService = stateService;
    }

    public void start() {
        stop();
        long interval = Math.max(1L, plugin.getConfig().getLong("compass.update-ticks", 10L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
        lastTitles.clear();
        lastProgress.clear();
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (!shouldShow(player)) {
            remove(player);
            return;
        }
        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), ignored -> createBar());
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
        UUID uuid = player.getUniqueId();
        String title = Text.color(format(player));
        double progress = Math.max(0.0D, Math.min(1.0D, plugin.getConfig().getDouble("compass.bossbar.progress", 1.0D)));

        String previousTitle = lastTitles.put(uuid, title);
        if (!title.equals(previousTitle)) {
            bar.setTitle(title);
        }
        Double previousProgress = lastProgress.put(uuid, progress);
        if (previousProgress == null || Math.abs(previousProgress - progress) > 0.0001D) {
            bar.setProgress(progress);
        }
        if (!bar.isVisible()) {
            bar.setVisible(true);
        }
    }

    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = bars.remove(uuid);
        lastTitles.remove(uuid);
        lastProgress.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private BossBar createBar() {
        return Bukkit.createBossBar(
            "",
            parseColor(plugin.getConfig().getString("compass.bossbar.color", "BLUE"), BarColor.BLUE),
            parseStyle(plugin.getConfig().getString("compass.bossbar.style", "SOLID"), BarStyle.SOLID)
        );
    }

    private boolean shouldShow(Player player) {
        return plugin.getConfig().getBoolean("settings.enabled", true)
            && plugin.getConfig().getBoolean("compass.enabled", true)
            && player.hasPermission("leeseolhud.use")
            && player.hasPermission("leeseolhud.compass")
            && stateService.compass(player)
            && isWorldEnabled(player.getWorld(), "compass.worlds");
    }

    private String format(Player player) {
        int yaw = roundedDegrees(player);
        String compass = CompassDirection.fromYaw(player.getLocation().getYaw()).name();
        String fallback = plugin.getConfig().getString("compass.fallback-format", "&b%compass% &7| &f%yaw%°")
            .replace("%compass%", compass)
            .replace("%yaw%", String.valueOf(yaw));
        if (!plugin.getConfig().getBoolean("compass.image.enabled", true)) {
            return fallback;
        }
        String glyph = glyph(player);
        String format = plugin.getConfig().getString("compass.format", "%glyph%");
        if (plugin.getConfig().getBoolean("compass.image.show-fallback-text", false)) {
            format = format + " " + fallback;
        }
        return format
            .replace("%glyph%", glyph)
            .replace("%compass%", compass)
            .replace("%yaw%", String.valueOf(yaw));
    }

    public String glyph(Player player) {
        int step = Math.max(1, plugin.getConfig().getInt("compass.image.step-degrees", 5));
        int degrees = roundedDegrees(player);
        int index = Math.floorDiv(degrees, step);
        int baseCodepoint = plugin.getConfig().getInt("compass.image.base-codepoint", 58176);
        int segments = Math.max(1, plugin.getConfig().getInt("compass.image.glyph-segments", 4));
        StringBuilder builder = new StringBuilder();
        for (int segment = 0; segment < segments; segment++) {
            builder.appendCodePoint(baseCodepoint + index * segments + segment);
        }
        return builder.toString();
    }

    public int roundedDegrees(Player player) {
        int step = Math.max(1, plugin.getConfig().getInt("compass.image.step-degrees", 5));
        float normalized = ((player.getLocation().getYaw() + 180.0F) % 360.0F + 360.0F) % 360.0F;
        int rounded = Math.round(normalized / step) * step;
        return rounded >= 360 ? 0 : rounded;
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
