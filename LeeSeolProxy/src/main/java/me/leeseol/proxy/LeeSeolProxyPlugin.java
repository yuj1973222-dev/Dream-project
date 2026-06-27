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
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import me.leeseol.proxy.command.LobbyCommand;
import me.leeseol.proxy.command.ServerListCommand;
import me.leeseol.proxy.command.SurvivalQueueCommand;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.queue.SurvivalQueueController;
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
    private static final String DEFAULT_RESOURCE_PACK_URL = "http://34.64.126.179:8163/generated.zip";
    private static final String DEFAULT_RESOURCE_PACK_SHA1 = "6484feef71105bfd2a2d6acdcc2af6a1bde2f598";

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private ResourcePackInfo resourcePackInfo;
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
        properties.setProperty("enabled", "true");
        properties.setProperty("url", DEFAULT_RESOURCE_PACK_URL);
        properties.setProperty("sha1", DEFAULT_RESOURCE_PACK_SHA1);
        properties.setProperty("force", "false");
        properties.setProperty("prompt", "익스페디션 서버 리소스팩을 적용합니다.");

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

        boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
        if (!enabled) {
            resourcePackInfo = null;
            logger.info("Velocity resource pack offer disabled.");
            return;
        }

        String url = properties.getProperty("url", "").trim();
        String sha1 = properties.getProperty("sha1", "").trim();
        String prompt = properties.getProperty("prompt", "").trim();
        boolean force = Boolean.parseBoolean(properties.getProperty("force", "false"));

        if (url.isEmpty() || sha1.isEmpty()) {
            resourcePackInfo = null;
            logger.warn("Resource pack url or sha1 is empty. Resource pack offer is disabled.");
            return;
        }

        try {
            ResourcePackInfo.Builder builder = proxy.createResourcePackBuilder(url)
                    .setHash(hexToBytes(sha1))
                    .setShouldForce(force);
            if (!prompt.isEmpty()) {
                builder.setPrompt(Component.text(prompt));
            }
            resourcePackInfo = builder.build();
            logger.info("Velocity resource pack offer enabled: {}", url);
        } catch (IllegalArgumentException exception) {
            resourcePackInfo = null;
            logger.warn("Invalid resource pack settings. Resource pack offer is disabled.", exception);
        }
    }

    private NetworkSettings loadNetworkSettings() {
        Path configPath = dataDirectory.resolve("network.properties");
        Properties properties = new Properties();
        properties.setProperty("maintenance", "false");
        properties.setProperty("maintenance-message", "로비 점검 중입니다. 잠시 후 다시 접속해주세요.");
        properties.setProperty("fallback-enabled", "true");
        properties.setProperty("fallback-server", "lobby");
        properties.setProperty("fallback-from", "survival,newworld");
        properties.setProperty("fallback-message", "서버 연결이 끊겨 로비로 이동합니다.");
        properties.setProperty("fallback-unavailable-message", "로비가 열려 있지 않아 접속할 수 없습니다.");

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

        boolean maintenance = Boolean.parseBoolean(properties.getProperty("maintenance", "false"));
        boolean fallbackEnabled = Boolean.parseBoolean(properties.getProperty("fallback-enabled", "true"));
        String fallbackServer = properties.getProperty("fallback-server", "lobby").trim();
        Set<String> fallbackFrom = Arrays.stream(properties.getProperty("fallback-from", "survival,newworld").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return new NetworkSettings(
                maintenance,
                properties.getProperty("maintenance-message", "로비 점검 중입니다. 잠시 후 다시 접속해주세요."),
                fallbackEnabled,
                fallbackServer.isBlank() ? "lobby" : fallbackServer,
                fallbackFrom.isEmpty() ? Set.of("survival", "newworld") : fallbackFrom,
                properties.getProperty("fallback-message", "서버 연결이 끊겨 로비로 이동합니다."),
                properties.getProperty("fallback-unavailable-message", "로비가 열려 있지 않아 접속할 수 없습니다.")
        );
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

    private static byte[] hexToBytes(String hex) {
        String normalized = hex.trim();
        if (normalized.length() != 40) {
            throw new IllegalArgumentException("SHA1 must be 40 hex characters.");
        }

        byte[] bytes = new byte[20];
        for (int index = 0; index < bytes.length; index++) {
            int high = Character.digit(normalized.charAt(index * 2), 16);
            int low = Character.digit(normalized.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("SHA1 contains non-hex characters.");
            }
            bytes[index] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private record NetworkSettings(
            boolean maintenance,
            String maintenanceMessage,
            boolean fallbackEnabled,
            String fallbackServer,
            Set<String> fallbackFrom,
            String fallbackMessage,
            String fallbackUnavailableMessage
    ) {
        private static NetworkSettings defaults() {
            return new NetworkSettings(
                    false,
                    "로비 점검 중입니다. 잠시 후 다시 접속해주세요.",
                    true,
                    "lobby",
                    Set.of("survival", "newworld"),
                    "서버 연결이 끊겨 로비로 이동합니다.",
                    "로비가 열려 있지 않아 접속할 수 없습니다."
            );
        }
    }
}
