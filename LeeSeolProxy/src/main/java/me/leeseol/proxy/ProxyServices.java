package me.leeseol.proxy;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import me.leeseol.proxy.command.LobbyCommand;
import me.leeseol.proxy.command.ServerListCommand;
import me.leeseol.proxy.command.SurvivalQueueCommand;
import me.leeseol.proxy.config.ProxyConfigRepository;
import me.leeseol.proxy.network.NetworkSettings;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.queue.SurvivalQueueController;
import me.leeseol.proxy.resourcepack.ResourcePackOfferService;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

final class ProxyServices {
    private final LeeSeolProxyPlugin plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final ProxyConfigRepository configRepository;
    private ResourcePackInfo resourcePackInfo;
    private ResourcePackOfferService resourcePackOfferService;
    private NetworkSettings networkSettings = NetworkSettings.defaults();
    private SurvivalQueueController queueController;
    private ChannelIdentifier queueChannel;

    ProxyServices(LeeSeolProxyPlugin plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.configRepository = new ProxyConfigRepository(dataDirectory);
    }

    void start() {
        CommandManager commandManager = proxy.getCommandManager();

        CommandMeta serversMeta = commandManager.metaBuilder("servers")
                .aliases("serverlist", "network")
                .plugin(plugin)
                .build();
        commandManager.register(serversMeta, new ServerListCommand(proxy));

        resourcePackOfferService = new ResourcePackOfferService(proxy, logger);
        loadResourcePackInfo();
        networkSettings = loadNetworkSettings();
        QueueSettings queueSettings = loadQueueSettings();
        queueChannel = MinecraftChannelIdentifier.from(queueSettings.pluginMessageChannel());
        proxy.getChannelRegistrar().register(queueChannel);
        queueController = new SurvivalQueueController(proxy, plugin, logger, queueChannel, queueSettings);
        queueController.start();
        registerLobbyCommand(commandManager);
        registerSurvivalCommand(commandManager);
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
        NetworkSettings settings = loadNetworkSettings();
        networkSettings = settings;
        if (settings.maintenance()) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(settings.maintenanceMessage())));
        }
    }

    void handlePostLogin(PostLoginEvent event) {
        ResourcePackInfo packInfo = resourcePackInfo;
        if (packInfo == null) {
            return;
        }

        Player player = event.getPlayer();
        player.sendResourcePackOffer(packInfo);
        logger.info("Sent network resource pack offer to {}", player.getUsername());
    }

    void handleResourcePackStatus(PlayerResourcePackStatusEvent event) {
        logger.info(
                "Resource pack status for {}: {}",
                event.getPlayer().getUsername(),
                event.getStatus()
        );
    }

    void handleKickedFromServer(KickedFromServerEvent event) {
        NetworkSettings settings = loadNetworkSettings();
        networkSettings = settings;

        String kickedServer = event.getServer().getServerInfo().getName().toLowerCase(Locale.ROOT);
        if (!settings.fallbackEnabled() || !settings.fallbackFrom().contains(kickedServer)) {
            return;
        }

        if (settings.maintenance()) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(settings.maintenanceMessage())));
            return;
        }

        String fallbackServerName = settings.fallbackServer().toLowerCase(Locale.ROOT);
        if (kickedServer.equals(fallbackServerName)) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(settings.fallbackUnavailableMessage())));
            return;
        }

        RegisteredServer fallbackServer = proxy.getServer(settings.fallbackServer()).orElse(null);
        if (fallbackServer == null) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(settings.fallbackUnavailableMessage())));
            return;
        }

        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                fallbackServer,
                Component.text(settings.fallbackMessage())
        ));
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

    private void registerLobbyCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("lobby")
                .aliases("hub", "濡쒕퉬")
                .plugin(plugin)
                .build();
        commandManager.register(meta, new LobbyCommand(queueController));
    }

    private void registerSurvivalCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("survival")
                .aliases("wild", "?쇱깮?낆옣")
                .plugin(plugin)
                .build();
        commandManager.register(meta, new SurvivalQueueCommand(queueController));
    }

    private void loadResourcePackInfo() {
        try {
            resourcePackInfo = resourcePackOfferService.reload(configRepository.loadResourcePackSettings());
        } catch (IOException exception) {
            logger.warn("Failed to load resource pack config. Resource pack offer is disabled.", exception);
            resourcePackInfo = null;
        }
    }

    private NetworkSettings loadNetworkSettings() {
        try {
            return configRepository.loadNetworkSettings();
        } catch (IOException exception) {
            logger.warn("Failed to load network settings. Using safe fallback defaults.", exception);
            return NetworkSettings.defaults();
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
