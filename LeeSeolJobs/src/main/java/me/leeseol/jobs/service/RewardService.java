package me.leeseol.jobs.service;

import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import me.leeseol.jobs.model.PlayerJobStats;
import org.bukkit.entity.Player;

public final class RewardService {
    private final LeeSeolJobsPlugin plugin;

    public RewardService(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean reward(Player player, JobType type, long baseAmount) {
        if (!plugin.jobsConfig().enabled()
                || !plugin.jobsConfig().isJobEnabled(type)
                || !plugin.jobsConfig().isWorldAllowed(player.getWorld())
                || baseAmount <= 0L) {
            return false;
        }

        PlayerJobStats stats = plugin.jobsStore().getOrCreate(player);
        long amount = Math.max(1L, Math.round(baseAmount * rankMultiplier(player)));
        long limit = plugin.jobsConfig().dailyLimit(type);
        if (!player.hasPermission("leeseoljobs.bypass.daily-limit") && limit > 0L && stats.daily(type) + amount > limit) {
            plugin.message(player, "daily-limit", "%type%", type.displayName());
            return false;
        }

        if (!plugin.economyService().deposit(player, amount)) {
            return false;
        }

        stats.add(type, amount);
        plugin.jobsStore().save();
        plugin.message(player, "reward", "%type%", type.displayName(), "%amount%", String.valueOf(amount));
        tryProgressQuest(player, amount);
        if (plugin.jobsConfig().logRewards()) {
            plugin.getLogger().info("Rewarded " + player.getName() + " " + amount + " for " + type.configKey());
        }
        return true;
    }

    private double rankMultiplier(Player player) {
        String[] ranks = {"S", "A", "B", "C", "D"};
        for (String rank : ranks) {
            if (player.hasPermission("leeseolranks.rank." + rank.toLowerCase())) {
                return plugin.jobsConfig().rankMultiplier(rank);
            }
        }
        return plugin.jobsConfig().rankMultiplier("PLAYER");
    }

    private void tryProgressQuest(Player player, long amount) {
        if (plugin.getServer().getPluginManager().getPlugin("LeeSeolQuest") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.leeseol.quest.api.LeeSeolQuestApi");
            api.getMethod("progress", Player.class, String.class, String.class, int.class)
                .invoke(null, player, "earn-money", "jobs", Math.toIntExact(Math.min(Integer.MAX_VALUE, amount)));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // LeeSeolQuest is a soft integration.
        }
    }
}
