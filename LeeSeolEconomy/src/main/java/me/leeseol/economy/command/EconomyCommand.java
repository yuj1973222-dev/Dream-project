package me.leeseol.economy.command;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.ledger.LedgerSnapshot;
import me.leeseol.economy.storage.BalanceSnapshot;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

        if (command.getName().equalsIgnoreCase("pay")) {
            handleDirectPay(sender, args);
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
            case "report", "ledger" -> handleReport(sender, args);
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
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.msg("pay-self"));
            return;
        }
        if (!plugin.balanceStore().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage(plugin.msg("not-enough-money").replace("%amount%", plugin.money().format(amount)));
            return;
        }
        plugin.ledger().recordTransferred("player_pay", amount);
        player.sendMessage(plugin.msg("paid-sender").replace("%player%", target.getName()).replace("%amount%", plugin.money().format(amount)));
        target.sendMessage(plugin.msg("paid-receiver").replace("%player%", player.getName()).replace("%amount%", plugin.money().format(amount)));
    }

    private void handleDirectPay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e/pay <player> <amount>");
            return;
        }
        handlePay(sender, new String[] {"pay", args[0], args[1]});
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
        long previous = plugin.balanceStore().getBalance(target.getUniqueId());
        switch (sub) {
            case "give" -> {
                plugin.balanceStore().deposit(target.getUniqueId(), amount);
                plugin.ledger().recordIssued("admin_give", amount);
            }
            case "take" -> {
                if (plugin.balanceStore().withdraw(target.getUniqueId(), amount)) {
                    plugin.ledger().recordRemoved("admin_take", amount);
                }
            }
            case "set" -> {
                plugin.balanceStore().setBalance(target.getUniqueId(), amount);
                if (amount > previous) {
                    plugin.ledger().recordIssued("admin_set", amount - previous);
                } else if (previous > amount) {
                    plugin.ledger().recordRemoved("admin_set", previous - amount);
                }
            }
            default -> {
                return;
            }
        }
        long balance = plugin.balanceStore().getBalance(target.getUniqueId());
        sender.sendMessage("§a" + (target.getName() == null ? args[1] : target.getName()) + " balance: " + plugin.money().format(balance));
    }

    private void handleReport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        LedgerSnapshot ledger = args.length >= 2 ? plugin.ledger().snapshot(args[1]) : plugin.ledger().today();
        BalanceSnapshot balances = plugin.balanceStore().snapshot();
        long marketActive = plugin.marketManager().offers().stream().filter(plugin.marketManager()::activeToday).count();

        sender.sendMessage("§b[경제 리포트] §f기간: §e" + ledger.period());
        sender.sendMessage("§7플레이어 지갑 총액: §e" + plugin.money().format(balances.totalBalance())
            + " §7계정: §f" + balances.accounts()
            + " §7평균: §f" + plugin.money().format(balances.averageBalance())
            + " §7최고: §f" + plugin.money().format(balances.highestBalance()));
        sender.sendMessage("§7오늘 발행: §a" + plugin.money().format(ledger.totalIssued())
            + " §7회수/지갑유출: §c" + plugin.money().format(ledger.totalRemoved())
            + " §7순증: §e" + plugin.money().format(Math.max(0L, ledger.netIssued()))
            + (ledger.netIssued() < 0L ? " §7(순감 " + plugin.money().format(Math.abs(ledger.netIssued())) + ")" : ""));
        sender.sendMessage("§7유저간 이동량: §f" + plugin.money().format(ledger.totalTransferred()));
        sender.sendMessage("§7발행 상세: §f" + formatSources(ledger.issued()));
        sender.sendMessage("§7회수 상세: §f" + formatSources(ledger.removed()));
        sender.sendMessage("§7이동 상세: §f" + formatSources(ledger.transferred()));
        sender.sendMessage("§7상단 예산: §f" + plugin.money().format(plugin.marketManager().totalSpent())
            + " / §e" + plugin.money().format(plugin.marketManager().dailyBudget())
            + " §7오늘 주문: §f" + marketActive + "/" + plugin.marketManager().offers().size());
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
        sender.sendMessage("§e/pay <player> <amount> §7- 빠른 송금");
        if (sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage("§e/won give|take|set <player> <amount> §7- 관리자 금액 조정");
            sender.sendMessage("§e/won report [yyyy-mm-dd] §7- 경제 장부/통화량 리포트");
            sender.sendMessage("§e/won reload §7- 설정 다시 불러오기");
        }
    }

    private String formatSources(Map<String, Long> values) {
        if (values.isEmpty()) {
            return "-";
        }
        List<Map.Entry<String, Long>> entries = values.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(6)
            .toList();
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Long> entry : entries) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append("=").append(plugin.money().format(entry.getValue()));
        }
        if (values.size() > entries.size()) {
            builder.append(", ...");
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("pay")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList();
            }
            List<String> options = new ArrayList<>(Arrays.asList("balance", "pay"));
            if (sender.hasPermission("leeseoleconomy.admin")) {
                options.addAll(Arrays.asList("give", "take", "set", "report", "ledger", "reload"));
            }
            return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && command.getName().equalsIgnoreCase("pay")) {
            return List.of("100", "1000", "10000");
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
