package me.leeseol.core.command;

import me.leeseol.core.status.ServerStatusService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ServerInfoCommand implements CommandExecutor {
    private final ServerStatusService statusService;

    public ServerInfoCommand(ServerStatusService statusService) {
        this.statusService = statusService;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        sender.sendMessage("=== " + statusService.serverName() + " ===");
        sender.sendMessage("Online: " + statusService.onlinePlayers() + " / " + statusService.maxPlayers());
        sender.sendMessage("Minecraft: " + statusService.minecraftVersion());
        sender.sendMessage("Uptime: " + statusService.formattedUptime());
        return true;
    }
}
