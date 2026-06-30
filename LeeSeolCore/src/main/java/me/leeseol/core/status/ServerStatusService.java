package me.leeseol.core.status;

import java.time.Duration;
import java.util.Objects;
import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.Bukkit;

public final class ServerStatusService {
    private final LeeSeolCorePlugin plugin;

    public ServerStatusService(LeeSeolCorePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public String serverName() {
        return plugin.getConfig().getString("server-name", "LeeSeol Server");
    }

    public int onlinePlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    public int maxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    public String minecraftVersion() {
        return Bukkit.getMinecraftVersion();
    }

    public Duration uptime() {
        return Duration.ofMillis(System.currentTimeMillis() - plugin.getEnabledAtMillis());
    }

    public String formattedUptime() {
        return formatDuration(uptime());
    }

    public static String formatDuration(Duration duration) {
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
