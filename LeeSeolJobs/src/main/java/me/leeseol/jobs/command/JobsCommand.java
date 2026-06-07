package me.leeseol.jobs.command;

import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import me.leeseol.jobs.model.PlayerJobStats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class JobsCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolJobsPlugin plugin;

    public JobsCommand(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("leeseoljobs.use")) {
            plugin.message(player, "no-permission");
            return true;
        }
        sendStats(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("stats", "top") : List.of();
    }

    private void sendStats(Player player) {
        PlayerJobStats stats = plugin.jobsStore().getOrCreate(player);
        plugin.message(
            player,
            "stats",
            "%mining%", String.valueOf(stats.daily(JobType.MINING)),
            "%farming%", String.valueOf(stats.daily(JobType.FARMING)),
            "%fishing%", String.valueOf(stats.daily(JobType.FISHING))
        );
    }
}
