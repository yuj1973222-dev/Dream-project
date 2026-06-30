package me.leeseol.proxy.resourcepack;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import java.io.IOException;
import me.leeseol.proxy.config.ProxyConfigRepository;
import org.slf4j.Logger;

public final class ResourcePackCoordinator {
    private final Logger logger;
    private final ProxyConfigRepository configRepository;
    private final ResourcePackOfferService offerService;
    private ResourcePackInfo resourcePackInfo;

    public ResourcePackCoordinator(ProxyServer proxy, Logger logger, ProxyConfigRepository configRepository) {
        this.logger = logger;
        this.configRepository = configRepository;
        this.offerService = new ResourcePackOfferService(proxy, logger);
    }

    public void reload() {
        try {
            resourcePackInfo = offerService.reload(configRepository.loadResourcePackSettings());
        } catch (IOException exception) {
            logger.warn("Failed to load resource pack config. Resource pack offer is disabled.", exception);
            resourcePackInfo = null;
        }
    }

    public ResourcePackInfo current() {
        return resourcePackInfo;
    }

    public void handlePostLogin(PostLoginEvent event) {
        ResourcePackInfo packInfo = resourcePackInfo;
        if (packInfo == null) {
            return;
        }

        Player player = event.getPlayer();
        player.sendResourcePackOffer(packInfo);
        logger.info("Sent network resource pack offer to {}", player.getUsername());
    }

    public void handleResourcePackStatus(PlayerResourcePackStatusEvent event) {
        logger.info(
                "Resource pack status for {}: {}",
                event.getPlayer().getUsername(),
                event.getStatus()
        );
    }
}
