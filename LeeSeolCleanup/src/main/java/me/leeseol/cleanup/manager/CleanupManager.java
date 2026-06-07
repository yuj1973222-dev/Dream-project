package me.leeseol.cleanup.manager;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.cleanup.LeeSeolCleanupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

public final class CleanupManager {
    private final LeeSeolCleanupPlugin plugin;
    private BukkitTask task;
    private boolean enabled;
    private int intervalMinutes;
    private List<String> worlds;
    private boolean broadcast;
    private int minimumAgeTicks;
    private long nextCleanupAtMillis;

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

        if (enabled) {
            long intervalTicks = intervalMinutes * 60L * 20L;
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupScheduled, intervalTicks, intervalTicks);
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
}
