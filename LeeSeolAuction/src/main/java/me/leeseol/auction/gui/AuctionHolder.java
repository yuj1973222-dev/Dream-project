package me.leeseol.auction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AuctionHolder implements InventoryHolder {
    public enum Type {
        MAIN,
        SUBMIT,
        ADMIN_SELECT
    }

    private final Type type;
    private final int page;

    public AuctionHolder(Type type) {
        this(type, 0);
    }

    public AuctionHolder(Type type, int page) {
        this.type = type;
        this.page = page;
    }

    public Type type() {
        return type;
    }

    public int page() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
