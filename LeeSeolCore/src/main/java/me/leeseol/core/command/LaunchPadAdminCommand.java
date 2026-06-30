package me.leeseol.core.command;

import me.leeseol.core.LeeSeolCorePlugin;
import me.leeseol.core.config.CoreConfigWriter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class LaunchPadAdminCommand {
    private final LeeSeolCorePlugin plugin;
    private final CoreConfigWriter configWriter;

    public LaunchPadAdminCommand(LeeSeolCorePlugin plugin, CoreConfigWriter configWriter) {
        this.plugin = plugin;
        this.configWriter = configWriter;
    }

    public boolean handle(CommandSender sender, String[] args) {
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
        String path = "launch-pads.pads." + id;

        configWriter.set("launch-pads.enabled", true);
        configWriter.set(path + ".world", block.getWorld().getName());
        configWriter.set(path + ".x", block.getX());
        configWriter.set(path + ".y", block.getY());
        configWriter.set(path + ".z", block.getZ());
        configWriter.set(path + ".block", block.getType().name());
        configWriter.set(path + ".forward", forward);
        configWriter.set(path + ".upward", upward);
        configWriter.set(path + ".cooldownSeconds", cooldownSeconds);
        configWriter.setIfMissing(path + ".message", "&bJump!");
        configWriter.setIfMissing(path + ".sound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        configWriter.setIfMissing(path + ".particle", "CLOUD");
        configWriter.addEnabledWorld(block.getWorld().getName());
        configWriter.saveAndReload();

        sender.sendMessage(color("&aLaunchPad saved: &f" + id));
        sender.sendMessage(color("&7" + block.getWorld().getName() + " "
                + block.getX() + ", " + block.getY() + ", " + block.getZ()
                + " / " + block.getType().name()));
        return true;
    }

    private boolean removeLaunchPad(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("/leeseolcore launchpad remove <id>");
            return true;
        }

        String id = args[2];
        String path = "launch-pads.pads." + id;
        if (!configWriter.isConfigurationSection(path)) {
            sender.sendMessage(color("&cLaunchPad not found: &f" + id));
            return true;
        }

        configWriter.set(path, null);
        configWriter.saveAndReload();
        sender.sendMessage(color("&aLaunchPad removed: &f" + id));
        return true;
    }

    private boolean listLaunchPads(CommandSender sender) {
        ConfigurationSection pads = configWriter.section("launch-pads.pads");
        if (pads == null || pads.getKeys(false).isEmpty()) {
            sender.sendMessage("No launch pads configured.");
            return true;
        }

        sender.sendMessage(color("&aLaunchPads:"));
        for (String id : pads.getKeys(false)) {
            String base = "launch-pads.pads." + id;
            sender.sendMessage(color("&7- &f" + id + " &8("
                    + configWriter.getString(base + ".world", "?") + " "
                    + configWriter.getInt(base + ".x") + ", "
                    + configWriter.getInt(base + ".y") + ", "
                    + configWriter.getInt(base + ".z") + ")"));
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("/leeseolcore launchpad set <id> [forward] [upward] [cooldownSeconds]");
        sender.sendMessage("/leeseolcore launchpad list");
        sender.sendMessage("/leeseolcore launchpad remove <id>");
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
