package me.leeseol.core.launchpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class LaunchPadManager {
    private final LeeSeolCorePlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private List<LaunchPad> pads = List.of();
    private boolean enabled;

    public LaunchPadManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("launch-pads.enabled", false);
        cooldowns.clear();

        if (!enabled) {
            pads = List.of();
            plugin.getLogger().info("LaunchPad disabled.");
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("launch-pads.pads");
        if (section == null) {
            pads = List.of();
            plugin.getLogger().info("LaunchPad loaded 0 pad(s).");
            return;
        }

        List<LaunchPad> loaded = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection padSection = section.getConfigurationSection(id);
            if (padSection == null) {
                continue;
            }

            LaunchPad pad = loadPad(id, padSection);
            if (pad != null) {
                loaded.add(pad);
            }
        }

        pads = List.copyOf(loaded);
        plugin.getLogger().info("LaunchPad loaded " + pads.size() + " pad(s).");
    }

    public void handleMove(Player player, Location location) {
        if (!enabled || location == null || location.getWorld() == null) {
            return;
        }

        Block currentBlock = location.getBlock();
        Block belowBlock = location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();

        for (LaunchPad pad : pads) {
            if (tryLaunch(player, pad, currentBlock) || tryLaunch(player, pad, belowBlock)) {
                return;
            }
        }
    }

    private LaunchPad loadPad(String id, ConfigurationSection section) {
        String world = section.getString("world", "");
        if (world.isBlank()) {
            plugin.getLogger().warning("Skipping launch pad '" + id + "': world is empty.");
            return null;
        }

        Material block = parseMaterial(id, section.getString("block", ""));
        if (block == null) {
            return null;
        }

        return new LaunchPad(
                id,
                world,
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z"),
                block,
                section.getDouble("forward", 1.8D),
                section.getDouble("upward", 1.2D),
                section.getLong("cooldownSeconds", 1L),
                section.getString("message", ""),
                parseSound(id, section.getString("sound", "")),
                parseParticle(id, section.getString("particle", ""))
        );
    }

    private boolean tryLaunch(Player player, LaunchPad pad, Block block) {
        if (block == null
                || !pad.matches(block.getWorld().getName(), block.getX(), block.getY(), block.getZ())
                || block.getType() != pad.getBlock()
                || isCoolingDown(player.getUniqueId(), pad)) {
            return false;
        }

        markCooldown(player.getUniqueId(), pad);
        launch(player, pad);
        return true;
    }

    private void launch(Player player, LaunchPad pad) {
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);

        if (direction.lengthSquared() > 0.0001D) {
            direction.normalize().multiply(pad.getForward());
        } else {
            direction = new Vector(0.0D, 0.0D, 0.0D);
        }

        direction.setY(pad.getUpward());
        player.setVelocity(direction);

        if (pad.getSound() != null) {
            player.playSound(player.getLocation(), pad.getSound(), 1.0F, 1.0F);
        }

        if (pad.getParticle() != null) {
            player.getWorld().spawnParticle(pad.getParticle(), player.getLocation(), 20, 0.35D, 0.35D, 0.35D, 0.01D);
        }

        if (pad.getMessage() != null && !pad.getMessage().isBlank()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', pad.getMessage()));
        }
    }

    private boolean isCoolingDown(UUID playerId, LaunchPad pad) {
        long lastUsed = cooldowns
                .getOrDefault(playerId, Map.of())
                .getOrDefault(pad.getId(), 0L);
        return System.currentTimeMillis() - lastUsed < pad.getCooldownMillis();
    }

    private void markCooldown(UUID playerId, LaunchPad pad) {
        cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(pad.getId(), System.currentTimeMillis());
    }

    private Material parseMaterial(String id, String rawMaterial) {
        if (rawMaterial == null || rawMaterial.isBlank()) {
            plugin.getLogger().warning("Skipping launch pad '" + id + "': block material is empty.");
            return null;
        }

        Material material = Material.matchMaterial(rawMaterial);
        if (material == null) {
            plugin.getLogger().warning("Skipping launch pad '" + id + "': invalid material '" + rawMaterial + "'.");
        }
        return material;
    }

    private Sound parseSound(String id, String rawSound) {
        if (rawSound == null || rawSound.isBlank()) {
            return null;
        }

        try {
            return Sound.valueOf(rawSound.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Launch pad '" + id + "' has invalid sound '" + rawSound + "'.");
            return null;
        }
    }

    private Particle parseParticle(String id, String rawParticle) {
        if (rawParticle == null || rawParticle.isBlank()) {
            return null;
        }

        try {
            return Particle.valueOf(rawParticle.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Launch pad '" + id + "' has invalid particle '" + rawParticle + "'.");
            return null;
        }
    }
}
