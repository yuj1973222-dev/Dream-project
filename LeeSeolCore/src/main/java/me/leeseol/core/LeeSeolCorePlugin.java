package me.leeseol.core;

import me.leeseol.core.command.CoreCommand;
import me.leeseol.core.command.ServerInfoCommand;
import me.leeseol.core.config.ConfigManager;
import me.leeseol.core.launchpad.LaunchPadListener;
import me.leeseol.core.launchpad.LaunchPadManager;
import me.leeseol.core.listener.DimensionGateListener;
import me.leeseol.core.listener.JoinListener;
import me.leeseol.core.listener.RespawnToLobbyListener;
import me.leeseol.core.portal.PortalListener;
import me.leeseol.core.portal.PortalManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCorePlugin extends JavaPlugin {
    private long enabledAtMillis;
    private ConfigManager configManager;
    private PortalManager portalManager;
    private LaunchPadManager launchPadManager;

    @Override
    public void onEnable() {
        enabledAtMillis = System.currentTimeMillis();
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        portalManager = new PortalManager(this);
        launchPadManager = new LaunchPadManager(this);

        reloadCoreConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new RespawnToLobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionGateListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(configManager, portalManager), this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(configManager, launchPadManager), this);
        registerCommand("serverinfo", new ServerInfoCommand(this));
        CoreCommand coreCommand = new CoreCommand(this);
        registerCommand("lscore", coreCommand);
        registerCommand("leeseolcore", coreCommand);

        getLogger().info("LeeSeolCore enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LeeSeolCore disabled.");
    }

    public long getEnabledAtMillis() {
        return enabledAtMillis;
    }

    public void reloadCoreConfig() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        configManager.reload();
        portalManager.reload();
        launchPadManager.reload();
        getLogger().info("Enabled worlds: " + String.join(", ", configManager.getEnabledWorlds()));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
    }
}
