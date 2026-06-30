package me.leeseol.economy.command;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuBridgeCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SELF_ACTIONS = List.of("open", "move", "shop", "auction", "dungeon");
    private static final List<String> TARGET_ACTIONS = List.of("open-player", "move-player", "shop-player", "auction-player", "dungeon-player");
    private static final List<String> SERVER_NAMES = List.of("lobby", "survival", "newworld");

    private final LeeSeolEconomyPlugin plugin;

    public MenuBridgeCommand(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "open" -> handleOpen(sender);
            case "move" -> handleMove(sender, args);
            case "shop" -> handleShop(sender, args);
            case "auction" -> handlePlayerCommand(sender, "auction");
            case "dungeon" -> handlePlayerCommand(sender, "dungeon enter");
            case "open-player" -> handleTargetOpen(sender, args);
            case "move-player" -> handleTargetMove(sender, args);
            case "shop-player" -> handleTargetShop(sender, args);
            case "auction-player" -> handleTargetCommand(sender, args, "auction");
            case "dungeon-player" -> handleTargetCommand(sender, args, "dungeon enter");
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleOpen(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        plugin.serverMenuManager().open(player);
    }

    private void handleMove(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§e/leeseolmenu move <server>");
            return;
        }
        movePlayer(player, args[1]);
    }

    private void handleShop(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        openShop(player, args.length >= 2 ? args[1] : null);
    }

    private void handlePlayerCommand(CommandSender sender, String command) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!plugin.serverMenuManager().canUse(player, true)) {
            return;
        }
        player.performCommand(command);
    }

    private void handleTargetOpen(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/leeseolmenu open-player <player>");
            return;
        }
        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }
        plugin.serverMenuManager().open(target);
    }

    private void handleTargetMove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§e/leeseolmenu move-player <player> <server>");
            return;
        }
        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }
        movePlayer(target, args[2]);
    }

    private void handleTargetShop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/leeseolmenu shop-player <player> [shop]");
            return;
        }
        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }
        openShop(target, args.length >= 3 ? args[2] : null);
    }

    private void handleTargetCommand(CommandSender sender, String[] args, String command) {
        if (args.length < 2) {
            sender.sendMessage("§e/leeseolmenu " + args[0].toLowerCase() + " <player>");
            return;
        }
        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }
        if (!plugin.serverMenuManager().canUse(target, true)) {
            return;
        }
        target.performCommand(command);
    }

    private void movePlayer(Player player, String server) {
        if (!plugin.serverMenuManager().canUse(player, true)) {
            return;
        }
        if (server == null || server.isBlank()) {
            player.sendMessage("§c이동할 서버 이름이 비어 있습니다.");
            return;
        }
        player.closeInventory();
        player.sendMessage(plugin.msg("server-moving").replace("%server%", server));
        plugin.sendPlayerToServer(player, server);
    }

    private void openShop(Player player, String shopId) {
        if (!plugin.serverMenuManager().canUse(player, true)) {
            return;
        }
        if (!player.hasPermission("leeseoleconomy.shop")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        plugin.shopManager().open(player, shopId);
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (sender instanceof Player player && !player.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return null;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return null;
        }
        return target;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(plugin.msgRaw("player-only"));
        return null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e/leeseolmenu open");
        sender.sendMessage("§e/leeseolmenu move <server>");
        sender.sendMessage("§e/leeseolmenu shop [shop]");
        sender.sendMessage("§e/leeseolmenu auction");
        sender.sendMessage("§e/leeseolmenu dungeon");
        if (sender.hasPermission("leeseoleconomy.admin") || !(sender instanceof Player)) {
            sender.sendMessage("§e/leeseolmenu open-player <player>");
            sender.sendMessage("§e/leeseolmenu move-player <player> <server>");
            sender.sendMessage("§e/leeseolmenu shop-player <player> [shop]");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> actions = new ArrayList<>(SELF_ACTIONS);
            if (sender.hasPermission("leeseoleconomy.admin") || !(sender instanceof Player)) {
                actions.addAll(TARGET_ACTIONS);
            }
            return filter(actions, args[0]);
        }

        String action = args[0].toLowerCase();
        if (args.length == 2) {
            if (action.equals("move")) {
                return filter(SERVER_NAMES, args[1]);
            }
            if (action.equals("shop")) {
                return shopIds(args[1]);
            }
            if (TARGET_ACTIONS.contains(action)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
            }
        }

        if (args.length == 3) {
            if (action.equals("move-player")) {
                return filter(SERVER_NAMES, args[2]);
            }
            if (action.equals("shop-player")) {
                return shopIds(args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> shopIds(String prefix) {
        List<String> ids = new ArrayList<>();
        for (String id : plugin.shopManager().shopIds()) {
            ids.add(id);
        }
        return filter(ids, prefix);
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase();
        return values.stream()
            .filter(value -> value.toLowerCase().startsWith(lower))
            .toList();
    }
}
