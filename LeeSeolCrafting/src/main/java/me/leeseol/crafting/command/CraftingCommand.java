package me.leeseol.crafting.command;

import me.leeseol.crafting.LeeSeolCraftingPlugin;
import me.leeseol.crafting.model.RecipeType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CraftingCommand implements CommandExecutor {
    private final LeeSeolCraftingPlugin plugin;
    private final RecipeType type;
    private final boolean repair;

    public CraftingCommand(LeeSeolCraftingPlugin plugin, RecipeType type) {
        this.plugin = plugin;
        this.type = type;
        this.repair = false;
    }

    public CraftingCommand(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
        this.type = null;
        this.repair = true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.message(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("leeseolcrafting.use")) {
            plugin.message(player, "no-permission");
            return true;
        }
        if (repair) {
            plugin.craftingGui().openRepair(player);
        } else {
            plugin.craftingGui().openList(player, type);
        }
        return true;
    }
}
