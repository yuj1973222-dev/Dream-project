package me.leeseol.economy.shop;

import me.leeseol.economy.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ShopItem {
    private final String id;
    private final int slot;
    private final Material material;
    private final int amount;
    private final long buyPrice;
    private final long sellPrice;
    private final String displayName;
    private final List<String> lore;

    private ShopItem(String id, int slot, Material material, int amount, long buyPrice, long sellPrice, String displayName, List<String> lore) {
        this.id = id;
        this.slot = slot;
        this.material = material;
        this.amount = Math.max(1, amount);
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.displayName = displayName;
        this.lore = lore;
    }

    public static ShopItem fromConfig(String id, ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null || material.isAir()) {
            return null;
        }
        return new ShopItem(
            id,
            section.getInt("slot", 0),
            material,
            section.getInt("amount", 1),
            section.getLong("buy", -1L),
            section.getLong("sell", -1L),
            section.getString("name", "&f" + material.name()),
            section.getStringList("lore")
        );
    }

    public ItemStack icon() {
        ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(displayName));
            meta.setLore(Text.colorList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack tradeStack() {
        return new ItemStack(material, amount);
    }

    public String id() {
        return id;
    }

    public int slot() {
        return slot;
    }

    public Material material() {
        return material;
    }

    public int amount() {
        return amount;
    }

    public long buyPrice() {
        return buyPrice;
    }

    public long sellPrice() {
        return sellPrice;
    }

    public String plainName() {
        return material.name().toLowerCase();
    }
}
