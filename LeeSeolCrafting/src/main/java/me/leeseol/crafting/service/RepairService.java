package me.leeseol.crafting.service;

import me.leeseol.crafting.LeeSeolCraftingPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public final class RepairService {
    private final LeeSeolCraftingPlugin plugin;

    public RepairService(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canRepair(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getType().getMaxDurability() <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) {
            return false;
        }
        String typeName = item.getType().name();
        return plugin.getConfig().getStringList("repair.allowed-types").stream().anyMatch(typeName::endsWith);
    }

    public long cost(ItemStack item) {
        if (!canRepair(item)) {
            return 0L;
        }
        Damageable damageable = (Damageable) item.getItemMeta();
        long base = plugin.getConfig().getLong("repair.base-cost", 1000L);
        long perDamage = plugin.getConfig().getLong("repair.cost-per-damage", 15L);
        long max = plugin.getConfig().getLong("repair.max-cost", 100000L);
        return Math.min(max, Math.max(0L, base + damageable.getDamage() * perDamage));
    }

    public boolean repairMainHand(Player player) {
        if (!plugin.getConfig().getBoolean("repair.enabled", true)) {
            plugin.message(player, "repair-disabled");
            return false;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!canRepair(item)) {
            plugin.message(player, "repair-invalid");
            return false;
        }
        long cost = cost(item);
        if (!player.hasPermission("leeseolcrafting.bypass.cost")) {
            if (!plugin.economyService().has(player, cost)) {
                plugin.message(player, "not-enough-money", "%amount%", String.valueOf(cost));
                return false;
            }
            plugin.economyService().withdraw(player, cost);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
            item.setItemMeta(meta);
        }
        plugin.message(player, "repair-success", "%cost%", String.valueOf(cost));
        return true;
    }
}
