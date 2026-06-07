package me.leeseol.auction.command;

import me.leeseol.auction.LeeSeolAuctionPlugin;
import me.leeseol.auction.gui.AuctionGui;
import me.leeseol.auction.service.AuctionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolAuctionPlugin plugin;
    private final AuctionService auctionService;
    private final AuctionGui auctionGui;

    public AuctionCommand(LeeSeolAuctionPlugin plugin, AuctionService auctionService, AuctionGui auctionGui) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.auctionGui = auctionGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.auctionEnabled()) {
            sender.sendMessage(plugin.msg("disabled"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (!player.hasPermission("leeseolauction.use")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (!plugin.auctionAvailable(player)) {
            player.sendMessage(plugin.msg("world-disabled"));
            return true;
        }

        if (args.length == 0) {
            auctionGui.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "submit" -> auctionGui.openSubmit(player);
            case "admin" -> openAdmin(player);
            case "open" -> openAuction(player, args);
            case "increment", "setincrement" -> setIncrement(player, args);
            case "end" -> auctionService.endAuction(player);
            case "reload" -> reload(player);
            default -> auctionGui.openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!plugin.auctionEnabled()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options = sender.hasPermission("leeseolauction.admin")
                    ? List.of("submit", "admin", "open", "increment", "end", "reload")
                    : List.of("submit");
            return options.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && sender.hasPermission("leeseolauction.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("open")) {
                return auctionService.submittedLots().stream()
                        .map(lot -> String.valueOf(lot.id()))
                        .filter(value -> value.startsWith(args[1]))
                        .toList();
            }
            if (sub.equals("increment") || sub.equals("setincrement")) {
                return List.of("100", "500", "1000", "5000").stream()
                        .filter(value -> value.startsWith(args[1]))
                        .toList();
            }
        }
        if ((args.length == 3 || args.length == 4) && sender.hasPermission("leeseolauction.admin") && args[0].equalsIgnoreCase("open")) {
            return List.of("1000", "5000", "10000", "50000").stream()
                    .filter(value -> value.startsWith(args[args.length - 1]))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void openAdmin(Player player) {
        if (!player.hasPermission("leeseolauction.admin")) {
            player.sendMessage(plugin.msg("admin-only"));
            return;
        }
        auctionGui.openAdminSelect(player, 0);
    }

    private void openAuction(Player player, String[] args) {
        if (!player.hasPermission("leeseolauction.admin")) {
            player.sendMessage(plugin.msg("admin-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.msg("usage-open"));
            return;
        }

        Long lotId = parseLong(player, args[1]);
        if (lotId == null) {
            return;
        }

        long startingBid = plugin.defaultStartingBid();
        long bidIncrement = plugin.bidIncrement();
        if (args.length >= 3) {
            Long parsed = parseLong(player, args[2]);
            if (parsed == null) {
                return;
            }
            startingBid = parsed;
        }
        if (args.length >= 4) {
            Long parsed = parseLong(player, args[3]);
            if (parsed == null) {
                return;
            }
            bidIncrement = parsed;
        }

        auctionService.openAuction(player, lotId, startingBid, bidIncrement);
    }

    private void setIncrement(Player player, String[] args) {
        if (!player.hasPermission("leeseolauction.admin")) {
            player.sendMessage(plugin.msg("admin-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.msg("usage-increment"));
            return;
        }
        Long amount = parseLong(player, args[1]);
        if (amount == null) {
            return;
        }
        auctionService.setBidIncrement(player, amount);
    }

    private void reload(Player player) {
        if (!player.hasPermission("leeseolauction.admin")) {
            player.sendMessage(plugin.msg("admin-only"));
            return;
        }
        plugin.reloadAll();
        player.sendMessage(plugin.msg("reloaded"));
    }

    private Long parseLong(Player player, String input) {
        try {
            long value = Long.parseLong(input.replace(",", ""));
            if (value < 0L) {
                player.sendMessage(plugin.msg("not-number"));
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            player.sendMessage(plugin.msg("not-number"));
            return null;
        }
    }
}
