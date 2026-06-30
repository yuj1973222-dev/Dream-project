package me.leeseol.proxy;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import me.leeseol.proxy.command.ProxyCommandRegistrar;
import me.leeseol.proxy.config.ProxyConfigRepository;
import me.leeseol.proxy.network.NetworkRouteService;
import me.leeseol.proxy.queue.QueueCoordinator;
import me.leeseol.proxy.queue.SurvivalQueueController;
import me.leeseol.proxy.resourcepack.ResourcePackCoordinator;
import org.slf4j.Logger;

final class ProxyServices {
    private final LeeSeolProxyPlugin plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final ProxyConfigRepository configRepository;
    private ResourcePackCoordinator resourcePackCoordinator;
    private NetworkRouteService networkRouteService;
    private QueueCoordinator queueCoordinator;

    ProxyServices(LeeSeolProxyPlugin plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.configRepository = new ProxyConfigRepository(dataDirectory);
    }

    void start() {
        resourcePackCoordinator = new ResourcePackCoordinator(proxy, logger, configRepository);
        networkRouteService = new NetworkRouteService(proxy, logger, configRepository);
        queueCoordinator = new QueueCoordinator(proxy, plugin, logger, configRepository);
        resourcePackCoordinator.reload();
        networkRouteService.reload();
        queueCoordinator.start();

        new ProxyCommandRegistrar(proxy, plugin, queueCoordinator.controller()).registerAll();
        logger.info("LeeSeolProxy enabled.");
    }

    void close() {
        if (queueCoordinator != null) {
            queueCoordinator.close();
        }
    }

    void handleLogin(LoginEvent event) {
        networkRouteService.handleLogin(event);
    }

    void handlePostLogin(PostLoginEvent event) {
        resourcePackCoordinator.handlePostLogin(event);
    }

    void handleResourcePackStatus(PlayerResourcePackStatusEvent event) {
        resourcePackCoordinator.handleResourcePackStatus(event);
    }

    void handleKickedFromServer(KickedFromServerEvent event) {
        networkRouteService.handleKickedFromServer(event);
    }

    void handlePluginMessage(PluginMessageEvent event) {
        if (queueCoordinator != null) {
            queueCoordinator.handlePluginMessage(event);
        }
    }

    void handleServerConnected(ServerConnectedEvent event) {
        if (queueCoordinator != null) {
            queueCoordinator.handleServerConnected(event);
        }
    }

    void handleDisconnect(DisconnectEvent event) {
        if (queueCoordinator != null) {
            queueCoordinator.handleDisconnect(event);
        }
    }

    SurvivalQueueController queueController() {
        return queueCoordinator == null ? null : queueCoordinator.controller();
    }
}
