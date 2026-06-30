package me.leeseol.core.command;

import me.leeseol.core.LeeSeolCorePlugin;
import me.leeseol.core.config.CoreConfigWriter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CoreCommand implements CommandExecutor {
    private final LeeSeolCorePlugin plugin;
    private final LaunchPadAdminCommand launchPadAdminCommand;
    private final PortalAdminCommand portalAdminCommand;

    public CoreCommand(LeeSeolCorePlugin plugin, CoreConfigWriter configWriter) {
        this.plugin = plugin;
        this.launchPadAdminCommand = new LaunchPadAdminCommand(plugin, configWriter);
        this.portalAdminCommand = new PortalAdminCommand(plugin, configWriter);
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
            return launchPadAdminCommand.handle(sender, args);
        }

        if (args.length >= 2 && isPortalCommand(args[0])) {
            return portalAdminCommand.handle(sender, args);
        }

        if (args.length >= 2 && isServerNpcCommand(args[0])) {
            return plugin.serverNpcManager().handleCommand(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    private boolean isLaunchPadCommand(String value) {
        return value.equalsIgnoreCase("launchpad") || value.equalsIgnoreCase("pad");
    }

    private boolean isPortalCommand(String value) {
        return value.equalsIgnoreCase("portal");
    }

    private boolean isServerNpcCommand(String value) {
        return value.equalsIgnoreCase("servernpc") || value.equalsIgnoreCase("npc");
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
        sender.sendMessage("/leeseolcore servernpc create <id> <targetServer> [displayName]");
        sender.sendMessage("/leeseolcore servernpc bind <id> <citizensNpcId> <targetServer>");
        sender.sendMessage("/leeseolcore servernpc list");
        sender.sendMessage("/leeseolcore servernpc remove <id>");
    }
}
