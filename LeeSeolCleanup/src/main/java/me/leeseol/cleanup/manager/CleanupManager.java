package me.leeseol.cleanup.manager;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.cleanup.LeeSeolCleanupPlugin;
import me.leeseol.cleanup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class CleanupManager {
    private final LeeSeolCleanupPlugin plugin;
    private BukkitTask task;
    private BukkitTask countdownTask;
    private boolean enabled;
    private int intervalMinutes;
    private List<String> worlds;
    private boolean broadcast;
    private int minimumAgeTicks;
    private long nextCleanupAtMillis;
    private int warningSeconds;
    private String warningActionBar;
    private Sound warningSound;
    private boolean warningSoundEnabled;
    private boolean warningSoundPlayed;

    public CleanupManager(LeeSeolCleanupPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        stop();
        enabled = plugin.getConfig().getBoolean("cleanup.enabled", true);
        intervalMinutes = Math.max(1, plugin.getConfig().getInt("cleanup.interval-minutes", 10));
        worlds = new ArrayList<>(plugin.getConfig().getStringList("cleanup.worlds"));
        broadcast = plugin.getConfig().getBoolean("cleanup.broadcast", true);
        int minimumAgeSeconds = Math.max(0, plugin.getConfig().getInt("cleanup.minimum-age-seconds", 0));
        minimumAgeTicks = minimumAgeSeconds * 20;
        warningSeconds = Math.max(1, plugin.getConfig().getInt("cleanup.warning.seconds", 10));
        warningActionBar = plugin.getConfig().getString("cleanup.warning.actionbar", "&c곧 아이템이 삭제됩니다: &e%seconds%초");
        warningSoundEnabled = plugin.getConfig().getBoolean("cleanup.warning.sound.enabled", true);
        warningSound = parseSound(plugin.getConfig().getString("cleanup.warning.sound.name", "BLOCK_NOTE_BLOCK_PLING"));
        warningSoundPlayed = false;

        if (enabled) {
            long intervalTicks = intervalMinutes * 60L * 20L;
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupScheduled, intervalTicks, intervalTicks);
            countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCountdown, 20L, 20L);
            nextCleanupAtMillis = System.currentTimeMillis() + intervalMinutes * 60L * 1000L;
        } else {
            nextCleanupAtMillis = 0L;
        }
        plugin.getLogger().info("LeeSeolCleanup loaded. enabled=" + enabled
                + ", intervalMinutes=" + intervalMinutes + ", worlds=" + worlds.size());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public int cleanupNow() {
        int removed = 0;
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Cleanup world is not loaded: " + worldName);
                continue;
            }
            removed += cleanupWorld(world);
        }
        return removed;
    }

    public boolean enabled() {
        return enabled;
    }

    public int intervalMinutes() {
        return intervalMinutes;
    }

    public String worldSummary() {
        return String.join(", ", worlds);
    }

    public long secondsUntilNextCleanup() {
        if (!enabled || nextCleanupAtMillis <= 0L) {
            return -1L;
        }
        return Math.max(0L, (nextCleanupAtMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    public String nextCleanupText() {
        long seconds = secondsUntilNextCleanup();
        if (seconds < 0L) {
            return "비활성화";
        }
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return String.format("%02d분 %02d초", minutes, remainingSeconds);
    }

    private void cleanupScheduled() {
        if (!enabled) {
            return;
        }
        int removed = cleanupNow();
        if (broadcast && removed > 0) {
            plugin.broadcastToCleanupWorlds("cleaned", "%count%", String.valueOf(removed));
        }
        nextCleanupAtMillis = System.currentTimeMillis() + intervalMinutes * 60L * 1000L;
        warningSoundPlayed = false;
    }

    private void tickCountdown() {
        long seconds = secondsUntilNextCleanup();
        if (!CleanupCountdown.shouldShow(seconds, warningSeconds)) {
            if (seconds > warningSeconds) {
                warningSoundPlayed = false;
            }
            return;
        }

        boolean playSound = warningSoundEnabled
                && warningSound != null
                && CleanupCountdown.shouldPlayStartSound(seconds, warningSeconds, warningSoundPlayed);
        String message = CleanupCountdown.render(warningActionBar, seconds);
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                player.sendActionBar(Text.component(message));
                if (playSound) {
                    player.playSound(player.getLocation(), warningSound, 1.0F, 1.0F);
                }
            }
        }
        if (playSound) {
            warningSoundPlayed = true;
        }
    }

    private int cleanupWorld(World world) {
        int removed = 0;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            if (item.isDead() || item.getTicksLived() < minimumAgeTicks) {
                continue;
            }
            item.remove();
            removed++;
        }
        return removed;
    }

    private Sound parseSound(String name) {
        if (name == null || name.isBlank()) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Invalid cleanup warning sound: " + name);
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }
}
