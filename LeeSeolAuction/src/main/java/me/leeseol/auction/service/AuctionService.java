package me.leeseol.auction.service;

import me.leeseol.auction.LeeSeolAuctionPlugin;
import me.leeseol.auction.economy.VaultEconomyHook;
import me.leeseol.auction.model.ActiveAuction;
import me.leeseol.auction.model.AuctionLot;
import me.leeseol.auction.storage.AuctionStore;
import me.leeseol.auction.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class AuctionService {
    private final LeeSeolAuctionPlugin plugin;
    private final AuctionStore store;
    private final VaultEconomyHook economy;
    private ActiveAuction activeAuction;

    public AuctionService(LeeSeolAuctionPlugin plugin, AuctionStore store, VaultEconomyHook economy) {
        this.plugin = plugin;
        this.store = store;
        this.economy = economy;
    }

    public void reload() {
        activeAuction = null;
        economy.reload();
        store.load();
    }

    public void save() {
        store.save();
    }

    public void submitItems(Player player, List<ItemStack> items) {
        if (!player.hasPermission("leeseolauction.submit")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }

        int count = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            store.createLot(player.getUniqueId(), player.getName(), item.clone());
            count++;
        }

        if (count <= 0) {
            player.sendMessage(plugin.msg("no-items-submitted"));
            return;
        }

        store.save();
        player.sendMessage(plugin.msg("submitted").replace("%count%", String.valueOf(count)));
    }

    public boolean openAuction(Player admin, long lotId) {
        return openAuction(admin, lotId, plugin.defaultStartingBid(), plugin.bidIncrement());
    }

    public boolean openAuction(Player admin, long lotId, long startingBid, long bidIncrement) {
        if (!admin.hasPermission("leeseolauction.admin")) {
            admin.sendMessage(plugin.msg("admin-only"));
            return true;
        }

        AuctionLot lot = store.lot(lotId);
        if (lot == null || lot.status() != AuctionLot.Status.SUBMITTED) {
            admin.sendMessage(plugin.msg("no-lot"));
            return true;
        }

        if (activeAuction != null) {
            endAuction(admin);
        }

        long normalizedStartingBid = Math.max(0L, startingBid);
        long normalizedIncrement = Math.max(1L, bidIncrement);
        lot.setStatus(AuctionLot.Status.ACTIVE);
        activeAuction = new ActiveAuction(lot.id(), normalizedStartingBid, normalizedIncrement, System.currentTimeMillis());
        store.save();

        broadcast(plugin.msg("auction-opened")
                .replace("%item%", itemName(lot.item()))
                .replace("%starting%", economy.format(normalizedStartingBid))
                .replace("%increment%", economy.format(normalizedIncrement)));
        return true;
    }

    public boolean setBidIncrement(Player admin, long bidIncrement) {
        if (!admin.hasPermission("leeseolauction.admin")) {
            admin.sendMessage(plugin.msg("admin-only"));
            return true;
        }
        if (activeAuction == null || activeLot() == null) {
            admin.sendMessage(plugin.msg("no-active"));
            return true;
        }

        activeAuction.setBidIncrement(Math.max(1L, bidIncrement));
        admin.sendMessage(plugin.msg("increment-updated")
                .replace("%increment%", economy.format(activeAuction.bidIncrement())));
        return true;
    }

    public boolean bid(Player player, long multiplier) {
        if (!player.hasPermission("leeseolauction.use")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (!economy.available()) {
            player.sendMessage(plugin.msg("economy-missing"));
            return true;
        }
        if (activeAuction == null || activeLot() == null) {
            player.sendMessage(plugin.msg("auction-closed"));
            return true;
        }

        long nextBid = nextBid(multiplier);
        if (!economy.has(player, nextBid) || !economy.withdraw(player, nextBid)) {
            player.sendMessage(plugin.msg("not-enough-money").replace("%amount%", economy.format(nextBid)));
            return true;
        }

        refundPreviousBidder(player);
        activeAuction.setCurrentBid(nextBid);
        activeAuction.setBidderUuid(player.getUniqueId());
        activeAuction.setBidderName(player.getName());

        player.sendMessage(plugin.msg("bid-success").replace("%amount%", economy.format(nextBid)));
        broadcast(plugin.msg("bid-broadcast")
                .replace("%player%", player.getName())
                .replace("%amount%", economy.format(nextBid)));
        return true;
    }

    public boolean endAuction(Player admin) {
        if (!admin.hasPermission("leeseolauction.admin")) {
            admin.sendMessage(plugin.msg("admin-only"));
            return true;
        }
        if (activeAuction == null) {
            admin.sendMessage(plugin.msg("no-active"));
            return true;
        }

        AuctionLot lot = activeLot();
        if (lot == null) {
            activeAuction = null;
            admin.sendMessage(plugin.msg("no-lot"));
            return true;
        }

        if (!activeAuction.hasBidder()) {
            lot.setStatus(AuctionLot.Status.SUBMITTED);
            activeAuction = null;
            store.save();
            broadcast(plugin.msg("no-bids"));
            return true;
        }

        OfflinePlayer winner = Bukkit.getOfflinePlayer(activeAuction.bidderUuid());
        OfflinePlayer seller = Bukkit.getOfflinePlayer(lot.sellerUuid());
        economy.deposit(seller, activeAuction.currentBid());
        lot.setStatus(AuctionLot.Status.SOLD);
        giveOrDrop(winner, lot.item());

        Player winnerPlayer = winner.getPlayer();
        if (winnerPlayer != null) {
            winnerPlayer.sendMessage(plugin.msg("won")
                    .replace("%item%", itemName(lot.item()))
                    .replace("%amount%", economy.format(activeAuction.currentBid())));
        }

        Player sellerPlayer = seller.getPlayer();
        if (sellerPlayer != null) {
            sellerPlayer.sendMessage(plugin.msg("seller-paid").replace("%amount%", economy.format(activeAuction.currentBid())));
        }

        broadcast(plugin.msg("auction-ended"));
        activeAuction = null;
        store.save();
        return true;
    }

    public ActiveAuction activeAuction() {
        return activeAuction;
    }

    public AuctionLot activeLot() {
        return activeAuction == null ? null : store.lot(activeAuction.lotId());
    }

    public List<AuctionLot> submittedLots() {
        return store.submittedLots();
    }

    public String format(long amount) {
        return economy.format(amount);
    }

    public int submittedCount() {
        return store.submittedLots().size();
    }

    public long displayedBid() {
        if (activeAuction == null) {
            return 0L;
        }
        return activeAuction.hasBidder() ? activeAuction.currentBid() : activeAuction.startingBid();
    }

    public long nextBid(long multiplier) {
        if (activeAuction == null) {
            return 0L;
        }
        long steps = Math.max(1L, multiplier);
        long basePrice = activeAuction.hasBidder() ? activeAuction.currentBid() : activeAuction.startingBid();
        return basePrice + (activeAuction.bidIncrement() * steps);
    }

    private void refundPreviousBidder(Player newBidder) {
        if (!activeAuction.hasBidder()) {
            return;
        }

        OfflinePlayer previous = Bukkit.getOfflinePlayer(activeAuction.bidderUuid());
        economy.deposit(previous, activeAuction.currentBid());
        if (!previous.getUniqueId().equals(newBidder.getUniqueId()) && previous.getPlayer() != null) {
            previous.getPlayer().sendMessage(plugin.msg("previous-bid-refunded")
                    .replace("%amount%", economy.format(activeAuction.currentBid())));
        }
    }

    private void giveOrDrop(OfflinePlayer player, ItemStack item) {
        Player online = player.getPlayer();
        if (online == null) {
            return;
        }

        var leftover = online.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack value : leftover.values()) {
                online.getWorld().dropItemNaturally(online.getLocation(), value);
            }
            online.sendMessage(plugin.msg("inventory-full"));
        }
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Text.component(message));
        }
        Bukkit.getConsoleSender().sendMessage(Text.color(message));
    }

    private String itemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().translationKey() + " x" + item.getAmount();
    }
}
