package me.leeseol.core;

import me.leeseol.core.command.CoreCommand;
import me.leeseol.core.command.ContentCommand;
import me.leeseol.core.command.ServerInfoCommand;
import me.leeseol.core.content.BlueMapContentMarkers;
import me.leeseol.core.config.ConfigManager;
import me.leeseol.core.content.ContentService;
import me.leeseol.core.content.WorldGuardContentRegionService;
import me.leeseol.core.launchpad.LaunchPadListener;
import me.leeseol.core.launchpad.LaunchPadManager;
import me.leeseol.core.listener.DimensionGateListener;
import me.leeseol.core.listener.JoinListener;
import me.leeseol.core.menu.CoreServerMenuManager;
import me.leeseol.core.networkmove.BungeeCordNetworkMovePort;
import me.leeseol.core.networkmove.NetworkMovePort;
import me.leeseol.core.portal.PortalListener;
import me.leeseol.core.portal.PortalManager;
import me.leeseol.core.servernpc.ServerNpcManager;
import me.leeseol.core.spawn.SurvivalSpawnManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeeSeolCorePlugin extends JavaPlugin {
    private long enabledAtMillis;
    private ConfigManager configManager;
    private PortalManager portalManager;
    private LaunchPadManager launchPadManager;
    private CoreServerMenuManager serverMenuManager;
    private ServerNpcManager serverNpcManager;
    private SurvivalSpawnManager survivalSpawnManager;
    private ContentService contentService;
    private BlueMapContentMarkers blueMapContentMarkers;
    private NetworkMovePort networkMovePort;

    @Override
    public void onEnable() {
        enabledAtMillis = System.currentTimeMillis();
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        portalManager = new PortalManager(this);
        launchPadManager = new LaunchPadManager(this);
        serverMenuManager = new CoreServerMenuManager(this);
        serverNpcManager = new ServerNpcManager(this);
        survivalSpawnManager = new SurvivalSpawnManager(this);
        networkMovePort = new BungeeCordNetworkMovePort(this);
        contentService = new ContentService(this, new WorldGuardContentRegionService(this));
        blueMapContentMarkers = new BlueMapContentMarkers(this, contentService);
        contentService.setAfterChange(blueMapContentMarkers::refreshLater);

        reloadCoreConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeCordNetworkMovePort.CHANNEL);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionGateListener(this), this);
        getServer().getPluginManager().registerEvents(new PortalListener(configManager, portalManager), this);
        getServer().getPluginManager().registerEvents(new LaunchPadListener(configManager, launchPadManager), this);
        getServer().getPluginManager().registerEvents(serverMenuManager, this);
        getServer().getPluginManager().registerEvents(survivalSpawnManager, this);
        registerCommand("serverinfo", new ServerInfoCommand(this));
        registerCommand("survivalspawn", survivalSpawnManager);
        CoreCommand coreCommand = new CoreCommand(this);
        registerCommand("lscore", coreCommand);
        registerCommand("leeseolcore", coreCommand);
        registerCommand("content", new ContentCommand(this, contentService));

        getLogger().info("LeeSeolCore enabled.");
    }

    @Override
    public void onDisable() {
        if (serverNpcManager != null) {
            org.bukkit.event.HandlerList.unregisterAll(serverNpcManager);
        }
        if (blueMapContentMarkers != null) {
            blueMapContentMarkers.clear();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, BungeeCordNetworkMovePort.CHANNEL);
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
        serverMenuManager.reload();
        serverNpcManager.reload();
        survivalSpawnManager.reload();
        contentService.reload();
        blueMapContentMarkers.refreshLater();
        getLogger().info("Enabled worlds: " + String.join(", ", configManager.getEnabledWorlds()));
    }

    public ServerNpcManager serverNpcManager() {
        return serverNpcManager;
    }

    public ContentService contentService() {
        return contentService;
    }

    public BlueMapContentMarkers blueMapContentMarkers() {
        return blueMapContentMarkers;
    }

    public void sendPlayerToServer(Player player, String targetServer) {
        networkMovePort.requestMove(player, targetServer);
    }

    public NetworkMovePort networkMovePort() {
        return networkMovePort;
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
