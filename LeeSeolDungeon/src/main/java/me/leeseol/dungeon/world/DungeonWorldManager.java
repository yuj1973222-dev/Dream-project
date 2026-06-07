package me.leeseol.dungeon.world;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.Locale;

public final class DungeonWorldManager {
    private final LeeSeolDungeonPlugin plugin;
    private String dungeonWorldName = "dungeon";
    private String returnWorldName = "world";

    public DungeonWorldManager(LeeSeolDungeonPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        dungeonWorldName = plugin.getConfig().getString("dungeon-world.name", "dungeon");
        returnWorldName = plugin.getConfig().getString("dungeon-world.return-world", "world");

        if (!plugin.getConfig().getBoolean("dungeon-world.enabled", true)) {
            return;
        }

        World world = Bukkit.getWorld(dungeonWorldName);
        if (world == null) {
            world = createWorld();
        }
        if (world != null) {
            applySpawn(world);
            plugin.getLogger().info("Dungeon world ready: " + world.getName());
        }
    }

    public String dungeonWorldName() {
        return dungeonWorldName;
    }

    public String returnWorldName() {
        return returnWorldName;
    }

    public boolean isDungeonWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(dungeonWorldName);
    }

    public World dungeonWorld() {
        return Bukkit.getWorld(dungeonWorldName);
    }

    public World returnWorld() {
        return Bukkit.getWorld(returnWorldName);
    }

    public Location dungeonSpawn() {
        World world = dungeonWorld();
        if (world == null) {
            return null;
        }
        return configuredLocation("dungeon-world.spawn", world, world.getSpawnLocation());
    }

    public Location returnSpawn() {
        World world = returnWorld();
        if (world == null) {
            return null;
        }
        return configuredLocation("dungeon-world.return-spawn", world, world.getSpawnLocation());
    }

    private World createWorld() {
        try {
            WorldCreator creator = new WorldCreator(dungeonWorldName);
            creator.environment(parseEnvironment(plugin.getConfig().getString("dungeon-world.environment", "NORMAL")));
            WorldType worldType = parseWorldType(plugin.getConfig().getString("dungeon-world.type", "NORMAL"));
            if (worldType != null) {
                creator.type(worldType);
            }
            creator.generateStructures(plugin.getConfig().getBoolean("dungeon-world.generate-structures", true));
            String seed = plugin.getConfig().getString("dungeon-world.seed", "");
            if (seed != null && !seed.isBlank()) {
                creator.seed(seed.hashCode());
            }
            return Bukkit.createWorld(creator);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Failed to create dungeon world: " + exception.getMessage());
            return null;
        }
    }

    private void applySpawn(World world) {
        Location spawn = configuredLocation("dungeon-world.spawn", world, null);
        if (spawn != null) {
            world.setSpawnLocation(spawn);
        }
    }

    private Location configuredLocation(String path, World world, Location fallback) {
        if (!plugin.getConfig().isConfigurationSection(path)) {
            return fallback;
        }
        double x = plugin.getConfig().getDouble(path + ".x", fallback == null ? 0.5D : fallback.getX());
        double y = plugin.getConfig().getDouble(path + ".y", fallback == null ? 65.0D : fallback.getY());
        double z = plugin.getConfig().getDouble(path + ".z", fallback == null ? 0.5D : fallback.getZ());
        float yaw = (float) plugin.getConfig().getDouble(path + ".yaw", fallback == null ? 0.0D : fallback.getYaw());
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", fallback == null ? 0.0D : fallback.getPitch());
        return new Location(world, x, y, z, yaw, pitch);
    }

    private World.Environment parseEnvironment(String raw) {
        try {
            return World.Environment.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid dungeon-world.environment: " + raw + ", using NORMAL");
            return World.Environment.NORMAL;
        }
    }

    private WorldType parseWorldType(String raw) {
        if (raw == null || raw.isBlank()) {
            return WorldType.NORMAL;
        }
        try {
            return WorldType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid dungeon-world.type: " + raw + ", using NORMAL");
            return WorldType.NORMAL;
        }
    }
}
