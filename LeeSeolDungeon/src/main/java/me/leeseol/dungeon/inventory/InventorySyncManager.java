package me.leeseol.dungeon.inventory;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import me.leeseol.dungeon.util.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class InventorySyncManager implements Listener {
    private final LeeSeolDungeonPlugin plugin;
    private File dataFile;
    private YamlConfiguration data;
    private boolean enabled;

    public InventorySyncManager(LeeSeolDungeonPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("inventory-sync.enabled", true);
        dataFile = new File(plugin.getConfig().getString("inventory-sync.data-file", "/opt/minecraft/shared/dungeon/inventories.yml"));
        if (dataFile.getParentFile() != null) {
            dataFile.getParentFile().mkdirs();
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        long delay = Math.max(1L, plugin.getConfig().getLong("inventory-sync.load-delay-ticks", 10L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> loadPlayer(event.getPlayer(), true), delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled || !plugin.getConfig().getBoolean("inventory-sync.save-on-quit", true)) {
            return;
        }
        savePlayer(event.getPlayer(), false);
        saveStore();
    }

    public void saveOnlinePlayers() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayer(player, false);
        }
    }

    public void saveBeforeTransfer(Player player) {
        if (!enabled || !plugin.getConfig().getBoolean("inventory-sync.save-before-portal-transfer", true)) {
            return;
        }
        savePlayer(player, true);
        saveStore();
    }

    public void savePlayer(Player player, boolean notify) {
        if (!enabled || data == null) {
            return;
        }
        String path = "players." + player.getUniqueId();
        try {
            data.set(path + ".name", player.getName());
            data.set(path + ".storage", ItemSerialization.serialize(player.getInventory().getStorageContents()));
            data.set(path + ".armor", ItemSerialization.serialize(player.getInventory().getArmorContents()));
            data.set(path + ".extra", ItemSerialization.serialize(player.getInventory().getExtraContents()));
            if (plugin.getConfig().getBoolean("inventory-sync.sync-ender-chest", true)) {
                data.set(path + ".ender", ItemSerialization.serialize(player.getEnderChest().getContents()));
            }
            if (plugin.getConfig().getBoolean("inventory-sync.sync-health-food-exp", true)) {
                data.set(path + ".health", player.getHealth());
                data.set(path + ".food", player.getFoodLevel());
                data.set(path + ".saturation", player.getSaturation());
                data.set(path + ".level", player.getLevel());
                data.set(path + ".exp", player.getExp());
                data.set(path + ".total-exp", player.getTotalExperience());
            }
            if (notify) {
                player.sendMessage(plugin.msg("inventory-saved"));
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to serialize inventory for " + player.getName() + ": " + exception.getMessage());
        }
    }

    public void loadPlayer(Player player, boolean notify) {
        if (!enabled || data == null) {
            return;
        }
        String path = "players." + player.getUniqueId();
        if (!data.isConfigurationSection(path)) {
            return;
        }
        try {
            ItemStack[] storage = ItemSerialization.deserialize(data.getString(path + ".storage", ""));
            ItemStack[] armor = ItemSerialization.deserialize(data.getString(path + ".armor", ""));
            ItemStack[] extra = ItemSerialization.deserialize(data.getString(path + ".extra", ""));
            player.getInventory().setStorageContents(ItemSerialization.fit(storage, player.getInventory().getStorageContents().length));
            player.getInventory().setArmorContents(ItemSerialization.fit(armor, player.getInventory().getArmorContents().length));
            player.getInventory().setExtraContents(ItemSerialization.fit(extra, player.getInventory().getExtraContents().length));
            if (plugin.getConfig().getBoolean("inventory-sync.sync-ender-chest", true)) {
                ItemStack[] ender = ItemSerialization.deserialize(data.getString(path + ".ender", ""));
                player.getEnderChest().setContents(ItemSerialization.fit(ender, player.getEnderChest().getSize()));
            }
            if (plugin.getConfig().getBoolean("inventory-sync.sync-health-food-exp", true)) {
                double maxHealth = player.getMaxHealth();
                player.setHealth(Math.max(1.0D, Math.min(maxHealth, data.getDouble(path + ".health", maxHealth))));
                player.setFoodLevel(Math.max(0, Math.min(20, data.getInt(path + ".food", 20))));
                player.setSaturation((float) data.getDouble(path + ".saturation", 5.0D));
                player.setLevel(Math.max(0, data.getInt(path + ".level", 0)));
                player.setExp((float) Math.max(0.0D, Math.min(1.0D, data.getDouble(path + ".exp", 0.0D))));
                player.setTotalExperience(Math.max(0, data.getInt(path + ".total-exp", 0)));
            }
            if (notify) {
                player.sendMessage(plugin.msg("inventory-loaded"));
            }
        } catch (IOException | ClassNotFoundException exception) {
            plugin.getLogger().warning("Failed to load inventory for " + player.getName() + ": " + exception.getMessage());
        }
    }

    public void saveStore() {
        if (data == null || dataFile == null) {
            return;
        }
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save inventory data: " + exception.getMessage());
        }
    }

    public boolean hasData(OfflinePlayer player) {
        return data != null && data.isConfigurationSection("players." + player.getUniqueId());
    }
}
