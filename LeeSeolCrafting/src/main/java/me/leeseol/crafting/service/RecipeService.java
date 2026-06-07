package me.leeseol.crafting.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.leeseol.crafting.LeeSeolCraftingPlugin;
import me.leeseol.crafting.model.Recipe;
import me.leeseol.crafting.model.RecipeInput;
import me.leeseol.crafting.model.RecipeType;
import me.leeseol.crafting.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RecipeService {
    private final LeeSeolCraftingPlugin plugin;
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public RecipeService(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        recipes.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("recipes");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            Recipe recipe = loadRecipe(id, section.getConfigurationSection(id));
            if (recipe != null) {
                recipes.put(id.toLowerCase(), recipe);
            }
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " crafting recipes.");
    }

    public Recipe recipe(String id) {
        return id == null ? null : recipes.get(id.toLowerCase());
    }

    public List<Recipe> recipes(RecipeType type) {
        return recipes.values().stream().filter(recipe -> recipe.type() == type).toList();
    }

    public int count() {
        return recipes.size();
    }

    public Iterable<String> ids() {
        return recipes.keySet();
    }

    private Recipe loadRecipe(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        List<RecipeInput> inputs = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("inputs")) {
            Object rawMaterial = raw.get("material");
            Material material = Material.matchMaterial(rawMaterial == null ? "" : String.valueOf(rawMaterial));
            int amount = parseAmount(raw.get("amount"));
            if (material == null || material.isAir() || amount <= 0) {
                plugin.getLogger().warning("Skipping invalid input in recipe " + id);
                continue;
            }
            inputs.add(new RecipeInput(new ItemStack(material, amount)));
        }
        if (inputs.isEmpty()) {
            plugin.getLogger().warning("Skipping recipe without inputs: " + id);
            return null;
        }
        ItemStack result = result(section.getConfigurationSection("result"));
        if (result == null) {
            plugin.getLogger().warning("Skipping recipe without valid result: " + id);
            return null;
        }
        return new Recipe(
            id,
            RecipeType.parse(section.getString("type", "crafting")),
            section.getString("display-name", id),
            section.getString("permission", ""),
            section.getString("required-rank", "PLAYER"),
            Math.max(0L, section.getLong("cost", 0L)),
            Math.max(0.0D, Math.min(1.0D, section.getDouble("success-rate", 1.0D))),
            List.copyOf(inputs),
            result
        );
    }

    private ItemStack result(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Material material = Material.matchMaterial(section.getString("material", ""));
        int amount = Math.max(1, section.getInt("amount", 1));
        if (material == null || material.isAir()) {
            return null;
        }
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", "");
            if (!name.isBlank()) {
                meta.setDisplayName(Text.color(name));
            }
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.setLore(Text.colorList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private int parseAmount(Object value) {
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
