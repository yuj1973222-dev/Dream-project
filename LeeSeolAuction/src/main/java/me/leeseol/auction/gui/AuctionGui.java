package me.leeseol.auction.gui;

import me.leeseol.auction.LeeSeolAuctionPlugin;
import me.leeseol.auction.model.ActiveAuction;
import me.leeseol.auction.model.AuctionLot;
import me.leeseol.auction.service.AuctionService;
import me.leeseol.auction.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AuctionGui implements Listener {
    private static final int[] ADMIN_LOT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int SUBMIT_ITEM_LAST_SLOT = 44;
    private static final int SUBMIT_BACK_SLOT = 45;
    private static final int SUBMIT_CONFIRM_SLOT = 49;
    private static final int MAIN_BACK_SLOT = 26;
    private static final int ADMIN_BACK_SLOT = 48;

    private final LeeSeolAuctionPlugin plugin;
    private final AuctionService auctionService;
    private final Map<UUID, Map<Integer, Long>> adminSlotLots = new HashMap<>();
    private final Set<UUID> ignoreSubmitClose = new HashSet<>();

    public AuctionGui(LeeSeolAuctionPlugin plugin, AuctionService auctionService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
    }

    public void openMain(Player player) {
        if (!ensureAvailable(player)) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(new AuctionHolder(AuctionHolder.Type.MAIN), 27, Text.color("&#FFD166경매장"));
        fill(inventory);

        ActiveAuction active = auctionService.activeAuction();
        AuctionLot activeLot = auctionService.activeLot();
        if (active == null || activeLot == null) {
            inventory.setItem(13, icon(Material.BARRIER, "&c현재 열린 경매가 없습니다.", List.of(
                    "&7관리자가 경매를 열면 입찰할 수 있습니다."
            )));
        } else {
            inventory.setItem(13, activeAuctionIcon(active, activeLot));
        }

        inventory.setItem(11, icon(Material.CHEST, "&a아이템 등록", List.of(
                "&7경매 후보 아이템을 맡길 수 있습니다.",
                "&7등록 확정을 눌러야 실제로 등록됩니다.",
                "&e클릭해서 등록 창 열기"
        )));

        if (player.hasPermission("leeseolauction.admin")) {
            inventory.setItem(15, icon(Material.GOLD_BLOCK, "&6관리자 경매 관리", List.of(
                    "&7등록된 후보 아이템을 보고",
                    "&7경매를 시작할 수 있습니다.",
                    "&8/auction open <번호> <시작가> <상승폭>"
            )));
            inventory.setItem(22, icon(Material.REDSTONE_BLOCK, "&c현재 경매 종료", List.of(
                    "&7최고 입찰자를 낙찰 처리합니다.",
                    "&7입찰자가 없으면 아이템을 후보로 되돌립니다."
            )));
        }

        inventory.setItem(MAIN_BACK_SLOT, icon(Material.ARROW, "&f이전 메뉴", List.of(
                "&7Shift+F 서버 메뉴로 돌아갑니다."
        )));

        player.openInventory(inventory);
    }

    public void openSubmit(Player player) {
        if (!ensureAvailable(player)) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(new AuctionHolder(AuctionHolder.Type.SUBMIT), 54, Text.color("&#8FD9A8경매 아이템 등록"));
        for (int slot = SUBMIT_ITEM_LAST_SLOT + 1; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        }
        inventory.setItem(SUBMIT_BACK_SLOT, icon(Material.ARROW, "&f뒤로가기", List.of(
                "&7넣은 아이템을 돌려받고",
                "&7경매장으로 돌아갑니다."
        )));
        inventory.setItem(SUBMIT_CONFIRM_SLOT, icon(Material.EMERALD_BLOCK, "&a등록 확정", List.of(
                "&7위 칸에 넣은 아이템을",
                "&7경매 후보 목록에 등록합니다."
        )));
        player.openInventory(inventory);
    }

    public void openAdminSelect(Player player, int page) {
        if (!ensureAvailable(player)) {
            return;
        }

        Inventory inventory = Bukkit.createInventory(new AuctionHolder(AuctionHolder.Type.ADMIN_SELECT, page), 54, Text.color("&#FFD166경매 후보 선택"));
        fill(inventory);

        List<AuctionLot> lots = auctionService.submittedLots();
        Map<Integer, Long> slots = new HashMap<>();
        int start = page * ADMIN_LOT_SLOTS.length;
        for (int index = 0; index < ADMIN_LOT_SLOTS.length; index++) {
            int lotIndex = start + index;
            if (lotIndex >= lots.size()) {
                break;
            }
            AuctionLot lot = lots.get(lotIndex);
            int slot = ADMIN_LOT_SLOTS[index];
            inventory.setItem(slot, adminIcon(lot));
            slots.put(slot, lot.id());
        }
        adminSlotLots.put(player.getUniqueId(), slots);

        inventory.setItem(45, icon(Material.ARROW, "&f이전 페이지", List.of("&7현재: " + (page + 1))));
        inventory.setItem(ADMIN_BACK_SLOT, icon(Material.OAK_DOOR, "&f경매장으로", List.of("&7메인 경매장 화면으로 돌아갑니다.")));
        inventory.setItem(49, icon(Material.REDSTONE_BLOCK, "&c현재 경매 종료", List.of("&7최고 입찰자를 낙찰 처리합니다.")));
        inventory.setItem(53, icon(Material.ARROW, "&f다음 페이지", List.of("&7현재: " + (page + 1))));
        player.openInventory(inventory);
    }

    public void refreshOpenMainMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof AuctionHolder holder)) {
                continue;
            }
            if (holder.type() == AuctionHolder.Type.MAIN) {
                openMain(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof AuctionHolder holder)) {
            return;
        }
        if (!plugin.auctionAvailable(player)) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(plugin.auctionEnabled() ? plugin.msg("world-disabled") : plugin.msg("disabled"));
            return;
        }

        if (holder.type() == AuctionHolder.Type.SUBMIT) {
            handleSubmitInventoryClick(event, player);
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (holder.type() == AuctionHolder.Type.MAIN) {
            handleMainClick(player, slot, event.isRightClick());
        } else if (holder.type() == AuctionHolder.Type.ADMIN_SELECT) {
            handleAdminClick(player, holder.page(), slot);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof AuctionHolder holder)) {
            return;
        }
        if (holder.type() != AuctionHolder.Type.SUBMIT) {
            event.setCancelled(true);
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && slot > SUBMIT_ITEM_LAST_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof AuctionHolder holder)) {
            return;
        }
        if (holder.type() != AuctionHolder.Type.SUBMIT) {
            return;
        }
        if (ignoreSubmitClose.remove(player.getUniqueId())) {
            return;
        }

        returnSubmitItems(player, event.getInventory());
    }

    private void handleSubmitInventoryClick(InventoryClickEvent event, Player player) {
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 0 || rawSlot >= topSize) {
            return;
        }
        if (rawSlot <= SUBMIT_ITEM_LAST_SLOT) {
            return;
        }

        event.setCancelled(true);
        if (rawSlot == SUBMIT_BACK_SLOT) {
            returnSubmitItems(player, event.getView().getTopInventory());
            ignoreSubmitClose.add(player.getUniqueId());
            clearSubmitItems(event.getView().getTopInventory());
            Bukkit.getScheduler().runTask(plugin, () -> openMain(player));
            return;
        }
        if (rawSlot == SUBMIT_CONFIRM_SLOT) {
            confirmSubmit(player, event.getView().getTopInventory());
        }
    }

    private void handleMainClick(Player player, int slot, boolean rightClick) {
        if (slot == 11) {
            openSubmit(player);
            return;
        }
        if (slot == 13) {
            long multiplier = rightClick ? 10L : 1L;
            auctionService.bid(player, multiplier);
            openMain(player);
            return;
        }
        if (slot == 15 && player.hasPermission("leeseolauction.admin")) {
            openAdminSelect(player, 0);
            return;
        }
        if (slot == 22 && player.hasPermission("leeseolauction.admin")) {
            auctionService.endAuction(player);
            openMain(player);
            return;
        }
        if (slot == MAIN_BACK_SLOT) {
            openServerMenu(player);
        }
    }

    private void handleAdminClick(Player player, int page, int slot) {
        if (slot == 45) {
            openAdminSelect(player, Math.max(0, page - 1));
            return;
        }
        if (slot == ADMIN_BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == 49) {
            auctionService.endAuction(player);
            openAdminSelect(player, page);
            return;
        }
        if (slot == 53) {
            openAdminSelect(player, page + 1);
            return;
        }

        Long lotId = adminSlotLots.getOrDefault(player.getUniqueId(), Map.of()).get(slot);
        if (lotId == null) {
            return;
        }
        auctionService.openAuction(player, lotId);
        openAdminSelect(player, page);
    }

    private void confirmSubmit(Player player, Inventory inventory) {
        List<ItemStack> submitted = collectSubmitItems(inventory);
        if (submitted.isEmpty()) {
            player.sendMessage(plugin.msg("no-items-submitted"));
            return;
        }

        ignoreSubmitClose.add(player.getUniqueId());
        clearSubmitItems(inventory);
        auctionService.submitItems(player, submitted);
        Bukkit.getScheduler().runTask(plugin, () -> openMain(player));
    }

    private List<ItemStack> collectSubmitItems(Inventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot <= SUBMIT_ITEM_LAST_SLOT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        return items;
    }

    private void clearSubmitItems(Inventory inventory) {
        for (int slot = 0; slot <= SUBMIT_ITEM_LAST_SLOT; slot++) {
            inventory.setItem(slot, null);
        }
    }

    private void returnSubmitItems(Player player, Inventory inventory) {
        for (int slot = 0; slot <= SUBMIT_ITEM_LAST_SLOT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            for (ItemStack value : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), value);
            }
            inventory.setItem(slot, null);
        }
    }

    private void openServerMenu(Player player) {
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (Bukkit.getPluginCommand("servermenu") != null) {
                player.performCommand("servermenu");
                return;
            }
            openMain(player);
        });
    }

    private boolean ensureAvailable(Player player) {
        if (plugin.auctionAvailable(player)) {
            return true;
        }
        player.sendMessage(plugin.auctionEnabled() ? plugin.msg("world-disabled") : plugin.msg("disabled"));
        return false;
    }

    private ItemStack activeAuctionIcon(ActiveAuction active, AuctionLot activeLot) {
        ItemStack display = activeLot.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(Text.color("&7등록자: &f" + activeLot.sellerName()));
            lore.add(Text.color("&7시작가: &e" + auctionService.format(active.startingBid())));
            lore.add(Text.color("&7현재가: &e" + auctionService.format(auctionService.displayedBid())));
            lore.add(Text.color("&7입찰 단위: &6" + auctionService.format(active.bidIncrement())));
            lore.add(Text.color("&7최고 입찰자: &f" + (active.bidderName() == null ? "-" : active.bidderName())));
            lore.add("");
            lore.add(Text.color("&e좌클릭: &f" + auctionService.format(auctionService.nextBid(1L)) + " 입찰"));
            lore.add(Text.color("&e우클릭: &f" + auctionService.format(auctionService.nextBid(10L)) + " 입찰"));
            lore.add(Text.color("&8낙찰과 아이템 지급은 관리자가 경매를 종료할 때 처리됩니다."));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack adminIcon(AuctionLot lot) {
        ItemStack item = lot.item().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(Text.color("&7등록 번호: &f#" + lot.id()));
            lore.add(Text.color("&7등록자: &f" + lot.sellerName()));
            lore.add(Text.color("&e클릭하면 기본 설정으로 경매를 엽니다."));
            lore.add(Text.color("&8직접 설정: /auction open " + lot.id() + " <시작가> <상승폭>"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(Text.colorList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
