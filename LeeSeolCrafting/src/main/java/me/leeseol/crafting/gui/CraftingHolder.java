package me.leeseol.crafting.gui;

import me.leeseol.crafting.model.RecipeType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CraftingHolder implements InventoryHolder {
    public enum Mode {
        LIST,
        CONFIRM,
        REPAIR
    }

    private final Mode mode;
    private final RecipeType type;
    private final String recipeId;

    public CraftingHolder(Mode mode, RecipeType type, String recipeId) {
        this.mode = mode;
        this.type = type;
        this.recipeId = recipeId;
    }

    public Mode mode() {
        return mode;
    }

    public RecipeType type() {
        return type;
    }

    public String recipeId() {
        return recipeId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
