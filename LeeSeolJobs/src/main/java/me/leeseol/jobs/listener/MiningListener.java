package me.leeseol.jobs.listener;

import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class MiningListener implements Listener {
    private final LeeSeolJobsPlugin plugin;

    public MiningListener(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.jobsConfig().antiPlaceAbuse()) {
            return;
        }
        long reward = plugin.jobsConfig().reward(JobType.MINING, event.getBlockPlaced().getType());
        if (reward > 0L) {
            plugin.blockHistoryService().markPlaced(event.getBlockPlaced(), plugin.jobsConfig().placedBlockRememberMillis());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        long reward = plugin.jobsConfig().reward(JobType.MINING, event.getBlock().getType());
        if (reward <= 0L) {
            return;
        }
        if (plugin.blockHistoryService().isPlayerPlaced(event.getBlock())) {
            plugin.blockHistoryService().remove(event.getBlock());
            return;
        }
        if (!plugin.cooldownService().checkAndSet(event.getPlayer(), JobType.MINING, plugin.jobsConfig().miningCooldownMillis())) {
            return;
        }
        plugin.rewardService().reward(event.getPlayer(), JobType.MINING, reward);
    }
}
