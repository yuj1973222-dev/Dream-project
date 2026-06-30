package me.leeseol.town.structure;

import java.util.HashMap;
import java.util.Map;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class StructureSelectionGui {
    public static final String TITLE = "구조물 선택";

    private final LeeSeolTownPlugin plugin;

    public StructureSelectionGui(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = Bukkit.createInventory(holder, 27, Text.component(TITLE));
        holder.inventory = inventory;

        int slot = 0;
        for (StructureDefinition definition : plugin.structureRegistry().all()) {
            Material material = Material.matchMaterial(definition.icon());
            ItemStack icon = new ItemStack(material == null ? Material.STONE : material);
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Text.component(definition.name()));
            icon.setItemMeta(meta);
            inventory.setItem(slot, icon);
            holder.slots.put(slot, definition.id());
            slot++;
            if (slot >= inventory.getSize()) {
                break;
            }
        }
        player.openInventory(inventory);
    }

    public static final class Holder implements InventoryHolder {
        private final Map<Integer, String> slots = new HashMap<>();
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        public String structureIdAt(int slot) {
            return slots.get(slot);
        }
    }
}
