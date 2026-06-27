package me.leeseol.economy.command;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.market.MarketManager;
import me.leeseol.economy.market.MarketOffer;
import me.leeseol.economy.market.MarketSaleResult;
import me.leeseol.economy.util.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MarketCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolEconomyPlugin plugin;
    private final MarketManager marketManager;

    public MarketCommand(LeeSeolEconomyPlugin plugin, MarketManager marketManager) {
        this.plugin = plugin;
        this.marketManager = marketManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!marketManager.enabledHere()) {
            sender.sendMessage(Text.color("&c현재 이 서버에서는 상단 매입을 사용할 수 없습니다."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            String category = args.length >= 2 ? args[1] : "";
            sendList(sender, category);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sell" -> handleSell(sender, args);
            case "price" -> handlePrice(sender, args);
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleSell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return;
        }
        if (!player.hasPermission("leeseoleconomy.market")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.color("&e/market sell <품목|hand> <수량|all>"));
            return;
        }

        MarketOffer offer = resolveOffer(player, args[1]);
        if (offer == null) {
            player.sendMessage(Text.color("&c상단이 매입하는 품목이 아닙니다: &e" + args[1]));
            return;
        }

        int available = countPlainItems(player, offer.material());
        if (available <= 0) {
            player.sendMessage(Text.color("&c판매할 수 있는 일반 아이템이 없습니다. 커스텀 이름/메타가 붙은 아이템은 판매하지 않습니다."));
            return;
        }

        int amount = parseAmount(args.length >= 3 ? args[2] : "all", available);
        if (amount <= 0) {
            player.sendMessage(Text.color("&c수량은 1 이상의 숫자 또는 all로 입력해주세요."));
            return;
        }

        MarketSaleResult result = marketManager.sell(player, offer, amount);
        if (!result.success()) {
            if ("inactive".equals(result.failureReason())) {
                player.sendMessage(Text.color("&c오늘 상단 주문에 없는 품목입니다. &e/market list&c로 오늘 품목을 확인해주세요."));
            } else if ("budget".equals(result.failureReason())) {
                player.sendMessage(Text.color("&c오늘 이 품목의 상단 예산이 소진되어 더 이상 매입할 수 없습니다."));
            } else {
                player.sendMessage(Text.color("&c판매할 아이템이 부족합니다."));
            }
            return;
        }

        player.sendMessage(Text.color("&a상단 납품 완료: &f" + offer.displayName()
            + " &7x" + result.acceptedAmount()
            + " &f-> &e" + plugin.money().format(result.payout())));
        if (result.acceptedAmount() < result.requestedAmount()) {
            player.sendMessage(Text.color("&7예산 부족으로 일부 수량만 매입했습니다."));
        }
        if (result.nextUnitPrice() > 0L) {
            player.sendMessage(Text.color("&7다음 예상 단가: &e" + plugin.money().format(result.nextUnitPrice())));
        }
    }

    private void handlePrice(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Text.color("&e/market price <품목>"));
            return;
        }
        MarketOffer offer = marketManager.offerById(args[1]);
        if (offer == null) {
            sender.sendMessage(Text.color("&c상단 품목을 찾을 수 없습니다: &e" + args[1]));
            return;
        }
        long price = marketManager.currentUnitPrice(offer, sender instanceof Player player ? player.getUniqueId() : null);
        sender.sendMessage(Text.color("&b[상단] &f" + offer.displayName()
            + " &7상태: &e" + (marketManager.activeToday(offer) ? "오늘 매입" : "오늘 미매입")
            + " &7현재 단가: &e" + (price <= 0L ? "마감" : plugin.money().format(price))
            + " &7기본가: &f" + plugin.money().format(offer.basePrice())
            + " &7최저가: &f" + plugin.money().format(offer.minPrice())));
        sender.sendMessage(Text.color("&7오늘 판매량: &f" + marketManager.itemSold(offer.id())
            + " &7" + marketManager.rollingDays() + "일 판매량: &f" + marketManager.rollingSold(offer.id())
            + " &7서버 재고: &f" + marketManager.stock(offer.id())));
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(Text.color("&b[상단] &f기간: &e" + marketManager.currentPeriod()));
        sender.sendMessage(Text.color("&7전체 예산: &e" + plugin.money().format(marketManager.dailyBudget())
            + " &7사용: &f" + plugin.money().format(marketManager.totalSpent())
            + " &7잔액: &a" + plugin.money().format(marketManager.remainingTotalBudget())));
        for (String category : marketManager.categories()) {
            sender.sendMessage(Text.color("&7- " + category
                + " &f" + plugin.money().format(marketManager.categorySpent(category))
                + " / &e" + plugin.money().format(marketManager.categoryBudget(category))
                + " &7잔액 &a" + plugin.money().format(marketManager.remainingCategoryBudget(category))));
        }
        long active = marketManager.offers().stream().filter(marketManager::activeToday).count();
        sender.sendMessage(Text.color("&7오늘 상단 주문: &e" + active + "&7개 / 전체 품목 &f"
            + marketManager.offers().size() + "&7개, 판매 압력 기간 &f" + marketManager.rollingDays() + "일"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(Text.color("&aLeeSeolEconomy와 상단 매입 설정을 다시 불러왔습니다."));
    }

    private void sendList(CommandSender sender, String category) {
        List<MarketOffer> offers = marketManager.activeOffersByCategory(category);
        if (offers.isEmpty()) {
            sender.sendMessage(Text.color("&c오늘 매입 중인 상단 품목이 없습니다."));
            return;
        }
        sender.sendMessage(Text.color("&b[오늘 상단 주문] &7/market sell <품목> <수량|all>"));
        int shown = 0;
        for (MarketOffer offer : offers) {
            if (shown >= 14) {
                sender.sendMessage(Text.color("&7더 보기: &e/market list <분류> &7또는 &e/market price <품목>"));
                break;
            }
            long price = marketManager.currentUnitPrice(offer, sender instanceof Player player ? player.getUniqueId() : null);
            sender.sendMessage(Text.color("&7- &e" + offer.id()
                + " &f" + offer.displayName()
                + " &8[" + offer.category() + "] &7단가: &a"
                + (price <= 0L ? "마감" : plugin.money().format(price))));
            shown++;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&e/market list [분류] &7- 상단 매입 품목"));
        sender.sendMessage(Text.color("&e/market price <품목> &7- 현재 단가 확인"));
        sender.sendMessage(Text.color("&e/market sell <품목|hand> <수량|all> &7- 아이템 납품"));
        sender.sendMessage(Text.color("&e/market status &7- 서버 은행 상태"));
        if (sender.hasPermission("leeseoleconomy.admin")) {
            sender.sendMessage(Text.color("&e/market reload &7- 설정 다시 불러오기"));
        }
    }

    private MarketOffer resolveOffer(Player player, String raw) {
        if (raw.equalsIgnoreCase("hand")) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                return null;
            }
            return marketManager.offerByMaterial(hand.getType());
        }
        return marketManager.offerById(raw);
    }

    private int parseAmount(String raw, int available) {
        if (raw == null || raw.equalsIgnoreCase("all")) {
            return available;
        }
        try {
            return Math.min(available, Math.max(0, Integer.parseInt(raw.replace(",", ""))));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private int countPlainItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material && !item.hasItemMeta()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("list", "price", "sell", "status"));
            if (sender.hasPermission("leeseoleconomy.admin")) {
                options.add("reload");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && List.of("price", "sell").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> ids = new ArrayList<>(marketManager.offerIds());
            if (args[0].equalsIgnoreCase("sell")) {
                ids.add("hand");
            }
            return filter(ids, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return filter(marketManager.categories(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            return filter(List.of("all", "64", "128", "576"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
            .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowered))
            .toList();
    }
}
