package me.leeseol.dungeon.portal;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import me.leeseol.dungeon.inventory.InventorySyncManager;
import me.leeseol.dungeon.returning.ReturnLocationManager;
import me.leeseol.dungeon.util.Text;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DungeonPortalManager {
    private final LeeSeolDungeonPlugin plugin;
    private final InventorySyncManager inventorySyncManager;
    private final ReturnLocationManager returnLocationManager;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private List<DungeonPortal> portals = List.of();
    private boolean enabled;

    public DungeonPortalManager(
            LeeSeolDungeonPlugin plugin,
            InventorySyncManager inventorySyncManager,
            ReturnLocationManager returnLocationManager
    ) {
        this.plugin = plugin;
        this.inventorySyncManager = inventorySyncManager;
        this.returnLocationManager = returnLocationManager;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("portal-triggers.enabled", true);
        cooldowns.clear();
        if (!enabled) {
            portals = List.of();
            return;
        }

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("portal-triggers.portals");
        if (root == null) {
            portals = List.of();
            return;
        }

        List<DungeonPortal> loaded = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            DungeonPortal portal = section == null ? null : loadPortal(id, section);
            if (portal != null) {
                loaded.add(portal);
            }
        }
        portals = List.copyOf(loaded);
    }

    public void handleMove(Player player, Location location) {
        if (!enabled || location == null || location.getWorld() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String worldName = location.getWorld().getName();
        for (DungeonPortal portal : portals) {
            if (!portal.contains(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                continue;
            }
            if (isCoolingDown(player.getUniqueId(), portal, now)) {
                continue;
            }
            markCooldown(player.getUniqueId(), portal, now);
            transfer(player, portal);
            return;
        }
    }

    public int portalCount() {
        return portals.size();
    }

    public void teleportToDungeon(Player player) {
        Location target = plugin.worldManager().dungeonSpawn();
        if (target == null) {
            player.sendMessage(plugin.msg("target-world-missing").replace("%world%", plugin.worldManager().dungeonWorldName()));
            return;
        }
        teleport(player, target);
    }

    public void teleportToReturn(Player player) {
        if (!returnLocationManager.consumeReturn(player)) {
            teleport(player, plugin.worldManager().returnSpawn());
        }
    }

    public void teleportToReturnSpawn(Player player) {
        teleport(player, plugin.worldManager().returnSpawn());
    }

    private DungeonPortal loadPortal(String id, ConfigurationSection section) {
        String world = section.getString("world", "");
        String targetWorld = section.getString("target-world", section.getString("target-server", ""));
        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");
        if (world.isBlank() || targetWorld.isBlank() || min == null || max == null) {
            plugin.getLogger().warning("Skipping invalid dungeon portal: " + id);
            return null;
        }
        long fallbackCooldown = plugin.getConfig().getLong("portal-triggers.cooldown-seconds", 3L);
        return new DungeonPortal(
                id,
                world,
                min.getInt("x"),
                min.getInt("y"),
                min.getInt("z"),
                max.getInt("x"),
                max.getInt("y"),
                max.getInt("z"),
                targetWorld,
                section.getLong("cooldown-seconds", fallbackCooldown),
                section.getString("message", "&a던전으로 이동합니다."),
                section.getString("sound", "ENTITY_ENDERMAN_TELEPORT")
        );
    }

    private boolean isCoolingDown(UUID playerId, DungeonPortal portal, long now) {
        long last = cooldowns.getOrDefault(playerId, Map.of()).getOrDefault(portal.id(), 0L);
        return now - last < portal.cooldownMillis();
    }

    private void markCooldown(UUID playerId, DungeonPortal portal, long now) {
        cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(portal.id(), now);
    }

    private void transfer(Player player, DungeonPortal portal) {
        if (!portal.message().isBlank()) {
            player.sendMessage(Text.color(portal.message()));
        }
        playSound(player, portal.sound());
        inventorySyncManager.saveBeforeTransfer(player);

        String dungeonWorldName = plugin.worldManager().dungeonWorldName();
        if (portal.targetWorld().equalsIgnoreCase(dungeonWorldName)) {
            returnLocationManager.prepareReturn(player, portal);
            teleport(player, plugin.worldManager().dungeonSpawn());
            return;
        }

        if (portal.targetWorld().equalsIgnoreCase("return")) {
            teleportToReturn(player);
            return;
        }

        World targetWorld = plugin.getServer().getWorld(portal.targetWorld());
        if (targetWorld == null) {
            plugin.getLogger().warning("Dungeon portal target world is not loaded: " + portal.targetWorld());
            player.sendMessage(plugin.msg("target-world-missing").replace("%world%", portal.targetWorld()));
            return;
        }
        teleport(player, targetWorld.getSpawnLocation());
    }

    private void teleport(Player player, Location target) {
        if (target == null || target.getWorld() == null) {
            player.sendMessage(plugin.msg("target-world-missing").replace("%world%", "?"));
            return;
        }
        player.teleport(target);
    }

    private void playSound(Player player, String rawSound) {
        if (rawSound == null || rawSound.isBlank()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(rawSound.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid dungeon portal sound: " + rawSound);
        }
    }
}
