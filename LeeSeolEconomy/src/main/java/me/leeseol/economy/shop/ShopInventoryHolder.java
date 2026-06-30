package me.leeseol.economy.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ShopInventoryHolder implements InventoryHolder {
    private final String shopId;

    public ShopInventoryHolder(String shopId) {
        this.shopId = shopId;
    }

    public String shopId() {
        return shopId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
