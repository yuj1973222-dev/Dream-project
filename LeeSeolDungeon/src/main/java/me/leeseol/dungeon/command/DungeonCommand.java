package me.leeseol.dungeon.command;

import me.leeseol.dungeon.LeeSeolDungeonPlugin;
import me.leeseol.dungeon.loot.LootChestManager;
import me.leeseol.dungeon.portal.DungeonPortalManager;
import me.leeseol.dungeon.util.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DungeonCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolDungeonPlugin plugin;
    private final DungeonPortalManager portalManager;
    private final LootChestManager lootChestManager;
    private final Map<UUID, Selection> selections = new HashMap<>();

    public DungeonCommand(LeeSeolDungeonPlugin plugin, DungeonPortalManager portalManager, LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.lootChestManager = lootChestManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String root = args[0].toLowerCase();
        return switch (root) {
            case "reload" -> reload(sender);
            case "enter" -> enterDungeon(sender);
            case "exit" -> exitDungeon(sender);
            case "portal" -> handlePortal(sender, args);
            case "chest" -> handleChest(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "enter", "exit", "portal", "chest"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("portal")) {
            return filter(List.of("pos1", "pos2", "create", "remove", "list"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("chest")) {
            return filter(List.of("table", "addspot", "removespot", "spawn", "roll", "list"), args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("create")) {
            return filter(List.of(plugin.worldManager().dungeonWorldName(), "return", plugin.worldManager().returnWorldName()), args[3]);
        }
        return List.of();
    }

    private boolean reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        plugin.reloadDungeon();
        sender.sendMessage(plugin.msg("reloaded"));
        return true;
    }

    private boolean enterDungeon(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        portalManager.teleportToDungeon(player);
        sender.sendMessage(plugin.msg("dungeon-entered"));
        return true;
    }

    private boolean exitDungeon(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        portalManager.teleportToReturnSpawn(player);
        sender.sendMessage(plugin.msg("dungeon-exited"));
        return true;
    }

    private boolean handlePortal(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 2) {
            sendPortalHelp(sender);
            return true;
        }
        String action = args[1].toLowerCase();
        return switch (action) {
            case "pos1" -> setPortalPosition(sender, true);
            case "pos2" -> setPortalPosition(sender, false);
            case "create" -> createPortal(sender, args);
            case "remove" -> removePortal(sender, args);
            case "list" -> listPortals(sender);
            default -> {
                sendPortalHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleChest(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 2) {
            sendChestHelp(sender);
            return true;
        }
        String action = args[1].toLowerCase();
        return switch (action) {
            case "table" -> openLootTable(sender, args);
            case "addspot" -> addChestSpot(sender, args);
            case "removespot" -> removeChestSpot(sender, args);
            case "spawn" -> spawnChest(sender, args);
            case "roll" -> rollChests(sender);
            case "list" -> listChests(sender);
            default -> {
                sendChestHelp(sender);
                yield true;
            }
        };
    }

    private boolean setPortalPosition(CommandSender sender, boolean first) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        BlockPos pos = BlockPos.from(player.getLocation());
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection());
        if (first) {
            selection.pos1 = pos;
            sender.sendMessage(plugin.msg("portal-pos1").replace("%pos%", pos.format()));
        } else {
            selection.pos2 = pos;
            sender.sendMessage(plugin.msg("portal-pos2").replace("%pos%", pos.format()));
        }
        return true;
    }

    private boolean createPortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(Text.color("&c/dungeon portal create <id> <targetWorld|return> [cooldownSeconds]"));
            return true;
        }
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null || !selection.pos1.worldName.equals(selection.pos2.worldName)) {
            sender.sendMessage(plugin.msg("portal-missing-selection"));
            return true;
        }

        String id = args[2];
        String target = args[3];
        long cooldown = parseLong(args, 4, plugin.getConfig().getLong("portal-triggers.cooldown-seconds", 3L));
        BlockPos pos1 = selection.pos1;
        BlockPos pos2 = selection.pos2;
        String base = "portal-triggers.portals." + id;
        plugin.getConfig().set("portal-triggers.enabled", true);
        plugin.getConfig().set(base + ".world", pos1.worldName);
        plugin.getConfig().set(base + ".target-world", target);
        plugin.getConfig().set(base + ".min.x", Math.min(pos1.x, pos2.x));
        plugin.getConfig().set(base + ".min.y", Math.min(pos1.y, pos2.y));
        plugin.getConfig().set(base + ".min.z", Math.min(pos1.z, pos2.z));
        plugin.getConfig().set(base + ".max.x", Math.max(pos1.x, pos2.x));
        plugin.getConfig().set(base + ".max.y", Math.max(pos1.y, pos2.y));
        plugin.getConfig().set(base + ".max.z", Math.max(pos1.z, pos2.z));
        plugin.getConfig().set(base + ".cooldown-seconds", cooldown);
        plugin.getConfig().set(base + ".message", "&a던전으로 이동합니다.");
        plugin.getConfig().set(base + ".sound", "ENTITY_ENDERMAN_TELEPORT");
        plugin.saveConfig();
        plugin.reloadDungeon();
        sender.sendMessage(plugin.msg("portal-created").replace("%id%", id).replace("%target%", target));
        return true;
    }

    private boolean removePortal(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.color("&c/dungeon portal remove <id>"));
            return true;
        }
        String id = args[2];
        String path = "portal-triggers.portals." + id;
        if (!plugin.getConfig().isConfigurationSection(path)) {
            sender.sendMessage(Text.color("&c포탈을 찾을 수 없습니다: &f" + id));
            return true;
        }
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        plugin.reloadDungeon();
        sender.sendMessage(plugin.msg("portal-removed").replace("%id%", id));
        return true;
    }

    private boolean listPortals(CommandSender sender) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("portal-triggers.portals");
        if (root == null || root.getKeys(false).isEmpty()) {
            sender.sendMessage(Text.color("&7등록된 던전 포탈이 없습니다."));
            return true;
        }
        sender.sendMessage(Text.color("&b던전 포탈 목록"));
        for (String id : root.getKeys(false)) {
            String target = plugin.getConfig().getString("portal-triggers.portals." + id + ".target-world",
                    plugin.getConfig().getString("portal-triggers.portals." + id + ".target-server", "?"));
            sender.sendMessage(Text.color("&7- &f" + id + " &7-> &e" + target));
        }
        return true;
    }

    private boolean openLootTable(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.color("&c/dungeon chest table <tableId>"));
            return true;
        }
        lootChestManager.openTableEditor(player, args[2]);
        return true;
    }

    private boolean addChestSpot(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(Text.color("&c/dungeon chest addspot <id> <tableId> [chance] [respawnSeconds]"));
            return true;
        }
        double chance = parseDouble(args, 4, plugin.getConfig().getDouble("loot-chests.default-chance", 0.25D));
        long respawn = parseLong(args, 5, plugin.getConfig().getLong("loot-chests.default-respawn-seconds", 1800L));
        lootChestManager.addSpot(player, args[2], args[3], chance, respawn);
        return true;
    }

    private boolean removeChestSpot(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.color("&c/dungeon chest removespot <id>"));
            return true;
        }
        String id = args[2];
        if (!lootChestManager.removeSpot(id)) {
            sender.sendMessage(plugin.msg("chest-not-found").replace("%id%", id));
            return true;
        }
        sender.sendMessage(plugin.msg("chest-spot-removed").replace("%id%", id));
        return true;
    }

    private boolean spawnChest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.color("&c/dungeon chest spawn <id>"));
            return true;
        }
        String id = args[2];
        if (!lootChestManager.spawnSpot(id, true)) {
            sender.sendMessage(plugin.msg("chest-not-found").replace("%id%", id));
            return true;
        }
        sender.sendMessage(plugin.msg("chest-spawned").replace("%id%", id));
        return true;
    }

    private boolean rollChests(CommandSender sender) {
        lootChestManager.rollAll();
        sender.sendMessage(Text.color("&a등록된 던전 상자 위치를 확률 계산했습니다."));
        return true;
    }

    private boolean listChests(CommandSender sender) {
        List<String> descriptions = lootChestManager.describeSpots();
        if (descriptions.isEmpty()) {
            sender.sendMessage(Text.color("&7등록된 던전 상자 위치가 없습니다."));
            return true;
        }
        sender.sendMessage(Text.color("&b던전 상자 위치 목록"));
        for (String description : descriptions) {
            sender.sendMessage(Text.color("&7- &f" + description));
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("leeseoldungeon.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return false;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&b/dungeon reload"));
        sender.sendMessage(Text.color("&b/dungeon enter"));
        sender.sendMessage(Text.color("&b/dungeon exit"));
        sendPortalHelp(sender);
        sendChestHelp(sender);
    }

    private void sendPortalHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&b/dungeon portal pos1"));
        sender.sendMessage(Text.color("&b/dungeon portal pos2"));
        sender.sendMessage(Text.color("&b/dungeon portal create <id> <targetWorld|return> [cooldownSeconds]"));
        sender.sendMessage(Text.color("&b/dungeon portal list"));
        sender.sendMessage(Text.color("&b/dungeon portal remove <id>"));
    }

    private void sendChestHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&b/dungeon chest table <tableId>"));
        sender.sendMessage(Text.color("&b/dungeon chest addspot <id> <tableId> [chance] [respawnSeconds]"));
        sender.sendMessage(Text.color("&b/dungeon chest list"));
        sender.sendMessage(Text.color("&b/dungeon chest spawn <id>"));
        sender.sendMessage(Text.color("&b/dungeon chest roll"));
        sender.sendMessage(Text.color("&b/dungeon chest removespot <id>"));
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase();
        return values.stream().filter(value -> value.toLowerCase().startsWith(lower)).toList();
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

    private static final class Selection {
        private BlockPos pos1;
        private BlockPos pos2;
    }

    private record BlockPos(String worldName, int x, int y, int z) {
        private static BlockPos from(Location location) {
            return new BlockPos(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        private String format() {
            return worldName + " " + x + ", " + y + ", " + z;
        }
    }
}
