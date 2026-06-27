package me.leeseol.core.menu;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CoreServerMenuManager implements Listener {
    private final LeeSeolCorePlugin plugin;
    private final Map<Integer, ServerButton> buttons = new LinkedHashMap<>();
    private final Map<UUID, Long> shortcutCooldowns = new HashMap<>();
    private String title = color("&0서버 이동");
    private int size = 9;
    private ItemStack filler;

    public CoreServerMenuManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        buttons.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("server-menu");
        if (section == null) {
            return;
        }

        title = color(section.getString("title", "&0서버 이동"));
        size = normalizeSize(section.getInt("size", 9));
        filler = loadFiller(section.getConfigurationSection("filler"));

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String id : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(id);
            if (item == null) {
                continue;
            }
            Material material = Material.matchMaterial(item.getString("material", "GRASS_BLOCK"));
            int slot = item.getInt("slot", 4);
            if (material == null || material.isAir() || slot < 0 || slot >= size) {
                plugin.getLogger().warning("Skipping invalid lobby server menu item: " + id);
                continue;
            }
            buttons.put(slot, new ServerButton(
                    item.getString("server", ""),
                    item.getString("command", ""),
                    item.getString("permission", ""),
                    material,
                    item.getString("name", "&f" + id),
                    item.getStringList("lore")
            ));
        }
    }

    public void open(Player player) {
        if (!enabled()) {
            return;
        }
        if (!player.hasPermission("leeseolcore.servermenu")) {
            player.sendMessage(color(plugin.getConfig().getString("server-menu.messages.no-permission", "&c권한이 없습니다.")));
            return;
        }

        Inventory inventory = Bukkit.createInventory(new CoreServerMenuHolder(), size, title);
        if (filler != null) {
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler.clone());
            }
        }
        for (Map.Entry<Integer, ServerButton> entry : buttons.entrySet()) {
            ServerButton button = entry.getValue();
            if (button.visibleFor(player)) {
                inventory.setItem(entry.getKey(), button.icon());
            }
        }
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!enabled() || !plugin.getConfig().getBoolean("server-menu.shortcut.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        boolean requireSneak = plugin.getConfig().getBoolean("server-menu.shortcut.require-sneak", true);
        if (requireSneak && !player.isSneaking()) {
            return;
        }
        if (!player.hasPermission("leeseolcore.servermenu")) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = plugin.getConfig().getLong("server-menu.shortcut.cooldown-millis", 700L);
        long lastUsed = shortcutCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUsed < cooldownMillis) {
            event.setCancelled(true);
            return;
        }
        shortcutCooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);
        open(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof CoreServerMenuHolder)) {
            return;
        }

        event.setCancelled(true);
        ServerButton button = buttons.get(event.getRawSlot());
        if (button == null || !button.visibleFor(player)) {
            return;
        }

        player.closeInventory();
        if (!button.command().isBlank()) {
            player.performCommand(button.command());
            return;
        }
        if (button.server().isBlank()) {
            return;
        }
        player.sendMessage(color(plugin.getConfig().getString("server-menu.messages.moving", "&a%server% 서버로 이동합니다.")
                .replace("%server%", button.server())));
        plugin.sendPlayerToServer(player, button.server());
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("server-menu.enabled", false);
    }

    private ItemStack loadFiller(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return null;
        }
        Material material = Material.matchMaterial(section.getString("material", "GRAY_STAINED_GLASS_PANE"));
        if (material == null || material.isAir()) {
            return null;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(section.getString("name", " ")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private int normalizeSize(int configuredSize) {
        int normalized = Math.max(9, Math.min(54, configuredSize));
        return normalized % 9 == 0 ? normalized : ((normalized / 9) + 1) * 9;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    private static List<String> colorList(List<String> messages) {
        List<String> output = new ArrayList<>();
        if (messages == null) {
            return output;
        }
        for (String message : messages) {
            output.add(color(message));
        }
        return output;
    }

    private record ServerButton(
            String server,
            String command,
            String permission,
            Material material,
            String name,
            List<String> lore
    ) {
        private boolean visibleFor(Player player) {
            return permission == null || permission.isBlank() || player.hasPermission(permission);
        }

        private ItemStack icon() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(name));
                meta.setLore(colorList(lore));
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}
