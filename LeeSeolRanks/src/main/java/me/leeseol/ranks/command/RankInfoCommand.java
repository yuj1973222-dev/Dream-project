package me.leeseol.ranks.command;

import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import me.leeseol.ranks.service.RequirementLine;
import me.leeseol.ranks.service.RequirementResult;
import me.leeseol.ranks.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RankInfoCommand implements CommandExecutor {
    private final LeeSeolRanksPlugin plugin;
    private final RankUpCommand rankUpCommand;

    public RankInfoCommand(LeeSeolRanksPlugin plugin, RankUpCommand rankUpCommand) {
        this.plugin = plugin;
        this.rankUpCommand = rankUpCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("up") || args[0].equalsIgnoreCase("rankup"))) {
            return rankUpCommand.onCommand(sender, command, label, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("progress")) {
            return progress(sender);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("requirements")) {
            return requirements(sender);
        }
        if (args.length > 0) {
            RankData target = plugin.rankStore().findByName(args[0]);
            if (target == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                target = plugin.rankStore().getOrCreate(offlinePlayer);
            }
            plugin.message(sender, "info-other",
                    "%player%", target.name(),
                    "%rank%", target.rank().display(),
                    "%kills%", String.valueOf(target.kills()));
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-not-found");
            return true;
        }
        RankData data = plugin.rankStore().getOrCreate(player);
        Rank next = data.rank().next();
        if (next == null) {
            plugin.message(sender, "info-max", "%rank%", data.rank().display());
            return true;
        }
        plugin.message(sender, "info-self",
                "%rank%", data.rank().display(),
                "%kills%", String.valueOf(data.kills()),
                "%required%", String.valueOf(plugin.requiredKills(next)));
        return true;
    }

    private boolean progress(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }
        RankData data = plugin.rankStore().getOrCreate(player);
        Rank next = data.rank().next();
        if (next == null) {
            plugin.message(sender, "info-max", "%rank%", data.rank().display());
            return true;
        }
        RequirementResult result = plugin.rankRequirementService().result(player, data, next);
        Text.send(player, "&6[랭크] &f다음 랭크: &e" + next.display());
        for (RequirementLine line : result.lines()) {
            Text.send(player, line.format());
        }
        Text.send(player, result.met() ? "&a승급 가능합니다. /rankup" : "&c아직 승급 조건이 부족합니다.");
        return true;
    }

    private boolean requirements(CommandSender sender) {
        Text.send(sender, "&6[랭크] &f승급 조건");
        for (Rank rank : Rank.values()) {
            if (!rank.progressionRank() || rank == Rank.PLAYER) {
                continue;
            }
            Text.send(sender, "&e" + rank.display()
                + " &7- 킬 &f" + plugin.requiredKills(rank)
                + "&7, 돈 &f" + plugin.getConfig().getLong("rank-up.requirements." + rank.name() + ".money", 0L) + "원"
                + "&7, 플레이타임 &f" + plugin.getConfig().getInt("rank-up.requirements." + rank.name() + ".playtime-minutes", 0) + "분");
        }
        return true;
    }
}
