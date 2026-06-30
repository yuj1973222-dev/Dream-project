package me.leeseol.jobs.listener;

import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class FishingListener implements Listener {
    private final LeeSeolJobsPlugin plugin;

    public FishingListener(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!plugin.cooldownService().checkAndSet(event.getPlayer(), JobType.FISHING, plugin.jobsConfig().fishingCooldownMillis())) {
            return;
        }
        String key = isTreasure(event.getCaught()) ? "treasure" : "default";
        long reward = plugin.jobsConfig().fishingReward(key);
        plugin.rewardService().reward(event.getPlayer(), JobType.FISHING, reward);
    }

    private boolean isTreasure(org.bukkit.entity.Entity caught) {
        if (!(caught instanceof Item item)) {
            return false;
        }
        Material type = item.getItemStack().getType();
        return type == Material.BOW
            || type == Material.ENCHANTED_BOOK
            || type == Material.FISHING_ROD
            || type == Material.NAME_TAG
            || type == Material.NAUTILUS_SHELL
            || type == Material.SADDLE;
    }
}
