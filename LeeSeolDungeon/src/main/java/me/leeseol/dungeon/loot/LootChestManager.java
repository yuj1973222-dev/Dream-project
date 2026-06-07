package me.leeseol.dungeon.loot;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import me.leeseol.dungeon.util.ItemSerialization;
import me.leeseol.dungeon.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class LootChestManager implements Listener {
    private final LeeSeolDungeonPlugin plugin;
    private final Map<String, LootChestSpot> spots = new LinkedHashMap<>();
    private BukkitTask rollTask;
    private boolean enabled;

    public LootChestManager(LeeSeolDungeonPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        stop();
        enabled = plugin.getConfig().getBoolean("loot-chests.enabled", true);
        spots.clear();
        loadSpots();
        if (enabled && plugin.roleAllows("dungeon")) {
            long intervalSeconds = Math.max(10L, plugin.getConfig().getLong("loot-chests.roll-interval-seconds", 300L));
            rollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::rollAll, intervalSeconds * 20L, intervalSeconds * 20L);
        }
    }

    public void stop() {
        if (rollTask != null) {
            rollTask.cancel();
            rollTask = null;
        }
    }

    public void openTableEditor(Player player, String tableId) {
        Inventory inventory = Bukkit.createInventory(new LootEditorHolder(tableId), 27, Text.color("&#B9E6FF던전 상자: " + tableId));
        ItemStack[] contents = loadTable(tableId);
        inventory.setContents(ItemSerialization.fit(contents, inventory.getSize()));
        player.openInventory(inventory);
        player.sendMessage(plugin.msg("chest-table-opened").replace("%table%", tableId));
    }

    public void addSpot(Player player, String id, String table, double chance, long respawnSeconds) {
        Block target = player.getTargetBlockExact(8);
        Location location = target == null ? player.getLocation().getBlock().getLocation() : target.getLocation();
        String base = "loot-chests.spots." + id;
        plugin.getConfig().set(base + ".world", location.getWorld().getName());
        plugin.getConfig().set(base + ".x", location.getBlockX());
        plugin.getConfig().set(base + ".y", location.getBlockY());
        plugin.getConfig().set(base + ".z", location.getBlockZ());
        plugin.getConfig().set(base + ".table", table);
        plugin.getConfig().set(base + ".chance", Math.max(0.0D, Math.min(1.0D, chance)));
        plugin.getConfig().set(base + ".respawn-seconds", Math.max(1L, respawnSeconds));
        plugin.saveConfig();
        reload();
        player.sendMessage(plugin.msg("chest-spot-added").replace("%id%", id));
    }

    public boolean removeSpot(String id) {
        if (!plugin.getConfig().isConfigurationSection("loot-chests.spots." + id)) {
            return false;
        }
        plugin.getConfig().set("loot-chests.spots." + id, null);
        plugin.saveConfig();
        reload();
        return true;
    }

    public boolean spawnSpot(String id, boolean force) {
        LootChestSpot spot = spots.get(id);
        if (spot == null) {
            return false;
        }
        return spawnSpot(spot, force);
    }

    public void rollAll() {
        long now = System.currentTimeMillis();
        for (LootChestSpot spot : spots.values()) {
            if (!spot.canRoll(now)) {
                continue;
            }
            spot.markRolled(now);
            if (ThreadLocalRandom.current().nextDouble() <= spot.chance()) {
                spawnSpot(spot, false);
            }
        }
    }

    public int spotCount() {
        return spots.size();
    }

    public List<String> describeSpots() {
        List<String> descriptions = new ArrayList<>();
        for (LootChestSpot spot : spots.values()) {
            descriptions.add(spot.id() + " (" + spot.worldName() + " "
                    + spot.x() + ", " + spot.y() + ", " + spot.z()
                    + " / table=" + spot.table() + " / chance=" + spot.chance() + ")");
        }
        return descriptions;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof LootEditorHolder holder)) {
            return;
        }
        saveTable(holder.tableId(), event.getInventory().getContents());
        player.sendMessage(plugin.msg("chest-table-saved").replace("%table%", holder.tableId()));
    }

    private void loadSpots() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("loot-chests.spots");
        if (root == null) {
            return;
        }
        double defaultChance = plugin.getConfig().getDouble("loot-chests.default-chance", 0.25D);
        long defaultRespawn = plugin.getConfig().getLong("loot-chests.default-respawn-seconds", 1800L);
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String world = section.getString("world", "");
            String table = section.getString("table", "default");
            if (world.isBlank() || table.isBlank()) {
                plugin.getLogger().warning("Skipping invalid loot chest spot: " + id);
                continue;
            }
            spots.put(id, new LootChestSpot(
                    id,
                    world,
                    section.getInt("x"),
                    section.getInt("y"),
                    section.getInt("z"),
                    table,
                    section.getDouble("chance", defaultChance),
                    section.getLong("respawn-seconds", defaultRespawn)
            ));
        }
    }

    private boolean spawnSpot(LootChestSpot spot, boolean force) {
        World world = Bukkit.getWorld(spot.worldName());
        if (world == null) {
            return false;
        }
        ItemStack[] contents = loadTable(spot.table());
        if (contents.length == 0) {
            return false;
        }

        Block block = world.getBlockAt(spot.x(), spot.y(), spot.z());
        boolean replaceExisting = plugin.getConfig().getBoolean("loot-chests.replace-existing", false);
        boolean refillExisting = plugin.getConfig().getBoolean("loot-chests.refill-existing-chest", false);
        if (!force && !replaceExisting && block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR && block.getType() != Material.CHEST) {
            return false;
        }
        if (!force && block.getType() == Material.CHEST && !refillExisting) {
            return false;
        }

        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            chest.getInventory().clear();
            chest.getInventory().setContents(ItemSerialization.fit(contents, chest.getInventory().getSize()));
            chest.update(true);
            return true;
        }
        return false;
    }

    private ItemStack[] loadTable(String tableId) {
        String raw = plugin.getConfig().getString("loot-chests.loot-tables." + tableId + ".contents", "");
        try {
            return ItemSerialization.deserialize(raw);
        } catch (IOException | ClassNotFoundException exception) {
            plugin.getLogger().warning("Failed to load loot table '" + tableId + "': " + exception.getMessage());
            return new ItemStack[0];
        }
    }

    private void saveTable(String tableId, ItemStack[] contents) {
        try {
            plugin.getConfig().set("loot-chests.loot-tables." + tableId + ".contents", ItemSerialization.serialize(contents));
            plugin.saveConfig();
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save loot table '" + tableId + "': " + exception.getMessage());
        }
    }
}
