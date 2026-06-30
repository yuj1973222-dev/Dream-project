package me.leeseol.proxy.network;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.io.IOException;
import java.util.Locale;
import me.leeseol.proxy.config.ProxyConfigRepository;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public final class NetworkRouteService {
    private final ProxyServer proxy;
    private final Logger logger;
    private final ProxyConfigRepository configRepository;
    private NetworkSettings settings = NetworkSettings.defaults();

    public NetworkRouteService(ProxyServer proxy, Logger logger, ProxyConfigRepository configRepository) {
        this.proxy = proxy;
        this.logger = logger;
        this.configRepository = configRepository;
    }

    public NetworkSettings reload() {
        try {
            settings = configRepository.loadNetworkSettings();
        } catch (IOException exception) {
            logger.warn("Failed to load network settings. Using safe fallback defaults.", exception);
            settings = NetworkSettings.defaults();
        }
        return settings;
    }

    public NetworkSettings settings() {
        return settings;
    }

    public void handleLogin(LoginEvent event) {
        NetworkSettings current = reload();
        if (current.maintenance()) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(current.maintenanceMessage())));
        }
    }

    public void handleKickedFromServer(KickedFromServerEvent event) {
        NetworkSettings current = reload();

        String kickedServer = event.getServer().getServerInfo().getName().toLowerCase(Locale.ROOT);
        if (!current.fallbackEnabled() || !current.fallbackFrom().contains(kickedServer)) {
            return;
        }

        if (current.maintenance()) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(current.maintenanceMessage())));
            return;
        }

        String fallbackServerName = current.fallbackServer().toLowerCase(Locale.ROOT);
        if (kickedServer.equals(fallbackServerName)) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(current.fallbackUnavailableMessage())));
            return;
        }

        RegisteredServer fallbackServer = proxy.getServer(current.fallbackServer()).orElse(null);
        if (fallbackServer == null) {
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text(current.fallbackUnavailableMessage())));
            return;
        }

        event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                fallbackServer,
                Component.text(current.fallbackMessage())
        ));
    }
}
