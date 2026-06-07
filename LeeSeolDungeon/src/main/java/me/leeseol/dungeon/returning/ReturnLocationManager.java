package me.leeseol.dungeon.returning;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import me.leeseol.dungeon.portal.DungeonPortal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public final class ReturnLocationManager implements Listener {
    private final LeeSeolDungeonPlugin plugin;
    private File dataFile;
    private YamlConfiguration data;
    private boolean enabled;

    public ReturnLocationManager(LeeSeolDungeonPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("return-random.enabled", true);
        dataFile = new File(plugin.getConfig().getString("return-random.data-file", "/opt/minecraft/shared/dungeon/returns.yml"));
        if (dataFile.getParentFile() != null) {
            dataFile.getParentFile().mkdirs();
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void prepareReturn(Player player, DungeonPortal portal) {
        if (!enabled || data == null || !plugin.roleAllows("survival")) {
            return;
        }
        int radius = Math.max(1, plugin.getConfig().getInt("return-random.radius", 50));
        String path = "players." + player.getUniqueId();
        data.set(path + ".world", portal.worldName());
        data.set(path + ".center-x", portal.centerX());
        data.set(path + ".center-z", portal.centerZ());
        data.set(path + ".radius", radius);
        data.set(path + ".created-at", System.currentTimeMillis());
        save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled || !plugin.roleAllows("survival")) {
            return;
        }
        long delay = Math.max(1L, plugin.getConfig().getLong("return-random.delay-ticks", 30L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> consumeReturn(event.getPlayer()), delay);
    }

    public boolean consumeReturn(Player player) {
        if (!enabled || data == null) {
            return false;
        }
        String path = "players." + player.getUniqueId();
        if (!data.isConfigurationSection(path)) {
            return false;
        }

        World world = Bukkit.getWorld(data.getString(path + ".world", "world"));
        int centerX = data.getInt(path + ".center-x");
        int centerZ = data.getInt(path + ".center-z");
        int radius = Math.max(1, data.getInt(path + ".radius", 50));
        data.set(path, null);
        save();

        Location target = findSafeLocation(world, centerX, centerZ, radius);
        if (target != null) {
            player.teleport(target);
            player.sendMessage(plugin.msg("return-teleported"));
            return true;
        }
        return false;
    }

    private Location findSafeLocation(World world, int centerX, int centerZ, int radius) {
        if (world == null) {
            return null;
        }
        int attempts = Math.max(1, plugin.getConfig().getInt("return-random.max-attempts", 50));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = centerX + random.nextInt(-radius, radius + 1);
            int z = centerZ + random.nextInt(-radius, radius + 1);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5D, y, z + 0.5D);
            if (isSafe(candidate)) {
                return candidate;
            }
        }
        int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
        return new Location(world, centerX + 0.5D, y, centerZ + 0.5D);
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);
        return below.getType().isSolid()
                && feet.getType().isAir()
                && head.getType().isAir()
                && below.getType() != Material.LAVA
                && below.getType() != Material.MAGMA_BLOCK
                && below.getType() != Material.CACTUS;
    }

    public void save() {
        if (data == null || dataFile == null) {
            return;
        }
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save dungeon return data: " + exception.getMessage());
        }
    }
}
