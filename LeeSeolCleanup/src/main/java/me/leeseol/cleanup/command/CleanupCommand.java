package me.leeseol.cleanup.command;

import java.util.Locale;
import me.leeseol.cleanup.LeeSeolCleanupPlugin;
import me.leeseol.cleanup.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CleanupCommand implements CommandExecutor {
    private final LeeSeolCleanupPlugin plugin;

    public CleanupCommand(LeeSeolCleanupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolcleanup.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.message(sender, "status",
                    "%enabled%", plugin.cleanupManager().enabled() ? "ON" : "OFF",
                    "%minutes%", String.valueOf(plugin.cleanupManager().intervalMinutes()),
                    "%worlds%", plugin.cleanupManager().worldSummary());
            return true;
        }
        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("run")) {
            int removed = plugin.cleanupManager().cleanupNow();
            plugin.message(sender, "cleaned-manual", "%count%", String.valueOf(removed));
            return true;
        }
        if (subCommand.equals("reload")) {
            plugin.reloadPluginConfig();
            plugin.message(sender, "reloaded");
            return true;
        }
        Text.send(sender, "&c사용법: /" + label + " <status|run|reload>");
        return true;
    }
}
