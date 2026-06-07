package me.leeseol.economy.servermenu;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import me.leeseol.economy.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerMenuManager implements Listener, CommandExecutor {
    private final LeeSeolEconomyPlugin plugin;
    private final Map<Integer, ServerButton> buttons = new LinkedHashMap<>();
    private final Map<UUID, Long> shortcutCooldowns = new HashMap<>();
    private final Map<String, BlockedWorld> blockedWorlds = new HashMap<>();
    private ItemStack filler;
    private String title;
    private String localServer;
    private int size;

    public ServerMenuManager(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        buttons.clear();
        blockedWorlds.clear();
        this.title = Text.color(plugin.getConfig().getString("server-menu.title", "&0서버 이동"));
        this.size = normalizeSize(plugin.getConfig().getInt("server-menu.size", 27));
        this.localServer = plugin.getConfig().getString("server-menu.local-server", "").toLowerCase();
        this.filler = loadFiller();
        loadBlockedWorlds();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("server-menu.items");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Material material = Material.matchMaterial(section.getString("material", "ENDER_PEARL"));
            if (material == null || material.isAir()) {
                plugin.getLogger().warning("Skipping invalid server menu item: " + id);
                continue;
            }
            int slot = section.getInt("slot", 0);
            if (slot < 0 || slot >= size) {
                continue;
            }
            buttons.put(slot, new ServerButton(
                section.getString("server", id),
                section.getString("command", ""),
                section.getString("permission", ""),
                section.getStringList("local-servers"),
                section.getStringList("worlds"),
                section.getStringList("hidden-worlds"),
                material,
                section.getString("name", "&f" + id),
                section.getStringList("lore")
            ));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return true;
        }
        open(player);
        return true;
    }

    public void open(Player player) {
        if (!canUse(player, true)) {
            return;
        }
        if (openExternalMenu(player)) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(new ServerMenuHolder(), size, title);
        if (filler != null) {
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler.clone());
            }
        }
        for (Map.Entry<Integer, ServerButton> entry : buttons.entrySet()) {
            ServerButton button = entry.getValue();
            if (button.visibleFor(player, localServer)) {
                inventory.setItem(entry.getKey(), button.icon());
            }
        }
        player.openInventory(inventory);
        tryProgressQuestOpenGui(player, "server-menu");
    }

    public boolean canUse(Player player, boolean notify) {
        if (!plugin.featureEnabled("server-menu")) {
            return false;
        }
        if (!player.hasPermission("leeseoleconomy.servermenu")) {
            if (notify) {
                player.sendMessage(plugin.msg("no-permission"));
            }
            return false;
        }
        return !isWorldBlocked(player, notify);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!plugin.featureEnabled("server-menu")) {
            return;
        }
        if (!plugin.getConfig().getBoolean("server-menu.shortcut.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        boolean requireSneak = plugin.getConfig().getBoolean("server-menu.shortcut.require-sneak", true);
        if (requireSneak && !player.isSneaking()) {
            return;
        }
        if (!player.hasPermission("leeseoleconomy.servermenu")) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong("server-menu.shortcut.cooldown-millis", 700L);
        long lastUsed = shortcutCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUsed < cooldown) {
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
        if (!(event.getInventory().getHolder() instanceof ServerMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        ServerButton button = buttons.get(event.getRawSlot());
        if (button == null) {
            return;
        }
        if (!button.visibleFor(player, localServer)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (isWorldBlocked(player, true)) {
            player.closeInventory();
            return;
        }
        player.closeInventory();
        if (!button.command().isBlank()) {
            player.performCommand(button.command());
            return;
        }
        player.sendMessage(plugin.msg("server-moving").replace("%server%", button.server()));
        plugin.sendPlayerToServer(player, button.server());
    }

    private ItemStack loadFiller() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("server-menu.filler");
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
            meta.setDisplayName(Text.color(section.getString("name", " ")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void loadBlockedWorlds() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("server-menu.blocked-worlds");
        if (root == null) {
            return;
        }
        for (String worldName : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(worldName);
            if (section == null) {
                continue;
            }
            blockedWorlds.put(worldName.toLowerCase(), new BlockedWorld(
                    section.getString("bypass-permission", ""),
                    section.getString("message", "&c이 월드에서는 서버 이동 메뉴를 사용할 수 없습니다.")
            ));
        }
    }

    private boolean openExternalMenu(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("server-menu.external-menu");
        if (section == null || !section.getBoolean("enabled", false)) {
            return false;
        }
        String command = section.getString("open-command", "");
        if (command == null || command.isBlank()) {
            return false;
        }
        command = command
            .replace("%player%", player.getName())
            .replace("%player_name%", player.getName())
            .replace("%uuid%", player.getUniqueId().toString());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        tryProgressQuestOpenGui(player, "server-menu");
        return true;
    }

    private void tryProgressQuestOpenGui(Player player, String target) {
        if (plugin.getServer().getPluginManager().getPlugin("LeeSeolQuest") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.leeseol.quest.api.LeeSeolQuestApi");
            api.getMethod("progress", Player.class, String.class, String.class, int.class)
                .invoke(null, player, "open-gui", target, 1);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // LeeSeolQuest is a soft integration.
        }
    }

    private boolean isWorldBlocked(Player player, boolean notify) {
        BlockedWorld blockedWorld = blockedWorlds.get(player.getWorld().getName().toLowerCase());
        if (blockedWorld == null) {
            return false;
        }
        if (!blockedWorld.bypassPermission().isBlank() && player.hasPermission(blockedWorld.bypassPermission())) {
            return false;
        }
        if (notify) {
            player.sendMessage(Text.color(blockedWorld.message()));
        }
        return true;
    }

    private int normalizeSize(int configuredSize) {
        int normalized = Math.max(9, Math.min(54, configuredSize));
        return normalized % 9 == 0 ? normalized : ((normalized / 9) + 1) * 9;
    }

    private record BlockedWorld(String bypassPermission, String message) {
    }

    private record ServerButton(
            String server,
            String command,
            String permission,
            List<String> localServers,
            List<String> worlds,
            List<String> hiddenWorlds,
            Material material,
            String name,
            List<String> lore
    ) {
        private boolean visibleFor(Player player, String currentServer) {
            if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
                return false;
            }
            String serverName = currentServer == null ? "" : currentServer.toLowerCase();
            if (localServers != null && !localServers.isEmpty()
                    && localServers.stream().map(String::toLowerCase).noneMatch(serverName::equals)) {
                return false;
            }
            String worldName = player.getWorld().getName().toLowerCase();
            if (hiddenWorlds != null && hiddenWorlds.stream().map(String::toLowerCase).anyMatch(worldName::equals)) {
                return false;
            }
            if (worlds != null && !worlds.isEmpty() && worlds.stream().map(String::toLowerCase).noneMatch(worldName::equals)) {
                return false;
            }
            return true;
        }

        private ItemStack icon() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Text.color(name));
                meta.setLore(Text.colorList(lore));
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}
