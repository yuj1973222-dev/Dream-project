package me.leeseol.core;

import me.leeseol.core.command.CoreCommand;
import me.leeseol.core.command.ContentCommand;
import me.leeseol.core.command.ServerInfoCommand;
import me.leeseol.core.content.BlueMapContentMarkers;
import me.leeseol.core.content.ContentService;
import me.leeseol.core.launchpad.LaunchPadListener;
import me.leeseol.core.listener.DimensionGateListener;
import me.leeseol.core.listener.JoinListener;
import me.leeseol.core.networkmove.BungeeCordNetworkMovePort;
import me.leeseol.core.networkmove.NetworkMovePort;
import me.leeseol.core.portal.PortalListener;
import me.leeseol.core.servernpc.ServerNpcManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCorePlugin extends JavaPlugin {
    private long enabledAtMillis;
    private CoreServices services;

    @Override
    public void onEnable() {
        enabledAtMillis = System.currentTimeMillis();
        saveDefaultConfig();
        services = new CoreServices(this);

        reloadCoreConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeCordNetworkMovePort.CHANNEL);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionGateListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(services.configManager(), services.portalManager()), this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(services.configManager(), services.launchPadManager()), this);
        getServer().getPluginManager().registerEvents(services.serverMenuManager(), this);
        getServer().getPluginManager().registerEvents(services.survivalSpawnManager(), this);
        registerCommand("serverinfo", new ServerInfoCommand(services.serverStatusService()));
        registerCommand("survivalspawn", services.survivalSpawnManager());
        CoreCommand coreCommand = new CoreCommand(this);
        registerCommand("lscore", coreCommand);
        registerCommand("leeseolcore", coreCommand);
        registerCommand("content", new ContentCommand(this, services.contentService()));

        getLogger().info("LeeSeolCore enabled.");
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.disableAll();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, BungeeCordNetworkMovePort.CHANNEL);
        getLogger().info("LeeSeolCore disabled.");
    }

    public long getEnabledAtMillis() {
        return enabledAtMillis;
    }

    public void reloadCoreConfig() {
        services.reloadAll();
    }

    public ServerNpcManager serverNpcManager() {
        return services.serverNpcManager();
    }

    public ContentService contentService() {
        return services.contentService();
    }

    public BlueMapContentMarkers blueMapContentMarkers() {
        return services.blueMapContentMarkers();
    }

    public void sendPlayerToServer(Player player, String targetServer) {
        services.networkMoveService().move(player, targetServer);
    }

    public NetworkMovePort networkMovePort() {
        return services.networkMovePort();
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabExecutor tabExecutor) {
            command.setTabCompleter(tabExecutor);
        }
    }
}
