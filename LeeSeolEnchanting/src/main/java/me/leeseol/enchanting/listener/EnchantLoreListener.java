package me.leeseol.enchanting.listener;

import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class EnchantLoreListener implements Listener {
    private final LeeSeolEnchantingPlugin plugin;

    public EnchantLoreListener(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.enchantingConfig().loreDescriptionsEnabled()
            || !plugin.enchantingConfig().refreshLoreOnInventoryClick()
            || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.loreDescriptionService().refreshInventory(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.enchantingConfig().loreDescriptionsEnabled()) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> plugin.loreDescriptionService().refreshInventory(event.getPlayer()),
            20L
        );
    }
}
