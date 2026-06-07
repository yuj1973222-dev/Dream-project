package me.leeseol.hud.command;

import java.util.List;
import me.leeseol.hud.LeeSeolHudPlugin;
import me.leeseol.hud.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HudAdminCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "status");

    private final LeeSeolHudPlugin plugin;

    public HudAdminCommand(LeeSeolHudPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("leeseolhud.admin")) {
            send(sender, "messages.no-permission");
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadHud();
                send(sender, "messages.reloaded");
            }
            case "status" -> Text.send(sender, prefix() + "&fonline=&e" + Bukkit.getOnlinePlayers().size()
                + " &7settings.enabled=&e" + plugin.getConfig().getBoolean("settings.enabled", true));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

    private void send(CommandSender sender, String path) {
        Text.send(sender, prefix() + plugin.getConfig().getString(path, ""));
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&7[&bHUD&7] ");
    }
}
