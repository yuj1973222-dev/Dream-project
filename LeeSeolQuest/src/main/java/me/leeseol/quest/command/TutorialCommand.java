package me.leeseol.quest.command;

import java.util.List;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.model.Quest;
import me.leeseol.quest.service.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class TutorialCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;

    public TutorialCommand(LeeSeolQuestPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("start")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used in game.");
                return true;
            }
            Quest tutorial = firstTutorial();
            if (tutorial == null) {
                plugin.message(player, "unknown-quest", "%quest%", "tutorial");
                return true;
            }
            questService.startQuest(player, tutorial.id(), false);
            return true;
        }

        if (args[0].equalsIgnoreCase("skip")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used in game.");
                return true;
            }
            questService.skipTutorial(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("leeseolquest.admin")) {
                plugin.message(sender, "no-permission");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.color("&c사용법: /" + label + " reset <player>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            questService.reset(target);
            plugin.message(sender, "admin-reset", "%player%", target.getName() == null ? args[1] : target.getName());
            return true;
        }

        sender.sendMessage(plugin.color("&b/" + label + " start"));
        sender.sendMessage(plugin.color("&b/" + label + " skip"));
        sender.sendMessage(plugin.color("&b/" + label + " reset <player>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("start", "skip", "reset"), args[0]);
        }
        return List.of();
    }

    private Quest firstTutorial() {
        for (Quest quest : questService.quests().values()) {
            if (quest.autoStart()) {
                return quest;
            }
        }
        return null;
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).toList();
    }
}
