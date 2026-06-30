package me.leeseol.proxy;

import com.google.inject.Inject;
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
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
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
    private ProxyServices services;

    @Inject
    public LeeSeolProxyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        services = new ProxyServices(this, proxy, logger, dataDirectory);
        services.start();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (services != null) {
            services.close();
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (services != null) {
            services.handleLogin(event);
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (services != null) {
            services.handlePostLogin(event);
        }
    }

    @Subscribe
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (services != null) {
            services.handleResourcePackStatus(event);
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (services != null) {
            services.handleKickedFromServer(event);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (services != null) {
            services.handlePluginMessage(event);
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (services != null) {
            services.handleServerConnected(event);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (services != null) {
            services.handleDisconnect(event);
        }
    }
}
