package me.leeseol.quest.command;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.gui.QuestGui;
import me.leeseol.quest.service.QuestService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class QuestCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;
    private final QuestGui questGui;

    public QuestCommand(LeeSeolQuestPlugin plugin, QuestService questService, QuestGui questGui) {
        this.plugin = plugin;
        this.questService = questService;
        this.questGui = questGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }
        if (!player.hasPermission("leeseolquest.use")) {
            plugin.message(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            questGui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sendHelp(player, label);
                    return true;
                }
                questService.startQuest(player, args[1], false);
            }
            case "progress" -> questService.sendProgress(player);
            case "abandon" -> questService.abandon(player);
            default -> sendHelp(player, label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("start", "progress", "abandon"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return filter(new ArrayList<>(questService.quests().keySet()), args[1]);
        }
        return List.of();
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(plugin.color("&b/" + label + " &7- 퀘스트 GUI"));
        player.sendMessage(plugin.color("&b/" + label + " start <id> &7- 퀘스트 시작"));
        player.sendMessage(plugin.color("&b/" + label + " progress &7- 진행도 확인"));
        player.sendMessage(plugin.color("&b/" + label + " abandon &7- 퀘스트 포기"));
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).toList();
    }
}
