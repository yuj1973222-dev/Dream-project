package me.leeseol.crafting.gui;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.crafting.LeeSeolCraftingPlugin;
import me.leeseol.crafting.model.Recipe;
import me.leeseol.crafting.model.RecipeInput;
import me.leeseol.crafting.model.RecipeType;
import me.leeseol.crafting.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CraftingGui {
    private final LeeSeolCraftingPlugin plugin;

    public CraftingGui(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    public void openList(Player player, RecipeType type) {
        if (!plugin.canUse(player)) {
            return;
        }
        int size = normalizeSize(plugin.getConfig().getInt("gui." + type.configName() + ".size", 54));
        String title = plugin.getConfig().getString("gui." + type.configName() + ".title", "&0제작");
        Inventory inventory = Bukkit.createInventory(new CraftingHolder(CraftingHolder.Mode.LIST, type, null), size, Text.color(title));
        int slot = 0;
        for (Recipe recipe : plugin.recipeService().recipes(type)) {
            if (slot >= size) {
                break;
            }
            inventory.setItem(slot++, icon(recipe));
        }
        player.openInventory(inventory);
    }

    public void openConfirm(Player player, Recipe recipe) {
        int size = normalizeSize(plugin.getConfig().getInt("gui.confirm.size", 27));
        String title = plugin.getConfig().getString("gui.confirm.title", "&0제작 확인");
        Inventory inventory = Bukkit.createInventory(new CraftingHolder(CraftingHolder.Mode.CONFIRM, recipe.type(), recipe.id()), size, Text.color(title));
        inventory.setItem(11, icon(recipe));
        inventory.setItem(15, named(Material.LIME_STAINED_GLASS_PANE, "&a확정", List.of("&7클릭하면 제작을 진행합니다.")));
        inventory.setItem(22, named(Material.BARRIER, "&c뒤로", List.of("&7레시피 목록으로 돌아갑니다.")));
        player.openInventory(inventory);
    }

    public void openRepair(Player player) {
        if (!plugin.canUse(player)) {
            return;
        }
        if (!player.hasPermission("leeseolcrafting.repair")) {
            plugin.message(player, "no-permission");
            return;
        }
        Inventory inventory = Bukkit.createInventory(new CraftingHolder(CraftingHolder.Mode.REPAIR, null, null), 27, Text.color(plugin.getConfig().getString("gui.repair.title", "&0수리")));
        ItemStack item = player.getInventory().getItemInMainHand();
        long cost = plugin.repairService().cost(item);
        inventory.setItem(11, item == null || item.getType().isAir()
            ? named(Material.BARRIER, "&c수리 불가", List.of("&7손에 손상된 장비를 들어주세요."))
            : item.clone());
        inventory.setItem(15, named(Material.ANVIL, "&a수리 확정", List.of("&7비용: &f" + cost + "원", "&7손에 든 장비를 수리합니다.")));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (holder.mode() == CraftingHolder.Mode.LIST) {
            ItemStack current = event.getCurrentItem();
            if (current == null || !current.hasItemMeta()) {
                return;
            }
            String recipeId = current.getItemMeta().getPersistentDataContainer().get(LeeSeolCraftingPlugin.RECIPE_KEY, PersistentDataType.STRING);
            Recipe recipe = plugin.recipeService().recipe(recipeId);
            if (recipe != null) {
                openConfirm(player, recipe);
            }
            return;
        }
        if (holder.mode() == CraftingHolder.Mode.CONFIRM) {
            if (event.getRawSlot() == 22) {
                openList(player, holder.type());
                return;
            }
            if (event.getRawSlot() == 15) {
                Recipe recipe = plugin.recipeService().recipe(holder.recipeId());
                player.closeInventory();
                plugin.craftingService().craft(player, recipe);
            }
            return;
        }
        if (holder.mode() == CraftingHolder.Mode.REPAIR && event.getRawSlot() == 15) {
            player.closeInventory();
            plugin.repairService().repairMainHand(player);
        }
    }

    private ItemStack icon(Recipe recipe) {
        ItemStack item = recipe.result().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(recipe.displayName()));
            List<String> lore = new ArrayList<>();
            lore.add(Text.color("&7비용: &f" + recipe.cost() + "원"));
            lore.add(Text.color("&7성공률: &f" + Math.round(recipe.successRate() * 100.0D) + "%"));
            lore.add(Text.color("&7필요 랭크: &f" + recipe.requiredRank()));
            lore.add(Text.color("&8재료"));
            for (RecipeInput input : recipe.inputs()) {
                lore.add(Text.color("&7- &f" + input.stack().getType().name() + " x" + input.amount()));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(LeeSeolCraftingPlugin.RECIPE_KEY, PersistentDataType.STRING, recipe.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(Text.colorList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private int normalizeSize(int size) {
        int bounded = Math.max(9, Math.min(54, size));
        return bounded % 9 == 0 ? bounded : ((bounded / 9) + 1) * 9;
    }
}
