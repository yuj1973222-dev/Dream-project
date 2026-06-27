package me.leeseol.core.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class PortalManager {
    private final LeeSeolCorePlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private List<PortalTrigger> portals = List.of();
    private boolean enabled;

    public PortalManager(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("portal-triggers.enabled", false);
        cooldowns.clear();

        if (!enabled) {
            portals = List.of();
            plugin.getLogger().info("PortalTrigger disabled.");
            return;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("portal-triggers.portals");
        if (section == null) {
            portals = List.of();
            plugin.getLogger().info("PortalTrigger loaded 0 portals.");
            return;
        }

        List<PortalTrigger> loaded = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection portalSection = section.getConfigurationSection(id);
            if (portalSection == null) {
                continue;
            }

            PortalTrigger portal = loadPortal(id, portalSection);
            if (portal != null) {
                loaded.add(portal);
            }
        }

        portals = List.copyOf(loaded);
        plugin.getLogger().info("PortalTrigger loaded " + portals.size() + " portal(s).");
    }

    public void handleMove(Player player, Location location) {
        if (!enabled || location == null || location.getWorld() == null) {
            return;
        }

        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        long now = System.currentTimeMillis();

        for (PortalTrigger portal : portals) {
            if (!portal.contains(worldName, x, y, z) || isCoolingDown(player.getUniqueId(), portal, now)) {
                continue;
            }

            markCooldown(player.getUniqueId(), portal, now);
            runActions(player, portal);
        }
    }

    private PortalTrigger loadPortal(String id, ConfigurationSection section) {
        String world = section.getString("world", "");
        if (world.isBlank()) {
            plugin.getLogger().warning("Skipping portal '" + id + "': world is empty.");
            return null;
        }

        ConfigurationSection min = section.getConfigurationSection("min");
        ConfigurationSection max = section.getConfigurationSection("max");
        if (min == null || max == null) {
            plugin.getLogger().warning("Skipping portal '" + id + "': min/max section is missing.");
            return null;
        }

        List<PortalAction> actions = loadActions(section);
        if (actions.isEmpty()) {
            plugin.getLogger().warning("Skipping portal '" + id + "': no valid actions.");
            return null;
        }

        return new PortalTrigger(
                id,
                world,
                min.getInt("x"),
                min.getInt("y"),
                min.getInt("z"),
                max.getInt("x"),
                max.getInt("y"),
                max.getInt("z"),
                section.getLong("cooldownSeconds", 3L),
                actions
        );
    }

    private List<PortalAction> loadActions(ConfigurationSection section) {
        List<PortalAction> actions = new ArrayList<>();
        List<?> rawActions = section.getList("actions", List.of());

        for (Object raw : rawActions) {
            PortalAction action = null;
            if (raw instanceof Map<?, ?> map) {
                action = PortalAction.fromMap(map);
            } else if (raw instanceof ConfigurationSection actionSection) {
                action = PortalAction.fromSection(actionSection);
            }

            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    private boolean isCoolingDown(UUID playerId, PortalTrigger portal, long now) {
        long lastUsed = cooldowns
                .getOrDefault(playerId, Map.of())
                .getOrDefault(portal.getId(), 0L);
        return now - lastUsed < portal.getCooldownMillis();
    }

    private void markCooldown(UUID playerId, PortalTrigger portal, long now) {
        cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(portal.getId(), now);
    }

    private void runActions(Player player, PortalTrigger portal) {
        for (PortalAction action : portal.getActions()) {
            runAction(player, action);
        }
    }

    private void runAction(Player player, PortalAction action) {
        switch (action.getType()) {
            case CONSOLE_COMMAND -> runConsoleCommand(player, action.getCommand());
            case PLAYER_COMMAND -> runPlayerCommand(player, action.getCommand());
            case VELOCITY_SERVER -> connectToVelocityServer(player, action.getTarget());
            case MESSAGE -> player.sendMessage(color(replacePlayer(action.getValue(), player)));
            case SOUND -> playSound(player, action.getValue());
            case TITLE -> showTitle(player, action);
        }
    }

    private void runConsoleCommand(Player player, String command) {
        String parsedCommand = stripSlash(replacePlayer(command, player));
        if (!parsedCommand.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }

    private void runPlayerCommand(Player player, String command) {
        String parsedCommand = stripSlash(replacePlayer(command, player));
        if (!parsedCommand.isBlank()) {
            player.performCommand(parsedCommand);
        }
    }

    private void connectToVelocityServer(Player player, String targetServer) {
        plugin.sendPlayerToServer(player, targetServer);

        // Velocity 환경에서 동작 확인 필요: BungeeCord 호환 Connect subchannel을 사용한다.
    }

    private void playSound(Player player, String rawSound) {
        if (rawSound == null || rawSound.isBlank()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(rawSound.toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid portal sound '" + rawSound + "'.");
        }
    }

    private void showTitle(Player player, PortalAction action) {
        String title = action.getTitle().isBlank() ? action.getValue() : action.getTitle();
        player.sendTitle(
                color(replacePlayer(title, player)),
                color(replacePlayer(action.getSubtitle(), player)),
                action.getFadeIn(),
                action.getStay(),
                action.getFadeOut()
        );
    }

    private String replacePlayer(String value, Player player) {
        return value == null ? "" : value.replace("%player%", player.getName());
    }

    private String stripSlash(String command) {
        if (command == null) {
            return "";
        }
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
