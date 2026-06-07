package me.leeseol.cleanup;

import me.leeseol.cleanup.command.CleanupCommand;
import me.leeseol.cleanup.hook.CleanupPlaceholderExpansion;
import me.leeseol.cleanup.manager.CleanupManager;
import me.leeseol.cleanup.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCleanupPlugin extends JavaPlugin {
    private CleanupManager cleanupManager;
    private CleanupPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cleanupManager = new CleanupManager(this);
        cleanupManager.reload();
        getCommand("leeseolcleanup").setExecutor(new CleanupCommand(this));
        registerPlaceholderExpansion();
        getLogger().info("LeeSeolCleanup enabled.");
    }

    @Override
    public void onDisable() {
        if (cleanupManager != null) {
            cleanupManager.stop();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        cleanupManager.reload();
    }

    public CleanupManager cleanupManager() {
        return cleanupManager;
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found. Cleanup placeholders are disabled.");
            return;
        }
        placeholderExpansion = new CleanupPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("Registered PlaceholderAPI placeholders: %leeseolcleanup_next%, %leeseolcleanup_seconds%");
    }

    public void broadcastToCleanupWorlds(String key, String... replacements) {
        String message = format(key, replacements);
        for (String worldName : getConfig().getStringList("cleanup.worlds")) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                Text.send(player, message);
            }
        }
    }

    public void message(CommandSender sender, String key, String... replacements) {
        Text.send(sender, format(key, replacements));
    }

    private String format(String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String message = getConfig().getString("messages." + key, "");
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        return prefix + message;
    }
}
