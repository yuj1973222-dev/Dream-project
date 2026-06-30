package me.leeseol.combat.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ItemSnapshots {
    private ItemSnapshots() {
    }

    public static List<ItemStack> snapshot(Player player) {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        addAll(items, inventory.getStorageContents());
        addAll(items, inventory.getArmorContents());
        addAll(items, inventory.getExtraContents());
        return items;
    }

    public static void drop(Location location, List<ItemStack> items) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        for (ItemStack item : items) {
            ItemStack copy = copy(item);
            if (copy != null) {
                world.dropItemNaturally(location, copy);
            }
        }
    }

    public static void clear(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.updateInventory();
    }

    public static ItemStack copy(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return null;
        }
        return item.clone();
    }

    private static void addAll(List<ItemStack> items, ItemStack[] source) {
        if (source == null) {
            return;
        }
        for (ItemStack item : source) {
            ItemStack copy = copy(item);
            if (copy != null) {
                items.add(copy);
            }
        }
    }
}
