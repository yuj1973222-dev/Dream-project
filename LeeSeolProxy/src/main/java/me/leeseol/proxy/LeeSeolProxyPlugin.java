package me.leeseol.proxy;

import com.google.inject.Inject;
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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import me.leeseol.proxy.command.LobbyCommand;
import me.leeseol.proxy.command.ServerListCommand;
import me.leeseol.proxy.command.SurvivalQueueCommand;
import me.leeseol.proxy.network.NetworkSettings;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.queue.SurvivalQueueController;
import me.leeseol.proxy.resourcepack.ResourcePackOfferService;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Plugin(
        id = "leeseolproxy",
        name = "LeeSeolProxy",
        version = "0.1.0",
        description = "Custom Velocity commands for LeeSeol's Minecraft network.",
        authors = {"lee_seol"}
)
public final class LeeSeolProxyPlugin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private ResourcePackInfo resourcePackInfo;
    private ResourcePackOfferService resourcePackOfferService;
    private NetworkSettings networkSettings = NetworkSettings.defaults();
    private SurvivalQueueController queueController;
    private ChannelIdentifier queueChannel;

    @Inject
    public LeeSeolProxyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        CommandManager commandManager = proxy.getCommandManager();

        CommandMeta serversMeta = commandManager.metaBuilder("servers")
                .aliases("serverlist", "network")
                .plugin(this)
                .build();
        commandManager.register(serversMeta, new ServerListCommand(proxy));

        resourcePackOfferService = new ResourcePackOfferService(proxy, logger);
        loadResourcePackInfo();
        networkSettings = loadNetworkSettings();
        QueueSettings queueSettings = loadQueueSettings();
        queueChannel = MinecraftChannelIdentifier.from(queueSettings.pluginMessageChannel());
        proxy.getChannelRegistrar().register(queueChannel);
        queueController = new SurvivalQueueController(proxy, this, logger, queueChannel, queueSettings);
        queueController.start();
        registerLobbyCommand(commandManager);
        registerSurvivalCommand(commandManager);
        logger.info("LeeSeolProxy enabled.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (queueController != null) {
            queueController.close();
        }
        if (queueChannel != null) {
            proxy.getChannelRegistrar().unregister(queueChannel);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        NetworkSettings settings = loadNetworkSettings();
        networkSettings = settings;
        if (settings.maintenance()) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(settings.maintenanceMessage())));
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        ResourcePackInfo packInfo = resourcePackInfo;
        if (packInfo == null) {
            return;
        }

        Player player = event.getPlayer();
        player.sendResourcePackOffer(packInfo);
        logger.info("Sent network resource pack offer to {}", player.getUsername());
    }

    @Subscribe
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        logger.info(
                "Resource pack status for {}: {}",
                event.getPlayer().getUsername(),
                event.getStatus()
        );
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
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

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (queueController != null) {
            queueController.onPluginMessage(event);
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (queueController != null) {
            queueController.onServerConnected(event);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (queueController != null) {
            queueController.onDisconnect(event);
        }
    }

    private void registerLobbyCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("lobby")
                .aliases("hub", "로비")
                .plugin(this)
                .build();
        commandManager.register(meta, new LobbyCommand(queueController));
    }

    private void registerSurvivalCommand(CommandManager commandManager) {
        CommandMeta meta = commandManager.metaBuilder("survival")
                .aliases("wild", "야생입장")
                .plugin(this)
                .build();
        commandManager.register(meta, new SurvivalQueueCommand(queueController));
    }

    private void loadResourcePackInfo() {
        Path configPath = dataDirectory.resolve("resourcepack.properties");
        Properties properties = new Properties();
        ResourcePackSettings.defaults().writeDefaultsTo(properties);

        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(configPath)) {
                try (var reader = Files.newBufferedReader(configPath)) {
                    properties.load(reader);
                }
            } else {
                try (var writer = Files.newBufferedWriter(configPath)) {
                    properties.store(writer, "LeeSeolProxy resource pack settings");
                }
            }
        } catch (IOException exception) {
            logger.warn("Failed to load resource pack config. Resource pack offer is disabled.", exception);
            resourcePackInfo = null;
            return;
        }

        resourcePackInfo = resourcePackOfferService.reload(ResourcePackSettings.from(properties));
    }

    private NetworkSettings loadNetworkSettings() {
        Path configPath = dataDirectory.resolve("network.properties");
        Properties properties = new Properties();
        NetworkSettings.defaults().writeDefaultsTo(properties);

        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(configPath)) {
                try (var reader = Files.newBufferedReader(configPath)) {
                    properties.load(reader);
                }
            } else {
                try (var writer = Files.newBufferedWriter(configPath)) {
                    properties.store(writer, "LeeSeolProxy network settings");
                }
            }
        } catch (IOException exception) {
            logger.warn("Failed to load network settings. Using safe fallback defaults.", exception);
        }

        return NetworkSettings.from(properties);
    }

    private QueueSettings loadQueueSettings() {
        Path configPath = dataDirectory.resolve("queue.properties");
        Properties properties = new Properties();
        QueueSettings.defaults().writeDefaultsTo(properties);

        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(configPath)) {
                try (var reader = Files.newBufferedReader(configPath)) {
                    properties.load(reader);
                }
            } else {
                try (var writer = Files.newBufferedWriter(configPath)) {
                    properties.store(writer, "LeeSeolProxy survival queue settings");
                }
            }
        } catch (IOException exception) {
            logger.warn("Failed to load queue settings. Using safe defaults.", exception);
        }

        return QueueSettings.from(properties);
    }

}
