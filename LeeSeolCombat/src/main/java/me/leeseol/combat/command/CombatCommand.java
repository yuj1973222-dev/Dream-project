package me.leeseol.combat.command;

import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.model.PvpRecord;
import me.leeseol.combat.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CombatCommand implements CommandExecutor {
    private final LeeSeolCombatPlugin plugin;

    public CombatCommand(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            Text.send(sender, "&7[&c전투&7] &f전투 태그: &e" + plugin.combatTagManager().activeCount()
                + " &7/ NPC: &e" + plugin.combatCloneManager().activeCloneCount()
                + " &7/ 관전 NPC: &e" + onOff(plugin.combatConfig().spectatorCloneEnabled())
                + " &7/ PVP 기록: &e" + plugin.pvpRewardService().recordCount());
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!hasAdmin(sender)) {
                return true;
            }
            plugin.reloadPluginConfig();
            plugin.message(sender, "reload");
            return true;
        }
        if (args[0].equalsIgnoreCase("force")) {
            if (!hasAdmin(sender)) {
                return true;
            }
            return forceCombat(sender, label, args);
        }
        if (args[0].equalsIgnoreCase("spectatorclone")) {
            if (!hasAdmin(sender)) {
                return true;
            }
            return spectatorClone(sender, label, args);
        }
        if (args[0].equalsIgnoreCase("pvp")) {
            return pvpStats(sender, label, args);
        }
        if (args[0].equalsIgnoreCase("pvppoints")) {
            if (!hasAdmin(sender)) {
                return true;
            }
            return pvpPoints(sender, label, args);
        }
        sendUsage(sender, label);
        return true;
    }

    private boolean forceCombat(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            Text.send(sender, "&c사용법: /" + label + " force <유저1> <유저2>");
            return true;
        }
        Player first = Bukkit.getPlayerExact(args[1]);
        Player second = Bukkit.getPlayerExact(args[2]);
        if (first == null || second == null) {
            plugin.message(sender, "player-not-found");
            return true;
        }
        if (first.equals(second)) {
            Text.send(sender, "&c서로 다른 유저 2명을 입력해야 합니다.");
            return true;
        }
        plugin.combatTagManager().forceTag(first, second);
        plugin.message(sender, "force-combat-admin", "%player1%", first.getName(), "%player2%", second.getName());
        return true;
    }

    private boolean spectatorClone(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            Text.send(sender, "&c사용법: /" + label + " spectatorclone <on|off>");
            return true;
        }
        if (args[1].equalsIgnoreCase("on")) {
            plugin.setSpectatorCloneEnabled(true);
            plugin.message(sender, "spectator-clone-on");
            return true;
        }
        if (args[1].equalsIgnoreCase("off")) {
            plugin.setSpectatorCloneEnabled(false);
            plugin.message(sender, "spectator-clone-off");
            return true;
        }
        Text.send(sender, "&c사용법: /" + label + " spectatorclone <on|off>");
        return true;
    }

    private boolean pvpStats(CommandSender sender, String label, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            Text.send(sender, "&c사용법: /" + label + " pvp <player>");
            return true;
        }
        PvpRecord record = plugin.pvpRewardService().record(target);
        Text.send(sender, "&7[&c전투&7] &f" + displayName(target)
            + " PVP 포인트: &e" + record.points()
            + " &7/ 처치: &e" + record.kills());
        return true;
    }

    private boolean pvpPoints(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            Text.send(sender, "&c사용법: /" + label + " pvppoints <set|add|take> <player> <amount>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        int amount;
        try {
            amount = Math.max(0, Integer.parseInt(args[3]));
        } catch (NumberFormatException exception) {
            Text.send(sender, "&c숫자를 입력해야 합니다.");
            return true;
        }
        PvpRecord record = plugin.pvpRewardService().record(target);
        switch (args[1].toLowerCase()) {
            case "set" -> record.setPoints(amount);
            case "add" -> record.addPoints(amount);
            case "take" -> record.setPoints(record.points() - amount);
            default -> {
                Text.send(sender, "&c사용법: /" + label + " pvppoints <set|add|take> <player> <amount>");
                return true;
            }
        }
        record.setName(displayName(target));
        plugin.pvpRewardService().save();
        Text.send(sender, "&7[&c전투&7] &f" + displayName(target) + " PVP 포인트: &e" + record.points());
        return true;
    }

    private boolean hasAdmin(CommandSender sender) {
        if (sender.hasPermission("leeseolcombat.admin")) {
            return true;
        }
        plugin.message(sender, "no-permission");
        return false;
    }

    private static void sendUsage(CommandSender sender, String label) {
        Text.send(sender, "&c사용법: /" + label + " status");
        Text.send(sender, "&c사용법: /" + label + " reload");
        Text.send(sender, "&c사용법: /" + label + " force <유저1> <유저2>");
        Text.send(sender, "&c사용법: /" + label + " spectatorclone <on|off>");
        Text.send(sender, "&c사용법: /" + label + " pvp [player]");
        Text.send(sender, "&c사용법: /" + label + " pvppoints <set|add|take> <player> <amount>");
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String displayName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }
}
