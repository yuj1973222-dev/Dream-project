package me.leeseol.economy.npc;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NpcCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolEconomyPlugin plugin;
    private final NpcManager npcManager;

    public NpcCommand(LeeSeolEconomyPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(sender, args);
            case "skin" -> skin(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "/wonnpc create <id> <shopId> [skin:<playerName>] [displayName]");
            return;
        }

        String skinName = null;
        int nameStart = 3;
        if (args.length >= 4 && args[3].toLowerCase().startsWith("skin:")) {
            skinName = args[3].substring("skin:".length());
            nameStart = 4;
        }
        String displayName = args.length > nameStart ? String.join(" ", Arrays.copyOfRange(args, nameStart, args.length)) : "&e상점";

        if (!npcManager.create(player, args[1], args[2], displayName, skinName)) {
            player.sendMessage(plugin.msg("shop-not-found").replace("%shop%", args[2]));
            return;
        }
        player.sendMessage(plugin.msg("npc-created").replace("%id%", args[1]));
    }

    private void skin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "/wonnpc skin <id> <playerName|none>");
            return;
        }
        if (!npcManager.setSkin(args[1], args[2])) {
            sender.sendMessage(plugin.msg("npc-not-found").replace("%id%", args[1]));
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "NPC skin updated: " + args[1]);
    }

    private void remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "/wonnpc remove <id>");
            return;
        }
        if (!npcManager.remove(args[1])) {
            sender.sendMessage(plugin.msg("npc-not-found").replace("%id%", args[1]));
            return;
        }
        sender.sendMessage(plugin.msg("npc-removed").replace("%id%", args[1]));
    }

    private void list(CommandSender sender) {
        List<String> ids = new ArrayList<>();
        for (String id : npcManager.npcIds()) {
            ids.add(id);
        }
        sender.sendMessage(ChatColor.YELLOW + "NPCs: " + ChatColor.WHITE + String.join(", ", ids));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/wonnpc create <id> <shopId> [skin:<playerName>] [displayName]");
        sender.sendMessage(ChatColor.YELLOW + "/wonnpc skin <id> <playerName|none>");
        sender.sendMessage(ChatColor.YELLOW + "/wonnpc remove <id>");
        sender.sendMessage(ChatColor.YELLOW + "/wonnpc list");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return List.of("create", "skin", "remove", "list").stream()
                .filter(option -> option.startsWith(args[0].toLowerCase()))
                .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("skin") || args[0].equalsIgnoreCase("remove"))) {
            List<String> ids = new ArrayList<>();
            for (String id : npcManager.npcIds()) {
                if (id.startsWith(args[1].toLowerCase())) {
                    ids.add(id);
                }
            }
            return ids;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            List<String> shops = new ArrayList<>();
            for (String id : plugin.shopManager().shopIds()) {
                if (id.startsWith(args[2].toLowerCase())) {
                    shops.add(id);
                }
            }
            return shops;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return List.of("skin:lee_seol");
        }
        return Collections.emptyList();
    }
}
