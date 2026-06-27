package me.leeseol.enchanting;

import me.leeseol.enchanting.command.EnchantingCommand;
import me.leeseol.enchanting.listener.EnchantLoreListener;
import me.leeseol.enchanting.listener.EnchantingTableListener;
import me.leeseol.enchanting.service.AdvancedEnchantmentsBridge;
import me.leeseol.enchanting.service.EnchantingConfig;
import me.leeseol.enchanting.service.LoreDescriptionService;
import me.leeseol.enchanting.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolEnchantingPlugin extends JavaPlugin {
    private EnchantingConfig enchantingConfig;
    private AdvancedEnchantmentsBridge advancedEnchantmentsBridge;
    private LoreDescriptionService loreDescriptionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        enchantingConfig = new EnchantingConfig(this);
        advancedEnchantmentsBridge = new AdvancedEnchantmentsBridge(this);
        loreDescriptionService = new LoreDescriptionService(this);
        reloadAll();

        EnchantingCommand command = new EnchantingCommand(this);
        getCommand("lsenchanting").setExecutor(command);
        getCommand("lsenchanting").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new EnchantingTableListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantLoreListener(this), this);
        getLogger().info("LeeSeolEnchanting enabled. ae=" + advancedEnchantmentsBridge.available()
            + ", bands=" + enchantingConfig.bands().size());
    }

    public void reloadAll() {
        reloadConfig();
        enchantingConfig.reload();
        advancedEnchantmentsBridge.reload();
    }

    public EnchantingConfig enchantingConfig() {
        return enchantingConfig;
    }

    public AdvancedEnchantmentsBridge advancedEnchantmentsBridge() {
        return advancedEnchantmentsBridge;
    }

    public LoreDescriptionService loreDescriptionService() {
        return loreDescriptionService;
    }

    public void message(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages.prefix", "") + getConfig().getString("messages." + key, key);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        sender.sendMessage(Text.color(message));
    }
}
