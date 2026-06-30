package me.leeseol.enchanting.command;

import java.util.List;
import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import me.leeseol.enchanting.service.BookshelfCounter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class EnchantingCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "status", "bookshelves", "lore");

    private final LeeSeolEnchantingPlugin plugin;

    public EnchantingCommand(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolenchanting.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase();
        return switch (subcommand) {
            case "reload" -> reload(sender);
            case "bookshelves" -> bookshelves(sender);
            case "lore" -> lore(sender);
            case "status" -> status(sender);
            default -> {
                sender.sendMessage("/" + label + " <reload|status|bookshelves|lore>");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadAll();
        plugin.message(sender, "reloaded");
        return true;
    }

    private boolean status(CommandSender sender) {
        plugin.message(
            sender,
            "status",
            "%enabled%",
            String.valueOf(plugin.enchantingConfig().enabled()),
            "%worlds%",
            plugin.enchantingConfig().allowedWorldsText(),
            "%bands%",
            String.valueOf(plugin.enchantingConfig().bands().size())
        );
        sender.sendMessage("AdvancedEnchantments API: " + plugin.advancedEnchantmentsBridge().available());
        return true;
    }

    private boolean bookshelves(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.ENCHANTING_TABLE) {
            plugin.message(sender, "not-enchanting-table");
            return true;
        }
        int count = new BookshelfCounter(plugin.enchantingConfig()).count(target);
        plugin.message(sender, "bookshelf-count", "%count%", String.valueOf(count));
        return true;
    }

    private boolean lore(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            plugin.message(sender, "lore-no-item");
            return true;
        }
        if (plugin.loreDescriptionService().refresh(item)) {
            plugin.message(sender, "lore-updated");
        } else {
            plugin.message(sender, "lore-no-custom-enchants");
        }
        return true;
    }
}
