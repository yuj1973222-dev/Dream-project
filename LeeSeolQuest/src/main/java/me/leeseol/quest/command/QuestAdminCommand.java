package me.leeseol.quest.command;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.quest.LeeSeolQuestPlugin;
import me.leeseol.quest.model.ObjectiveType;
import me.leeseol.quest.service.QuestService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class QuestAdminCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolQuestPlugin plugin;
    private final QuestService questService;

    public QuestAdminCommand(LeeSeolQuestPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolquest.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.message(sender, "reloaded");
            }
            case "set" -> setStage(sender, label, args);
            case "advance" -> advance(sender, label, args);
            case "objective" -> objective(sender, label, args);
            case "reset" -> reset(sender, label, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "set", "advance", "objective", "reset"), args[0]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(new ArrayList<>(questService.quests().keySet()), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("objective")) {
            List<String> types = new ArrayList<>();
            for (ObjectiveType type : ObjectiveType.values()) {
                types.add(type.configName());
            }
            return filter(types, args[2]);
        }
        return List.of();
    }

    private void setStage(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " set <player> <questId> <stage>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.color("&c접속 중인 플레이어만 설정할 수 있습니다."));
            return;
        }
        int stage;
        try {
            stage = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(plugin.color("&cstage는 숫자여야 합니다."));
            return;
        }
        questService.setStage(target, args[2], stage);
        plugin.message(sender, "admin-set", "%player%", target.getName());
    }

    private void advance(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " advance <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.color("&c접속 중인 플레이어만 진행시킬 수 있습니다."));
            return;
        }
        questService.advance(target);
    }

    private void objective(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " objective <player> <type> [target]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        ObjectiveType type = ObjectiveType.fromConfig(args[2]);
        if (target == null || type == null) {
            sender.sendMessage(plugin.color("&c플레이어 또는 목표 타입이 올바르지 않습니다."));
            return;
        }
        String objectiveTarget = args.length >= 4 ? args[3] : null;
        questService.progressObjective(target, type, objectiveTarget, null, 1);
    }

    private void reset(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&c사용법: /" + label + " reset <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        questService.reset(target);
        plugin.message(sender, "admin-reset", "%player%", target.getName() == null ? args[1] : target.getName());
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.color("&b/" + label + " reload"));
        sender.sendMessage(plugin.color("&b/" + label + " set <player> <questId> <stage>"));
        sender.sendMessage(plugin.color("&b/" + label + " advance <player>"));
        sender.sendMessage(plugin.color("&b/" + label + " objective <player> <type> [target]"));
        sender.sendMessage(plugin.color("&b/" + label + " reset <player>"));
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).toList();
    }
}
