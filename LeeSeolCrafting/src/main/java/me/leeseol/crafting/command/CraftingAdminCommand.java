package me.leeseol.crafting.command;

import java.util.ArrayList;
import java.util.List;
import me.leeseol.crafting.LeeSeolCraftingPlugin;
import me.leeseol.crafting.model.Recipe;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CraftingAdminCommand implements CommandExecutor, TabCompleter {
    private final LeeSeolCraftingPlugin plugin;

    public CraftingAdminCommand(LeeSeolCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("leeseolcrafting.admin")) {
            plugin.message(sender, "no-permission");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.message(sender, "status", "%recipes%", String.valueOf(plugin.recipeService().count()));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            plugin.message(sender, "reloaded");
            return true;
        }
        if (args[0].equalsIgnoreCase("recipe")) {
            return recipe(sender, label, args);
        }
        sendHelp(sender, label);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "reload", "recipe");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("recipe")) {
            return List.of("list", "give");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("recipe") && args[1].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>();
            plugin.recipeService().ids().forEach(ids::add);
            return ids;
        }
        return List.of();
    }

    private boolean recipe(CommandSender sender, String label, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(plugin.color("&b레시피 목록"));
            plugin.recipeService().ids().forEach(id -> sender.sendMessage(plugin.color("&7- &f" + id)));
            return true;
        }
        if (args.length >= 4 && args[1].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            Recipe recipe = plugin.recipeService().recipe(args[3]);
            if (target == null || recipe == null) {
                sender.sendMessage(plugin.color("&c플레이어 또는 레시피를 찾을 수 없습니다."));
                return true;
            }
            target.getInventory().addItem(recipe.result().clone());
            sender.sendMessage(plugin.color("&a지급 완료: " + recipe.id()));
            return true;
        }
        sender.sendMessage(plugin.color("&c사용법: /" + label + " recipe <list|give>"));
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(plugin.color("&b/" + label + " status"));
        sender.sendMessage(plugin.color("&b/" + label + " reload"));
        sender.sendMessage(plugin.color("&b/" + label + " recipe list"));
        sender.sendMessage(plugin.color("&b/" + label + " recipe give <player> <recipeId>"));
    }
}
