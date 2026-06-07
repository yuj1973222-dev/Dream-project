package me.leeseol.ranks.command;

import java.util.Locale;
import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import me.leeseol.ranks.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RankAdminCommand implements CommandExecutor {
    private final LeeSeolRanksPlugin plugin;

    public RankAdminCommand(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolranks.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.message(sender, "status", "%players%", String.valueOf(plugin.rankStore().all().size()));
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("reload")) {
            plugin.reloadPluginConfig();
            plugin.message(sender, "reloaded");
            return true;
        }
        if (subCommand.equals("set")) {
            return setRank(sender, label, args);
        }
        if (subCommand.equals("dev")) {
            return setDev(sender, label, args);
        }
        sendUsage(sender, label);
        return true;
    }

    private boolean setRank(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            Text.send(sender, "&c사용법: /" + label + " set <유저> <PLAYER|D|C|B|A|S|ADMIN|DEV>");
            return true;
        }
        Rank rank = Rank.parse(args[2]);
        if (rank == null) {
            plugin.message(sender, "invalid-rank");
            return true;
        }

        RankData data = targetData(args[1]);
        setRankData(data, rank);
        plugin.message(sender, "set-rank", "%player%", data.name(), "%rank%", rank.display());
        return true;
    }

    private boolean setDev(CommandSender sender, String label, String[] args) {
        if (args.length < 3 || (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off"))) {
            Text.send(sender, "&c사용법: /" + label + " dev <유저> <on|off>");
            return true;
        }
        RankData data = targetData(args[1]);
        Rank rank = args[2].equalsIgnoreCase("on") ? Rank.DEV : Rank.PLAYER;
        setRankData(data, rank);
        plugin.message(sender, args[2].equalsIgnoreCase("on") ? "dev-on" : "dev-off", "%player%", data.name());
        return true;
    }

    private void setRankData(RankData data, Rank rank) {
        data.setRank(rank);
        data.setKills(0);
        plugin.rankStore().save();
        plugin.permissionService().syncGlobalRank(data.name(), rank);
        applyIfOnline(data.name());
    }

    private RankData targetData(String name) {
        RankData existing = plugin.rankStore().findByName(name);
        if (existing != null) {
            return existing;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        RankData data = plugin.rankStore().getOrCreate(offlinePlayer);
        data.setName(name);
        return data;
    }

    private void applyIfOnline(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) {
            plugin.permissionService().apply(player);
        }
    }

    private static void sendUsage(CommandSender sender, String label) {
        Text.send(sender, "&c사용법: /" + label + " status");
        Text.send(sender, "&c사용법: /" + label + " reload");
        Text.send(sender, "&c사용법: /" + label + " set <유저> <PLAYER|D|C|B|A|S|ADMIN|DEV>");
        Text.send(sender, "&c사용법: /" + label + " dev <유저> <on|off>");
    }
}
