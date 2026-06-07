package me.leeseol.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import me.leeseol.proxy.command.ServerListCommand;
import me.leeseol.proxy.command.SwitchServerCommand;
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

    @Inject
    public LeeSeolProxyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        CommandManager commandManager = proxy.getCommandManager();

        registerSwitchCommand(commandManager, "lobby", new String[]{"hub"}, "lobby");
        registerSwitchCommand(commandManager, "survival", new String[]{"wild"}, "survival");

        CommandMeta serversMeta = commandManager.metaBuilder("servers")
                .aliases("serverlist", "network")
                .plugin(this)
                .build();
        commandManager.register(serversMeta, new ServerListCommand(proxy));

        loadResourcePackInfo();
        logger.info("LeeSeolProxy enabled.");
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

    private void registerSwitchCommand(
            CommandManager commandManager,
            String command,
            String[] aliases,
            String targetServer
    ) {
        CommandMeta meta = commandManager.metaBuilder(command)
                .aliases(aliases)
                .plugin(this)
                .build();
        commandManager.register(meta, new SwitchServerCommand(proxy, targetServer));
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
}
