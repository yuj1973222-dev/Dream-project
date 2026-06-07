package me.leeseol.economy.shop;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolEconomyPlugin plugin;
    private final ShopManager shopManager;

    public ShopCommand(LeeSeolEconomyPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msgRaw("player-only"));
            return true;
        }
        if (!player.hasPermission("leeseoleconomy.shop")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        shopManager.open(player, args.length >= 1 ? args[0] : null);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        for (String id : shopManager.shopIds()) {
            if (id.startsWith(args[0].toLowerCase())) {
                ids.add(id);
            }
        }
        return ids;
    }
}
