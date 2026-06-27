package me.leeseol.enchanting.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import me.leeseol.enchanting.model.EnchantBand;
import me.leeseol.enchanting.model.EnchantCandidate;
import me.leeseol.enchanting.model.EnchantOutput;
import me.leeseol.enchanting.service.AdvancedEnchantmentsBridge;
import me.leeseol.enchanting.service.BookshelfCounter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class EnchantingTableListener implements Listener {
    private final LeeSeolEnchantingPlugin plugin;

    public EnchantingTableListener(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.enchantingConfig().enabled()
            || !plugin.enchantingConfig().allowed(event.getEnchanter().getWorld())
            || !plugin.advancedEnchantmentsBridge().available()) {
            return;
        }

        int bookshelves = new BookshelfCounter(plugin.enchantingConfig()).count(event.getEnchantBlock());
        EnchantBand band = plugin.enchantingConfig().bandFor(bookshelves);
        if (band == null || band.chancePercent() <= 0.0D) {
            debug("No matching band. bookshelves=" + bookshelves);
            return;
        }
        if (ThreadLocalRandom.current().nextDouble(100.0D) >= band.chancePercent()) {
            debug("Roll failed. band=" + band.id() + ", bookshelves=" + bookshelves);
            return;
        }

        ItemStack target = event.getItem();
        EnchantCandidate candidate = chooseCandidate(band, target);
        if (candidate == null) {
            debug("No applicable custom enchant candidate. band=" + band.id() + ", item=" + target.getType());
            return;
        }
        int level = chooseLevel(candidate);
        Player player = event.getEnchanter();
        if (band.output() == EnchantOutput.GIVE_BOOK) {
            plugin.getServer().getScheduler().runTask(plugin, () -> giveBook(player, candidate, level));
            return;
        }
        applyToEnchantedItem(event, player, candidate, level);
    }

    private EnchantCandidate chooseCandidate(EnchantBand band, ItemStack target) {
        AdvancedEnchantmentsBridge bridge = plugin.advancedEnchantmentsBridge();
        List<EnchantCandidate> applicable = new ArrayList<>();
        int totalWeight = 0;
        for (EnchantCandidate candidate : band.candidates()) {
            if (!bridge.enchantExists(candidate.enchant())) {
                debug("Unknown AdvancedEnchantments enchant: " + candidate.enchant());
                continue;
            }
            if (band.output() == EnchantOutput.APPLY_TO_ITEM
                && !bridge.applicable(target.getType(), candidate.enchant())) {
                continue;
            }
            applicable.add(candidate);
            totalWeight += Math.max(1, candidate.weight());
        }
        if (applicable.isEmpty()) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (EnchantCandidate candidate : applicable) {
            cursor += Math.max(1, candidate.weight());
            if (roll < cursor) {
                return candidate;
            }
        }
        return applicable.getLast();
    }

    private int chooseLevel(EnchantCandidate candidate) {
        int highest = plugin.advancedEnchantmentsBridge().highestLevel(candidate.enchant());
        int min = Math.max(1, Math.min(candidate.minLevel(), highest));
        int max = Math.max(min, Math.min(candidate.maxLevel(), highest));
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void giveBook(Player player, EnchantCandidate candidate, int level) {
        ItemStack book = plugin.advancedEnchantmentsBridge().createBook(candidate.enchant(), level, 100, 0, player);
        if (book != null) {
            var overflow = player.getInventory().addItem(book);
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            plugin.message(player, "custom-applied", "%enchant%", candidate.enchant(), "%level%", String.valueOf(level));
        }
    }

    private void applyToEnchantedItem(EnchantItemEvent event, Player player, EnchantCandidate candidate, int level) {
        ItemStack target = event.getItem();
        ItemStack applied = plugin.advancedEnchantmentsBridge().apply(candidate.enchant(), level, target);
        if (applied == null || applied.getItemMeta() == null) {
            debug("AdvancedEnchantments did not return an applied item. enchant=" + candidate.enchant());
            return;
        }
        event.setItem(applied);
        Inventory inventory = event.getInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack current = inventory.getItem(0);
            if (current != null && !current.getType().isAir()) {
                plugin.loreDescriptionService().refresh(current);
            }
        });
        plugin.message(player, "custom-applied", "%enchant%", candidate.enchant(), "%level%", String.valueOf(level));
    }

    private void debug(String message) {
        if (plugin.enchantingConfig().debug()) {
            plugin.getLogger().info(message);
        }
    }
}
