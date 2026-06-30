package me.leeseol.crafting.model;

import org.bukkit.inventory.ItemStack;

public record RecipeInput(ItemStack stack) {
    public int amount() {
        return stack.getAmount();
    }
}
