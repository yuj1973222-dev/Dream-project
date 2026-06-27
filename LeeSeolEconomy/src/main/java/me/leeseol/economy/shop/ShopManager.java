package me.leeseol.economy.shop;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopManager implements Listener {
    private final LeeSeolEconomyPlugin plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        shops.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("shops");
        if (root == null) {
            return;
        }
        for (String shopId : root.getKeys(false)) {
            if (shopId.equalsIgnoreCase("default-shop")) {
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(shopId);
            if (section == null) {
                continue;
            }
            Shop shop = new Shop(shopId, section.getString("title", "&0Shop"), section.getInt("size", 27));
            ConfigurationSection items = section.getConfigurationSection("items");
            if (items != null) {
                for (String itemId : items.getKeys(false)) {
                    ConfigurationSection itemSection = items.getConfigurationSection(itemId);
                    if (itemSection == null) {
                        continue;
                    }
                    ShopItem item = ShopItem.fromConfig(itemId, itemSection);
                    if (item == null) {
                        plugin.getLogger().warning("Skipping invalid shop item " + shopId + "." + itemId);
                        continue;
                    }
                    shop.addItem(item);
                }
            }
            shops.put(shopId.toLowerCase(), shop);
        }
    }

    public boolean open(Player player, String shopId) {
        if (!plugin.featureEnabled("shops")) {
            player.sendMessage(plugin.msg("shop-disabled"));
            return false;
        }
        String resolved = shopId == null || shopId.isBlank()
            ? plugin.getConfig().getString("shops.default-shop", "general")
            : shopId;
        Shop shop = shops.get(resolved.toLowerCase());
        if (shop == null) {
            player.sendMessage(plugin.msg("shop-not-found").replace("%shop%", resolved));
            return false;
        }
        Inventory inventory = Bukkit.createInventory(new ShopInventoryHolder(shop.id()), shop.size(), Text.color(shop.title()));
        fillFiller(inventory, resolved);
        for (ShopItem item : shop.items()) {
            inventory.setItem(item.slot(), item.icon());
        }
        player.openInventory(inventory);
        return true;
    }

    public int shopCount() {
        return shops.size();
    }

    public Iterable<String> shopIds() {
        return shops.keySet();
    }

    public boolean hasShop(String shopId) {
        return shopId != null && shops.containsKey(shopId.toLowerCase());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ShopInventoryHolder holder)) {
            return;
        }
        event.setCancelled(true);
        Shop shop = shops.get(holder.shopId().toLowerCase());
        if (shop == null) {
            player.closeInventory();
            return;
        }
        ShopItem item = shop.itemAt(event.getRawSlot());
        if (item == null) {
            return;
        }
        ClickType click = event.getClick();
        if (click.isLeftClick()) {
            buy(player, item);
        } else if (click.isRightClick()) {
            sell(player, item);
        }
    }

    private void buy(Player player, ShopItem item) {
        if (item.buyPrice() < 0L) {
            return;
        }
        if (!hasSpace(player.getInventory(), item.tradeStack())) {
            player.sendMessage(plugin.msg("inventory-full"));
            return;
        }
        if (!plugin.balanceStore().withdraw(player.getUniqueId(), item.buyPrice())) {
            player.sendMessage(plugin.msg("not-enough-money").replace("%amount%", plugin.money().format(item.buyPrice())));
            return;
        }
        plugin.ledger().recordRemoved("shop_buy", item.buyPrice());
        player.getInventory().addItem(item.tradeStack());
        player.sendMessage(plugin.msg("buy-success")
            .replace("%item%", item.plainName())
            .replace("%amount%", Integer.toString(item.amount()))
            .replace("%price%", plugin.money().format(item.buyPrice())));
    }

    private void sell(Player player, ShopItem item) {
        if (item.sellPrice() < 0L) {
            return;
        }
        ItemStack stack = item.tradeStack();
        if (!player.getInventory().containsAtLeast(stack, item.amount())) {
            player.sendMessage(plugin.msg("sell-missing-items"));
            return;
        }
        player.getInventory().removeItem(stack);
        plugin.balanceStore().deposit(player.getUniqueId(), item.sellPrice());
        plugin.ledger().recordIssued("shop_sell", item.sellPrice());
        player.sendMessage(plugin.msg("sell-success")
            .replace("%item%", item.plainName())
            .replace("%amount%", Integer.toString(item.amount()))
            .replace("%price%", plugin.money().format(item.sellPrice())));
    }

    private boolean hasSpace(PlayerInventory inventory, ItemStack stack) {
        int remaining = stack.getAmount();
        int max = stack.getMaxStackSize();
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= max;
            } else if (content.isSimilar(stack)) {
                remaining -= Math.max(0, max - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    private void fillFiller(Inventory inventory, String shopId) {
        ConfigurationSection filler = plugin.getConfig().getConfigurationSection("shops." + shopId + ".filler");
        if (filler == null || !filler.getBoolean("enabled", false)) {
            return;
        }
        Material material = Material.matchMaterial(filler.getString("material", "BLACK_STAINED_GLASS_PANE"));
        if (material == null || material.isAir()) {
            return;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(filler.getString("name", " ")));
            item.setItemMeta(meta);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }
}
