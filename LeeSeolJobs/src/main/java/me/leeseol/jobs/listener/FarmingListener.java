package me.leeseol.jobs.listener;

import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class FarmingListener implements Listener {
    private final LeeSeolJobsPlugin plugin;

    public FarmingListener(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        long reward = plugin.jobsConfig().reward(JobType.FARMING, event.getBlock().getType());
        if (reward <= 0L) {
            return;
        }
        if (plugin.jobsConfig().farmingRequireFullyGrown()) {
            if (!(event.getBlock().getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
                return;
            }
        }
        plugin.rewardService().reward(event.getPlayer(), JobType.FARMING, reward);
    }
}
