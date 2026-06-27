package me.leeseol.enchanting.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import me.leeseol.enchanting.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public final class LoreDescriptionService {
    private final LeeSeolEnchantingPlugin plugin;

    public LoreDescriptionService(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean refresh(ItemStack item) {
        if (!plugin.enchantingConfig().loreDescriptionsEnabled()
            || !plugin.advancedEnchantmentsBridge().available()
            || item == null
            || item.getType().isAir()
            || !item.hasItemMeta()) {
            return false;
        }
        Map<String, Integer> enchantments = plugin.advancedEnchantmentsBridge().enchantmentsOn(item);
        if (enchantments.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (lore.isEmpty()) {
            return false;
        }
        List<String> rebuilt = rebuildLore(lore, enchantments);
        if (rebuilt.equals(lore)) {
            return false;
        }
        meta.setLore(rebuilt);
        item.setItemMeta(meta);
        return true;
    }

    public int refreshInventory(Player player) {
        int changed = 0;
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (refresh(item)) {
                changed++;
            }
        }
        for (ItemStack item : inventory.getArmorContents()) {
            if (refresh(item)) {
                changed++;
            }
        }
        if (refresh(inventory.getItemInOffHand())) {
            changed++;
        }
        return changed;
    }

    private List<String> rebuildLore(List<String> lore, Map<String, Integer> enchantments) {
        List<DescriptionLine> descriptions = descriptionsFor(enchantments);
        if (descriptions.isEmpty()) {
            return lore;
        }

        List<String> rebuilt = new ArrayList<>();
        for (String line : lore) {
            String normalizedLine = normalized(line);
            DescriptionLine description = descriptionForDisplayLine(normalizedLine, descriptions);
            if (description != null) {
                rebuilt.add(line);
                rebuilt.add(description.formatted());
                continue;
            }
            if (isKnownDescriptionLine(normalizedLine, descriptions)) {
                continue;
            }
            rebuilt.add(line);
        }
        return rebuilt;
    }

    private List<DescriptionLine> descriptionsFor(Map<String, Integer> enchantments) {
        List<DescriptionLine> descriptions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            String enchant = entry.getKey();
            int level = entry.getValue();
            String display = plugin.advancedEnchantmentsBridge().display(enchant, level);
            String description = plugin.enchantingConfig().loreDescription(enchant);
            if (description.isBlank()) {
                description = plugin.advancedEnchantmentsBridge().description(enchant, level);
            }
            if (description.isBlank()) {
                continue;
            }
            String formatted = Text.color(plugin.enchantingConfig().loreDescriptionFormat()
                .replace("%description%", description)
                .replace("%enchant%", enchant)
                .replace("%level%", String.valueOf(level)));
            descriptions.add(new DescriptionLine(normalized(display), formatted, normalized(formatted)));
        }
        return descriptions;
    }

    private DescriptionLine descriptionForDisplayLine(String normalizedLine, List<DescriptionLine> descriptions) {
        for (DescriptionLine description : descriptions) {
            if (description.displayNormalized().equals(normalizedLine)) {
                return description;
            }
        }
        return null;
    }

    private boolean isKnownDescriptionLine(String normalizedLine, List<DescriptionLine> descriptions) {
        for (DescriptionLine description : descriptions) {
            if (description.descriptionNormalized().equals(normalizedLine)) {
                return true;
            }
        }
        return false;
    }

    private String normalized(String line) {
        return ChatColor.stripColor(Text.color(line))
            .replaceAll("\\s+", " ")
            .trim();
    }

    private record DescriptionLine(String displayNormalized, String formatted, String descriptionNormalized) {
    }
}
