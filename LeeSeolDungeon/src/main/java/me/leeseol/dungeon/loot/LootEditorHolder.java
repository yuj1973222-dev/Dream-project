package me.leeseol.dungeon.loot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class LootEditorHolder implements InventoryHolder {
    private final String tableId;

    public LootEditorHolder(String tableId) {
        this.tableId = tableId;
    }

    public String tableId() {
        return tableId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
