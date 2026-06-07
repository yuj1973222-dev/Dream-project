package me.leeseol.economy.command;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class EconomyCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolEconomyPlugin plugin;

    public EconomyCommand(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.featureEnabled("economy-commands")) {
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.msgRaw("player-only"));
                return true;
            }
            sendBalance(player, player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "balance", "bal", "money" -> handleBalance(sender, args);
            case "pay" -> handlePay(sender, args);
            case "give", "take", "set" -> handleAdminBalance(sender, sub, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("leeseoleconomy.admin")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            long balance = plugin.balanceStore().getBalance(target.getUniqueId());
            sender.sendMessage(plugin.msg("balance-other")
                .replace("%player%", target.getName() == null ? args[1] : target.getName())
                .replace("%amount%", plugin.money().format(balance)));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return;
        }
        sendBalance(sender, player);
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return;
        }
        if (!player.hasPermission("leeseoleconomy.pay")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§e/won pay <player> <amount>");
            return;
        }
        long amount = plugin.money().parse(args[2]);
        if (amount <= 0L) {
            player.sendMessage(plugin.msg("invalid-amount"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(plugin.msg("player-not-found"));
            return;
        }
        if (!plugin.balanceStore().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage(plugin.msg("not-enough-money").replace("%amount%", plugin.money().format(amount)));
            return;
        }
        player.sendMessage(plugin.msg("paid-sender").replace("%player%", target.getName()).replace("%amount%", plugin.money().format(amount)));
        target.sendMessage(plugin.msg("paid-receiver").replace("%player%", player.getName()).replace("%amount%", plugin.money().format(amount)));
    }

    private void handleAdminBalance(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§e/won " + sub + " <player> <amount>");
            return;
        }
        long amount = plugin.money().parse(args[2]);
        if (amount <= 0L) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        switch (sub) {
            case "give" -> plugin.balanceStore().deposit(target.getUniqueId(), amount);
            case "take" -> plugin.balanceStore().withdraw(target.getUniqueId(), amount);
            case "set" -> plugin.balanceStore().setBalance(target.getUniqueId(), amount);
            default -> {
                return;
            }
        }
        long balance = plugin.balanceStore().getBalance(target.getUniqueId());
        sender.sendMessage("§a" + (target.getName() == null ? args[1] : target.getName()) + " balance: " + plugin.money().format(balance));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(plugin.msg("reloaded"));
    }

    private void sendBalance(CommandSender sender, Player player) {
        long balance = plugin.balanceStore().getBalance(player.getUniqueId());
        sender.sendMessage(plugin.msg("balance").replace("%amount%", plugin.money().format(balance)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e/won §7- 내 보유 금액 확인");
        sender.sendMessage("§e/won pay <player> <amount> §7- 송금");
        if (sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage("§e/won give|take|set <player> <amount> §7- 관리자 금액 조정");
            sender.sendMessage("§e/won reload §7- 설정 다시 불러오기");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("balance", "pay"));
            if (sender.hasPermission("leeseoleconomy.admin")) {
                options.addAll(Arrays.asList("give", "take", "set", "reload"));
            }
            return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && List.of("balance", "pay", "give", "take", "set").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && List.of("pay", "give", "take", "set").contains(args[0].toLowerCase())) {
            return List.of("100", "1000", "10000");
        }
        return Collections.emptyList();
    }
}
