package me.leeseol.economy.shop;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Shop {
    private final String id;
    private final String title;
    private final int size;
    private final Map<Integer, ShopItem> itemsBySlot = new LinkedHashMap<>();

    public Shop(String id, String title, int size) {
        this.id = id;
        this.title = title;
        this.size = normalizeSize(size);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public int size() {
        return size;
    }

    public void addItem(ShopItem item) {
        if (item.slot() >= 0 && item.slot() < size) {
            itemsBySlot.put(item.slot(), item);
        }
    }

    public ShopItem itemAt(int slot) {
        return itemsBySlot.get(slot);
    }

    public Collection<ShopItem> items() {
        return itemsBySlot.values();
    }

    private int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        return normalized % 9 == 0 ? normalized : ((normalized / 9) + 1) * 9;
    }
}
