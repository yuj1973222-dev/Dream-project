package me.leeseol.core.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.leeseol.core.LeeSeolCorePlugin;
import me.leeseol.core.portal.PortalCuboidSelection;
import me.leeseol.core.portal.WorldEditSelectionProvider;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class CoreCommand implements CommandExecutor {
    private final LeeSeolCorePlugin plugin;
    private final Map<UUID, PortalSelection> portalSelections = new HashMap<>();

    public CoreCommand(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("leeseolcore.admin")) {
                sender.sendMessage("You do not have permission.");
                return true;
            }

            plugin.reloadCoreConfig();
            sender.sendMessage("LeeSeolCore config reloaded.");
            return true;
        }

        if (args.length >= 2 && isLaunchPadCommand(args[0])) {
            return handleLaunchPadCommand(sender, args);
        }

        if (args.length >= 2 && isPortalCommand(args[0])) {
            return handlePortalCommand(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    private boolean handleLaunchPadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("leeseolcore.admin")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("set")) {
            return setLaunchPad(sender, args);
        }
        if (action.equals("remove")) {
            return removeLaunchPad(sender, args);
        }
        if (action.equals("list")) {
            return listLaunchPads(sender);
        }

        sendHelp(sender);
        return true;
    }

    private boolean handlePortalCommand(CommandSender sender, String[] args) {
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

    private boolean setLaunchPad(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in game.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("/leeseolcore launchpad set <id> [forward] [upward] [cooldownSeconds]");
            return true;
        }

        String id = args[2];
        double forward = parseDouble(args, 3, 1.8D);
        double upward = parseDouble(args, 4, 1.2D);
        long cooldownSeconds = parseLong(args, 5, 1L);
        Block block = findLaunchPadBlock(player);

        plugin.getConfig().set("launch-pads.enabled", true);
        plugin.getConfig().set("launch-pads.pads." + id + ".world", block.getWorld().getName());
        plugin.getConfig().set("launch-pads.pads." + id + ".x", block.getX());
        plugin.getConfig().set("launch-pads.pads." + id + ".y", block.getY());
        plugin.getConfig().set("launch-pads.pads." + id + ".z", block.getZ());
        plugin.getConfig().set("launch-pads.pads." + id + ".block", block.getType().name());
        plugin.getConfig().set("launch-pads.pads." + id + ".forward", forward);
        plugin.getConfig().set("launch-pads.pads." + id + ".upward", upward);
        plugin.getConfig().set("launch-pads.pads." + id + ".cooldownSeconds", cooldownSeconds);
        setIfMissing("launch-pads.pads." + id + ".message", "&bJump!");
        setIfMissing("launch-pads.pads." + id + ".sound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        setIfMissing("launch-pads.pads." + id + ".particle", "CLOUD");
        addEnabledWorld(block.getWorld().getName());

        plugin.saveConfig();
        plugin.reloadCoreConfig();

        sender.sendMessage(color("&aLaunchPad saved: &f" + id));
        sender.sendMessage(color("&7" + block.getWorld().getName() + " "
                + block.getX() + ", " + block.getY() + ", " + block.getZ()
                + " / " + block.getType().name()));
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

        plugin.getConfig().set("portal-triggers.enabled", true);
        plugin.getConfig().set(path + ".world", selection.getWorldName());
        plugin.getConfig().set(path + ".min.x", selection.getMinX());
        plugin.getConfig().set(path + ".min.y", selection.getMinY());
        plugin.getConfig().set(path + ".min.z", selection.getMinZ());
        plugin.getConfig().set(path + ".max.x", selection.getMaxX());
        plugin.getConfig().set(path + ".max.y", selection.getMaxY());
        plugin.getConfig().set(path + ".max.z", selection.getMaxZ());
        plugin.getConfig().set(path + ".cooldownSeconds", cooldownSeconds);
        plugin.getConfig().set(path + ".actions", defaultVelocityPortalActions(targetServer));
        addEnabledWorld(selection.getWorldName());

        plugin.saveConfig();
        plugin.reloadCoreConfig();

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
        if (!plugin.getConfig().isConfigurationSection(path)) {
            sender.sendMessage(color("&cPortal not found: &f" + id));
            return true;
        }

        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        plugin.reloadCoreConfig();
        sender.sendMessage(color("&aPortal removed: &f" + id));
        return true;
    }

    private boolean listPortals(CommandSender sender) {
        ConfigurationSection portals = plugin.getConfig().getConfigurationSection("portal-triggers.portals");
        if (portals == null || portals.getKeys(false).isEmpty()) {
            sender.sendMessage("No portals configured.");
            return true;
        }

        sender.sendMessage(color("&aPortals:"));
        for (String id : portals.getKeys(false)) {
            String base = "portal-triggers.portals." + id;
            sender.sendMessage(color("&7- &f" + id + " &8("
                    + plugin.getConfig().getString(base + ".world", "?") + " "
                    + plugin.getConfig().getInt(base + ".min.x") + ", "
                    + plugin.getConfig().getInt(base + ".min.y") + ", "
                    + plugin.getConfig().getInt(base + ".min.z") + " -> "
                    + plugin.getConfig().getInt(base + ".max.x") + ", "
                    + plugin.getConfig().getInt(base + ".max.y") + ", "
                    + plugin.getConfig().getInt(base + ".max.z") + ")"));
        }
        return true;
    }

    private boolean removeLaunchPad(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/leeseolcore launchpad remove <id>");
            return true;
        }

        String id = args[2];
        String path = "launch-pads.pads." + id;
        if (!plugin.getConfig().isConfigurationSection(path)) {
            sender.sendMessage(color("&cLaunchPad not found: &f" + id));
            return true;
        }

        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        plugin.reloadCoreConfig();
        sender.sendMessage(color("&aLaunchPad removed: &f" + id));
        return true;
    }

    private boolean listLaunchPads(CommandSender sender) {
        ConfigurationSection pads = plugin.getConfig().getConfigurationSection("launch-pads.pads");
        if (pads == null || pads.getKeys(false).isEmpty()) {
            sender.sendMessage("No launch pads configured.");
            return true;
        }

        sender.sendMessage(color("&aLaunchPads:"));
        for (String id : pads.getKeys(false)) {
            String base = "launch-pads.pads." + id;
            sender.sendMessage(color("&7- &f" + id + " &8("
                    + plugin.getConfig().getString(base + ".world", "?") + " "
                    + plugin.getConfig().getInt(base + ".x") + ", "
                    + plugin.getConfig().getInt(base + ".y") + ", "
                    + plugin.getConfig().getInt(base + ".z") + ")"));
        }
        return true;
    }

    private Block findLaunchPadBlock(Player player) {
        Block current = player.getLocation().getBlock();
        if (isPressurePlate(current.getType())) {
            return current;
        }

        Block below = player.getLocation().clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        if (isPressurePlate(below.getType())) {
            return below;
        }

        Block target = player.getTargetBlockExact(6);
        if (target != null) {
            return target;
        }

        return below;
    }

    private boolean isPressurePlate(Material material) {
        return material.name().endsWith("_PRESSURE_PLATE");
    }

    private void addEnabledWorld(String worldName) {
        List<String> worlds = new ArrayList<>(plugin.getConfig().getStringList("enabled-worlds"));
        for (String world : worlds) {
            if (world.equalsIgnoreCase(worldName)) {
                return;
            }
        }
        worlds.add(worldName);
        plugin.getConfig().set("enabled-worlds", worlds);
    }

    private void setIfMissing(String path, Object value) {
        if (!plugin.getConfig().contains(path)) {
            plugin.getConfig().set(path, value);
        }
    }

    private double parseDouble(String[] args, int index, double fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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

    private boolean isLaunchPadCommand(String value) {
        return value.equalsIgnoreCase("launchpad") || value.equalsIgnoreCase("pad");
    }

    private boolean isPortalCommand(String value) {
        return value.equalsIgnoreCase("portal");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/leeseolcore reload");
        sender.sendMessage("/leeseolcore launchpad set <id> [forward] [upward] [cooldownSeconds]");
        sender.sendMessage("/leeseolcore launchpad list");
        sender.sendMessage("/leeseolcore launchpad remove <id>");
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
