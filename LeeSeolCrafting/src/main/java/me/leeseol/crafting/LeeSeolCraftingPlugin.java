package me.leeseol.crafting;

import me.leeseol.crafting.command.CraftingAdminCommand;
import me.leeseol.crafting.command.CraftingCommand;
import me.leeseol.crafting.gui.CraftingGui;
import me.leeseol.crafting.model.RecipeType;
import me.leeseol.crafting.service.CraftingService;
import me.leeseol.crafting.service.EconomyService;
import me.leeseol.crafting.service.RankRequirementService;
import me.leeseol.crafting.service.RecipeService;
import me.leeseol.crafting.service.RepairService;
import me.leeseol.crafting.util.Text;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCraftingPlugin extends JavaPlugin implements Listener {
    public static NamespacedKey RECIPE_KEY;

    private RecipeService recipeService;
    private EconomyService economyService;
    private RankRequirementService rankRequirementService;
    private CraftingService craftingService;
    private RepairService repairService;
    private CraftingGui craftingGui;

    @Override
    public void onEnable() {
        RECIPE_KEY = new NamespacedKey(this, "recipe_id");
        saveDefaultConfig();
        recipeService = new RecipeService(this);
        economyService = new EconomyService(this);
        rankRequirementService = new RankRequirementService();
        craftingService = new CraftingService(this);
        repairService = new RepairService(this);
        craftingGui = new CraftingGui(this);
        reloadAll();
        registerCommands();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LeeSeolCrafting enabled. recipes=" + recipeService.count());
    }

    public void reloadAll() {
        reloadConfig();
        recipeService.reload();
        economyService.reload();
    }

    private void registerCommands() {
        getCommand("craftmenu").setExecutor(new CraftingCommand(this, RecipeType.CRAFTING));
        getCommand("forge").setExecutor(new CraftingCommand(this, RecipeType.CRAFTING));
        getCommand("process").setExecutor(new CraftingCommand(this, RecipeType.PROCESSING));
        getCommand("disassemble").setExecutor(new CraftingCommand(this, RecipeType.DISASSEMBLE));
        getCommand("repair").setExecutor(new CraftingCommand(this));
        CraftingAdminCommand adminCommand = new CraftingAdminCommand(this);
        getCommand("lscrafting").setExecutor(adminCommand);
        getCommand("lscrafting").setTabCompleter(adminCommand);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        craftingGui.handleClick(event);
    }

    public boolean canUse(Player player) {
        if (!getConfig().getBoolean("settings.enabled", true)) {
            message(player, "disabled");
            return false;
        }
        if (player.hasPermission("leeseolcrafting.bypass.world")) {
            return true;
        }
        var worlds = getConfig().getStringList("settings.allowed-worlds");
        if (worlds.isEmpty() || worlds.stream().anyMatch(player.getWorld().getName()::equalsIgnoreCase)) {
            return true;
        }
        message(player, "world-blocked");
        return false;
    }

    public RecipeService recipeService() {
        return recipeService;
    }

    public EconomyService economyService() {
        return economyService;
    }

    public RankRequirementService rankRequirementService() {
        return rankRequirementService;
    }

    public CraftingService craftingService() {
        return craftingService;
    }

    public RepairService repairService() {
        return repairService;
    }

    public CraftingGui craftingGui() {
        return craftingGui;
    }

    public String color(String message) {
        return Text.color(message);
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String message = getConfig().getString("messages." + key, key);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        sender.sendMessage(Text.color(prefix + message));
    }
}
