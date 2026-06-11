package me.leeseol.jobs;

import me.leeseol.jobs.command.JobsAdminCommand;
import me.leeseol.jobs.command.JobsCommand;
import me.leeseol.jobs.listener.ExplorationListener;
import me.leeseol.jobs.listener.FarmingListener;
import me.leeseol.jobs.listener.FishingListener;
import me.leeseol.jobs.listener.MiningListener;
import me.leeseol.jobs.service.BlockHistoryService;
import me.leeseol.jobs.service.CooldownService;
import me.leeseol.jobs.service.EconomyService;
import me.leeseol.jobs.service.JobsConfig;
import me.leeseol.jobs.service.RewardService;
import me.leeseol.jobs.storage.JobsStore;
import me.leeseol.jobs.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class LeeSeolJobsPlugin extends JavaPlugin {
    private final JobsConfig jobsConfig = new JobsConfig();
    private JobsStore jobsStore;
    private EconomyService economyService;
    private RewardService rewardService;
    private CooldownService cooldownService;
    private BlockHistoryService blockHistoryService;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        jobsStore = new JobsStore(this);
        economyService = new EconomyService(this);
        rewardService = new RewardService(this);
        cooldownService = new CooldownService();
        blockHistoryService = new BlockHistoryService();

        reloadAll();
        registerCommands();
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplorationListener(this), this);
        getLogger().info("LeeSeolJobs enabled. players=" + jobsStore.all().size());
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (jobsStore != null) {
            jobsStore.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        jobsConfig.load(getConfig());
        jobsStore.reload();
        economyService.reload();
        restartSaveTask();
    }

    private void registerCommands() {
        JobsCommand jobsCommand = new JobsCommand(this);
        getCommand("jobs").setExecutor(jobsCommand);
        getCommand("jobs").setTabCompleter(jobsCommand);

        JobsAdminCommand adminCommand = new JobsAdminCommand(this);
        getCommand("lsjobs").setExecutor(adminCommand);
        getCommand("lsjobs").setTabCompleter(adminCommand);
    }

    private void restartSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        long intervalTicks = Math.max(20L, jobsConfig.saveIntervalSeconds() * 20L);
        saveTask = getServer().getScheduler().runTaskTimer(this, () -> jobsStore.save(), intervalTicks, intervalTicks);
    }

    public JobsConfig jobsConfig() {
        return jobsConfig;
    }

    public JobsStore jobsStore() {
        return jobsStore;
    }

    public EconomyService economyService() {
        return economyService;
    }

    public RewardService rewardService() {
        return rewardService;
    }

    public CooldownService cooldownService() {
        return cooldownService;
    }

    public BlockHistoryService blockHistoryService() {
        return blockHistoryService;
    }

    public String color(String message) {
        return Text.color(message);
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String message = getConfig().getString("messages." + key, key);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        sender.sendMessage(Text.color(prefix + message));
    }
}
