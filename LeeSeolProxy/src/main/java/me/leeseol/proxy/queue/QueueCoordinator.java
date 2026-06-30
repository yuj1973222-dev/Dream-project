package me.leeseol.proxy.queue;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.IOException;
import me.leeseol.proxy.config.ProxyConfigRepository;
import org.slf4j.Logger;

public final class QueueCoordinator {
    private final ProxyServer proxy;
    private final Object plugin;
    private final Logger logger;
    private final ProxyConfigRepository configRepository;
    private ChannelIdentifier queueChannel;
    private SurvivalQueueController queueController;

    public QueueCoordinator(ProxyServer proxy, Object plugin, Logger logger, ProxyConfigRepository configRepository) {
        this.proxy = proxy;
        this.plugin = plugin;
        this.logger = logger;
        this.configRepository = configRepository;
    }

    public void start() {
        QueueSettings queueSettings = loadQueueSettings();
        queueChannel = MinecraftChannelIdentifier.from(queueSettings.pluginMessageChannel());
        proxy.getChannelRegistrar().register(queueChannel);
        queueController = new SurvivalQueueController(proxy, plugin, logger, queueChannel, queueSettings);
        queueController.start();
    }

    public void close() {
        if (queueController != null) {
            queueController.close();
        }
        if (queueChannel != null) {
            proxy.getChannelRegistrar().unregister(queueChannel);
        }
    }

    public SurvivalQueueController controller() {
        return queueController;
    }

    public void handlePluginMessage(PluginMessageEvent event) {
        if (queueController != null) {
            queueController.onPluginMessage(event);
        }
    }

    public void handleServerConnected(ServerConnectedEvent event) {
        if (queueController != null) {
            queueController.onServerConnected(event);
        }
    }

    public void handleDisconnect(DisconnectEvent event) {
        if (queueController != null) {
            queueController.onDisconnect(event);
        }
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
