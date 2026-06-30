package me.leeseol.core.command;

import java.time.Duration;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ServerInfoCommand implements CommandExecutor {
    private final LeeSeolCorePlugin plugin;

    public ServerInfoCommand(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        String serverName = plugin.getConfig().getString("server-name", "LeeSeol Server");
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        Duration uptime = Duration.ofMillis(System.currentTimeMillis() - plugin.getEnabledAtMillis());

        sender.sendMessage("=== " + serverName + " ===");
        sender.sendMessage("Online: " + onlinePlayers + " / " + maxPlayers);
        sender.sendMessage("Minecraft: " + Bukkit.getMinecraftVersion());
        sender.sendMessage("Uptime: " + formatDuration(uptime));
        return true;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
