package me.leeseol.crafting.model;

import java.util.List;
import org.bukkit.inventory.ItemStack;

public record Recipe(
    String id,
    RecipeType type,
    String displayName,
    String permission,
    String requiredRank,
    long cost,
    double successRate,
    List<RecipeInput> inputs,
    ItemStack result
) {
}
