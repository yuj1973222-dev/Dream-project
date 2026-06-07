package me.leeseol.jobs.command;

import java.util.List;
import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import me.leeseol.jobs.model.PlayerJobStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class JobsAdminCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolJobsPlugin plugin;

    public JobsAdminCommand(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseoljobs.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.message(sender, "status", "%players%", String.valueOf(plugin.jobsStore().all().size()));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.message(sender, "reloaded");
            return true;
        }
        if (args[0].equalsIgnoreCase("stats")) {
            return stats(sender, label, args);
        }
        if (args[0].equalsIgnoreCase("reset")) {
            return reset(sender, label, args);
        }
        sendHelp(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("status", "reload", "stats", "reset") : List.of();
    }

    private boolean stats(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " stats <player>"));
            return true;
        }
        PlayerJobStats stats = plugin.jobsStore().findByName(args[1]);
        if (stats == null) {
            plugin.message(sender, "player-not-found");
            return true;
        }
        sender.sendMessage(plugin.color("&a" + stats.name() + " Jobs"));
        sender.sendMessage(plugin.color("&7오늘: 광질 " + stats.daily(JobType.MINING)
            + " / 농사 " + stats.daily(JobType.FARMING)
            + " / 낚시 " + stats.daily(JobType.FISHING)));
        sender.sendMessage(plugin.color("&7누적: 광질 " + stats.total(JobType.MINING)
            + " / 농사 " + stats.total(JobType.FARMING)
            + " / 낚시 " + stats.total(JobType.FISHING)));
        return true;
    }

    private boolean reset(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " reset <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.jobsStore().reset(target);
        plugin.jobsStore().save();
        plugin.message(sender, "reset", "%player%", target.getName() == null ? args[1] : target.getName());
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.color("&a/" + label + " status"));
        sender.sendMessage(plugin.color("&a/" + label + " reload"));
        sender.sendMessage(plugin.color("&a/" + label + " stats <player>"));
        sender.sendMessage(plugin.color("&a/" + label + " reset <player>"));
    }
}
