package me.leeseol.core.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.leeseol.core.LeeSeolCorePlugin;
import me.leeseol.core.config.CoreConfigWriter;
import me.leeseol.core.portal.PortalCuboidSelection;
import me.leeseol.core.portal.WorldEditSelectionProvider;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class PortalAdminCommand {
    private final LeeSeolCorePlugin plugin;
    private final CoreConfigWriter configWriter;
    private final Map<UUID, PortalSelection> portalSelections = new HashMap<>();

    public PortalAdminCommand(LeeSeolCorePlugin plugin, CoreConfigWriter configWriter) {
        this.plugin = plugin;
        this.configWriter = configWriter;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leeseolcore.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("pos1")) {
            return setPortalPosition(sender, true);
        }
        if (action.equals("pos2")) {
            return setPortalPosition(sender, false);
        }
        if (action.equals("create")) {
            return createPortal(sender, args);
        }
        if (action.equals("remove")) {
            return removePortal(sender, args);
        }
        if (action.equals("list")) {
            return listPortals(sender);
        }
        if (action.equals("clear")) {
            return clearPortalSelection(sender);
        }

        sendHelp(sender);
        return true;
    }

    private boolean setPortalPosition(CommandSender sender, boolean firstPosition) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        BlockPosition position = BlockPosition.from(player.getLocation());
        PortalSelection selection = portalSelections.computeIfAbsent(player.getUniqueId(), ignored -> new PortalSelection());
        if (firstPosition) {
            selection.pos1 = position;
        } else {
            selection.pos2 = position;
        }

        sender.sendMessage(color("&aPortal pos" + (firstPosition ? "1" : "2") + " set: &7" + position.format()));
        return true;
    }

    private boolean createPortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("/leeseolcore portal create <id> <targetServer> [cooldownSeconds]");
            return true;
        }

        PortalCuboidSelection selection = resolvePortalSelection(player);
        if (selection == null) {
            sender.sendMessage(color("&cSelect a WorldEdit region first, or use /leeseolcore portal pos1 and pos2."));
            return true;
        }

        String id = args[2];
        String targetServer = args[3];
        long cooldownSeconds = parseLong(args, 4, 3L);
        String path = "portal-triggers.portals." + id;

        configWriter.set("portal-triggers.enabled", true);
        configWriter.set(path + ".world", selection.getWorldName());
        configWriter.set(path + ".min.x", selection.getMinX());
        configWriter.set(path + ".min.y", selection.getMinY());
        configWriter.set(path + ".min.z", selection.getMinZ());
        configWriter.set(path + ".max.x", selection.getMaxX());
        configWriter.set(path + ".max.y", selection.getMaxY());
        configWriter.set(path + ".max.z", selection.getMaxZ());
        configWriter.set(path + ".cooldownSeconds", cooldownSeconds);
        configWriter.set(path + ".actions", defaultVelocityPortalActions(targetServer));
        configWriter.addEnabledWorld(selection.getWorldName());
        configWriter.saveAndReload();

        sender.sendMessage(color("&aPortal saved: &f" + id + " &7-> &f" + targetServer));
        sender.sendMessage(color("&7" + selection.format()));
        return true;
    }

    private PortalCuboidSelection resolvePortalSelection(Player player) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
            try {
                return WorldEditSelectionProvider.getSelection(player);
            } catch (Throwable ignored) {
                // Fall back to LeeSeolCore's own pos1/pos2 selection when WorldEdit has no complete selection.
            }
        }

        PortalSelection selection = portalSelections.get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null) {
            return null;
        }

        if (!selection.pos1.worldName.equals(selection.pos2.worldName)) {
            return null;
        }

        return new PortalCuboidSelection(
                selection.pos1.worldName,
                selection.pos1.x,
                selection.pos1.y,
                selection.pos1.z,
                selection.pos2.x,
                selection.pos2.y,
                selection.pos2.z
        );
    }

    private List<Map<String, Object>> defaultVelocityPortalActions(String targetServer) {
        List<Map<String, Object>> actions = new ArrayList<>();
        actions.add(action("message", "value", "&aMoving to " + targetServer + "."));
        actions.add(action("sound", "value", "ENTITY_ENDERMAN_TELEPORT"));
        actions.add(action("velocity-server", "target", targetServer));
        return actions;
    }

    private Map<String, Object> action(String type, String key, String value) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", type);
        action.put(key, value);
        return action;
    }

    private boolean clearPortalSelection(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        portalSelections.remove(player.getUniqueId());
        sender.sendMessage(color("&aPortal selection cleared."));
        return true;
    }

    private boolean removePortal(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/leeseolcore portal remove <id>");
            return true;
        }

        String id = args[2];
        String path = "portal-triggers.portals." + id;
        if (!configWriter.isConfigurationSection(path)) {
            sender.sendMessage(color("&cPortal not found: &f" + id));
            return true;
        }

        configWriter.set(path, null);
        configWriter.saveAndReload();
        sender.sendMessage(color("&aPortal removed: &f" + id));
        return true;
    }

    private boolean listPortals(CommandSender sender) {
        ConfigurationSection portals = configWriter.section("portal-triggers.portals");
        if (portals == null || portals.getKeys(false).isEmpty()) {
            sender.sendMessage("No portals configured.");
            return true;
        }

        sender.sendMessage(color("&aPortals:"));
        for (String id : portals.getKeys(false)) {
            String base = "portal-triggers.portals." + id;
            sender.sendMessage(color("&7- &f" + id + " &8("
                    + configWriter.getString(base + ".world", "?") + " "
                    + configWriter.getInt(base + ".min.x") + ", "
                    + configWriter.getInt(base + ".min.y") + ", "
                    + configWriter.getInt(base + ".min.z") + " -> "
                    + configWriter.getInt(base + ".max.x") + ", "
                    + configWriter.getInt(base + ".max.y") + ", "
                    + configWriter.getInt(base + ".max.z") + ")"));
        }
        return true;
    }

    private long parseLong(String[] args, int index, long fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/leeseolcore portal pos1");
        sender.sendMessage("/leeseolcore portal pos2");
        sender.sendMessage("/leeseolcore portal create <id> <targetServer> [cooldownSeconds]");
        sender.sendMessage("/leeseolcore portal list");
        sender.sendMessage("/leeseolcore portal remove <id>");
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static final class PortalSelection {
        private BlockPosition pos1;
        private BlockPosition pos2;
    }

    private static final class BlockPosition {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;

        private BlockPosition(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static BlockPosition from(Location location) {
            return new BlockPosition(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }

        private String format() {
            return worldName + " " + x + ", " + y + ", " + z;
        }
    }
}
