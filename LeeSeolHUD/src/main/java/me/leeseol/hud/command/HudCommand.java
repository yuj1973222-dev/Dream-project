package me.leeseol.hud.command;

import java.util.List;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HudCommand implements CommandExecutor, TabCompleter {
    private static final List<String> FEATURES = List.of("compass", "target");
    private static final List<String> TOGGLES = List.of("on", "off");

    private final LeeSeolHudPlugin plugin;

    public HudCommand(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "messages.player-only");
            return true;
        }
        if (!player.hasPermission("leeseolhud.use")) {
            send(player, "messages.no-permission");
            return true;
        }
        if (command.getName().equalsIgnoreCase("compasshud")) {
            return handleCompassCommand(player, args);
        }
        if (args.length == 0) {
            sendStatus(player);
            return true;
        }
        if (args.length < 2) {
            return false;
        }

        boolean enabled = switch (args[1].toLowerCase()) {
            case "on" -> true;
            case "off" -> false;
            default -> {
                yield false;
            }
        };
        if (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off")) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "compass" -> setCompass(player, enabled);
            case "target" -> setTarget(player, enabled);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("compasshud")) {
            if (args.length == 1) {
                return TOGGLES.stream().filter(value -> value.startsWith(args[0].toLowerCase())).toList();
            }
            return List.of();
        }
        if (args.length == 1) {
            return FEATURES.stream().filter(value -> value.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2) {
            return TOGGLES.stream().filter(value -> value.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }

    private boolean handleCompassCommand(Player player, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (args[0].equalsIgnoreCase("on")) {
            setCompass(player, true);
            return true;
        }
        if (args[0].equalsIgnoreCase("off")) {
            setCompass(player, false);
            return true;
        }
        return false;
    }

    private void setCompass(Player player, boolean enabled) {
        if (!player.hasPermission("leeseolhud.compass")) {
            send(player, "messages.no-permission");
            return;
        }
        plugin.stateService().setCompass(player, enabled);
        if (enabled) {
            plugin.compassHudService().update(player);
            send(player, "messages.compass-on");
        } else {
            plugin.compassHudService().remove(player);
            send(player, "messages.compass-off");
        }
    }

    private void setTarget(Player player, boolean enabled) {
        if (!player.hasPermission("leeseolhud.target-health")) {
            send(player, "messages.no-permission");
            return;
        }
        plugin.stateService().setTargetHealth(player, enabled);
        if (!enabled) {
            plugin.targetHealthService().remove(player);
        }
        send(player, enabled ? "messages.target-on" : "messages.target-off");
    }

    private void sendStatus(Player player) {
        String message = plugin.getConfig().getString("messages.status", "&f나침반 &e%compass% &7/ &f대상 체력바 &e%target%")
            .replace("%compass%", plugin.stateService().compass(player) ? "켜짐" : "꺼짐")
            .replace("%target%", plugin.stateService().targetHealth(player) ? "켜짐" : "꺼짐");
        Text.send(player, prefix() + message);
    }

    private void send(CommandSender sender, String path) {
        Text.send(sender, prefix() + plugin.getConfig().getString(path, ""));
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&7[&bHUD&7] ");
    }
}
