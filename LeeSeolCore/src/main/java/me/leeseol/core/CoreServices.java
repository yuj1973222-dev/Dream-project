package me.leeseol.core;

import me.leeseol.core.config.ConfigManager;
import me.leeseol.core.content.BlueMapContentMarkers;
import me.leeseol.core.content.ContentService;
import me.leeseol.core.content.WorldGuardContentRegionService;
import me.leeseol.core.launchpad.LaunchPadManager;
import me.leeseol.core.menu.CoreServerMenuManager;
import me.leeseol.core.networkmove.BungeeCordNetworkMovePort;
import me.leeseol.core.networkmove.NetworkMoveService;
import me.leeseol.core.networkmove.NetworkMovePort;
import me.leeseol.core.portal.PortalManager;
import me.leeseol.core.servernpc.ServerNpcManager;
import me.leeseol.core.spawn.SurvivalSpawnManager;
import me.leeseol.core.status.ServerStatusService;
import org.bukkit.event.HandlerList;

final class CoreServices {
    private final LeeSeolCorePlugin plugin;
    private final ConfigManager configManager;
    private final PortalManager portalManager;
    private final LaunchPadManager launchPadManager;
    private final CoreServerMenuManager serverMenuManager;
    private final ServerNpcManager serverNpcManager;
    private final SurvivalSpawnManager survivalSpawnManager;
    private final ContentService contentService;
    private final BlueMapContentMarkers blueMapContentMarkers;
    private final NetworkMoveService networkMoveService;
    private final ServerStatusService serverStatusService;

    CoreServices(LeeSeolCorePlugin plugin) {
        this.plugin = plugin;
        this.configManager = new ConfigManager(plugin);
        this.portalManager = new PortalManager(plugin);
        this.launchPadManager = new LaunchPadManager(plugin);
        this.serverMenuManager = new CoreServerMenuManager(plugin);
        this.serverNpcManager = new ServerNpcManager(plugin);
        this.survivalSpawnManager = new SurvivalSpawnManager(plugin);
        this.networkMoveService = new NetworkMoveService(new BungeeCordNetworkMovePort(plugin));
        this.serverStatusService = new ServerStatusService(plugin);
        this.contentService = new ContentService(plugin, new WorldGuardContentRegionService(plugin));
        this.blueMapContentMarkers = new BlueMapContentMarkers(plugin, contentService);
        this.contentService.setAfterChange(blueMapContentMarkers::refreshLater);
    }

    void reloadAll() {
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        configManager.reload();
        portalManager.reload();
        launchPadManager.reload();
        serverMenuManager.reload();
        serverNpcManager.reload();
        survivalSpawnManager.reload();
        contentService.reload();
        blueMapContentMarkers.refreshLater();
        plugin.getLogger().info("Enabled worlds: " + String.join(", ", configManager.getEnabledWorlds()));
    }

    void disableAll() {
        HandlerList.unregisterAll(serverNpcManager);
        blueMapContentMarkers.clear();
    }

    ConfigManager configManager() {
        return configManager;
    }

    PortalManager portalManager() {
        return portalManager;
    }

    LaunchPadManager launchPadManager() {
        return launchPadManager;
    }

    CoreServerMenuManager serverMenuManager() {
        return serverMenuManager;
    }

    ServerNpcManager serverNpcManager() {
        return serverNpcManager;
    }

    SurvivalSpawnManager survivalSpawnManager() {
        return survivalSpawnManager;
    }

    ContentService contentService() {
        return contentService;
    }

    BlueMapContentMarkers blueMapContentMarkers() {
        return blueMapContentMarkers;
    }

    NetworkMoveService networkMoveService() {
        return networkMoveService;
    }

    NetworkMovePort networkMovePort() {
        return networkMoveService.movePort();
    }

    ServerStatusService serverStatusService() {
        return serverStatusService;
    }
}
