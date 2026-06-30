package me.leeseol.proxy;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.IOException;
import java.nio.file.Path;
import me.leeseol.proxy.command.ProxyCommandRegistrar;
import me.leeseol.proxy.config.ProxyConfigRepository;
import me.leeseol.proxy.network.NetworkRouteService;
import me.leeseol.proxy.queue.QueueSettings;
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
    private SurvivalQueueController queueController;
    private ChannelIdentifier queueChannel;

    ProxyServices(LeeSeolProxyPlugin plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.configRepository = new ProxyConfigRepository(dataDirectory);
    }

    void start() {
        resourcePackCoordinator = new ResourcePackCoordinator(proxy, logger, configRepository);
        networkRouteService = new NetworkRouteService(proxy, logger, configRepository);
        resourcePackCoordinator.reload();
        networkRouteService.reload();

        QueueSettings queueSettings = loadQueueSettings();
        queueChannel = MinecraftChannelIdentifier.from(queueSettings.pluginMessageChannel());
        proxy.getChannelRegistrar().register(queueChannel);
        queueController = new SurvivalQueueController(proxy, plugin, logger, queueChannel, queueSettings);
        queueController.start();

        new ProxyCommandRegistrar(proxy, plugin, queueController).registerAll();
        logger.info("LeeSeolProxy enabled.");
    }

    void close() {
        if (queueController != null) {
            queueController.close();
        }
        if (queueChannel != null) {
            proxy.getChannelRegistrar().unregister(queueChannel);
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
        if (queueController != null) {
            queueController.onPluginMessage(event);
        }
    }

    void handleServerConnected(ServerConnectedEvent event) {
        if (queueController != null) {
            queueController.onServerConnected(event);
        }
    }

    void handleDisconnect(DisconnectEvent event) {
        if (queueController != null) {
            queueController.onDisconnect(event);
        }
    }

    SurvivalQueueController queueController() {
        return queueController;
    }

    private QueueSettings loadQueueSettings() {
        try {
            return configRepository.loadQueueSettings();
        } catch (IOException exception) {
            logger.warn("Failed to load queue settings. Using safe defaults.", exception);
            return QueueSettings.defaults();
        }
    }
}
