package me.leeseol.crafting.service;

import java.util.HashMap;
import java.util.Map;
import me.leeseol.crafting.LeeSeolCraftingPlugin;
import me.leeseol.crafting.model.Recipe;
import me.leeseol.crafting.model.RecipeInput;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CraftingService {
    private final LeeSeolCraftingPlugin plugin;

    public CraftingService(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean craft(Player player, Recipe recipe) {
        if (!plugin.canUse(player)) {
            return false;
        }
        if (recipe == null) {
            plugin.message(player, "unknown-recipe");
            return false;
        }
        if (!recipe.permission().isBlank() && !player.hasPermission(recipe.permission())) {
            plugin.message(player, "no-permission");
            return false;
        }
        if (!plugin.rankRequirementService().allowed(player, recipe.requiredRank())) {
            plugin.message(player, "rank-blocked", "%rank%", recipe.requiredRank());
            return false;
        }
        if (!hasMaterials(player, recipe)) {
            plugin.message(player, "missing-materials");
            return false;
        }
        if (!hasSpace(player, recipe.result())) {
            plugin.message(player, "inventory-full");
            return false;
        }
        boolean bypassCost = player.hasPermission("leeseolcrafting.bypass.cost");
        if (!bypassCost && !plugin.economyService().has(player, recipe.cost())) {
            plugin.message(player, "not-enough-money", "%amount%", String.valueOf(recipe.cost()));
            return false;
        }

        boolean success = Math.random() <= recipe.successRate();
        if (success || plugin.getConfig().getBoolean("failure.consume-materials", true)) {
            removeMaterials(player, recipe);
        }
        if (!bypassCost && (success || plugin.getConfig().getBoolean("failure.consume-money", true))) {
            plugin.economyService().withdraw(player, recipe.cost());
        }
        if (!success) {
            plugin.message(player, "craft-failed", "%recipe%", plugin.color(recipe.displayName()));
            return false;
        }

        player.getInventory().addItem(recipe.result().clone());
        plugin.message(player, "craft-success", "%recipe%", plugin.color(recipe.displayName()));
        tryProgressQuest(player, recipe.result().getType().name());
        return true;
    }

    private boolean hasMaterials(Player player, Recipe recipe) {
        Map<Material, Integer> required = requirements(recipe);
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void removeMaterials(Player player, Recipe recipe) {
        for (Map.Entry<Material, Integer> entry : requirements(recipe).entrySet()) {
            player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    private Map<Material, Integer> requirements(Recipe recipe) {
        Map<Material, Integer> required = new HashMap<>();
        for (RecipeInput input : recipe.inputs()) {
            required.merge(input.stack().getType(), input.amount(), Integer::sum);
        }
        return required;
    }

    private boolean hasSpace(Player player, ItemStack stack) {
        return player.getInventory().firstEmpty() >= 0 || player.getInventory().containsAtLeast(stack, 1);
    }

    private void tryProgressQuest(Player player, String target) {
        if (plugin.getServer().getPluginManager().getPlugin("LeeSeolQuest") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.leeseol.quest.api.LeeSeolQuestApi");
            api.getMethod("progress", Player.class, String.class, String.class, int.class)
                .invoke(null, player, "craft-item", target, 1);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // LeeSeolQuest is a soft integration.
        }
    }
}
