package me.leeseol.ranks.command;

import me.leeseol.ranks.LeeSeolRanksPlugin;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import me.leeseol.ranks.service.RequirementLine;
import me.leeseol.ranks.service.RequirementResult;
import me.leeseol.ranks.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RankUpCommand implements CommandExecutor {
    private final LeeSeolRanksPlugin plugin;

    public RankUpCommand(LeeSeolRanksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }

        RankData data = plugin.rankStore().getOrCreate(player);
        Rank current = data.rank();
        Rank next = current.next();
        if (next == null) {
            plugin.message(player, "info-max", "%rank%", current.display());
            return true;
        }

        RequirementResult result = plugin.rankRequirementService().result(player, data, next);
        if (!result.met()) {
            plugin.message(player, "rank-up-not-ready",
                    "%rank%", next.display(),
                    "%kills%", String.valueOf(data.kills()),
                    "%required%", String.valueOf(plugin.requiredKills(next)));
            for (RequirementLine line : result.lines()) {
                Text.send(player, line.format());
            }
            return true;
        }

        data.setRank(next);
        data.setKills(0);
        plugin.rankStore().save();
        plugin.permissionService().apply(player);
        plugin.permissionService().syncGlobalRank(player.getName(), next);
        plugin.message(player, "rank-up", "%rank%", next.display());
        tryProgressQuest(player, next);
        return true;
    }

    private void tryProgressQuest(Player player, Rank rank) {
        if (plugin.getServer().getPluginManager().getPlugin("LeeSeolQuest") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.leeseol.quest.api.LeeSeolQuestApi");
            api.getMethod("progress", Player.class, String.class, String.class, int.class)
                .invoke(null, player, "rank-up", rank.name(), 1);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // LeeSeolQuest is a soft integration.
        }
    }
}
