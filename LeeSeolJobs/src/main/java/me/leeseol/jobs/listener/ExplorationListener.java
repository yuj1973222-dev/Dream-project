package me.leeseol.jobs.listener;

import java.util.Locale;
import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import me.leeseol.jobs.model.PlayerJobStats;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class ExplorationListener implements Listener {
    private final LeeSeolJobsPlugin plugin;

    public ExplorationListener(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || sameBlock(event.getFrom(), to)) {
            return;
        }
        if (!plugin.jobsConfig().enabled()
                || !plugin.jobsConfig().isJobEnabled(JobType.EXPLORATION)
                || !plugin.jobsConfig().isWorldAllowed(to.getWorld())) {
            return;
        }

        String biomeKey = biomeKey(to);
        PlayerJobStats stats = plugin.jobsStore().getOrCreate(event.getPlayer());
        if (stats.hasDailyExplorationBiome(biomeKey)) {
            return;
        }
        if (!plugin.cooldownService().checkAndSet(event.getPlayer(), JobType.EXPLORATION, plugin.jobsConfig().explorationCooldownMillis())) {
            return;
        }
        if (plugin.rewardService().reward(event.getPlayer(), JobType.EXPLORATION, plugin.jobsConfig().explorationNewBiomeReward())) {
            stats.markDailyExplorationBiome(biomeKey);
            plugin.jobsStore().save();
        }
    }

    private static boolean sameBlock(Location from, Location to) {
        return from.getWorld() != null
            && from.getWorld().equals(to.getWorld())
            && from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ();
    }

    private static String biomeKey(Location location) {
        return location.getBlock().getBiome().toString().toLowerCase(Locale.ROOT);
    }
}
