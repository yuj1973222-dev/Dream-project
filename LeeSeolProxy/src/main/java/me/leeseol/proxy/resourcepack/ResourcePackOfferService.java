package me.leeseol.proxy.resourcepack;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public final class ResourcePackOfferService {
    private final ProxyServer proxy;
    private final Logger logger;
    private ResourcePackInfo resourcePackInfo;

    public ResourcePackOfferService(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    public ResourcePackInfo reload(ResourcePackSettings settings) {
        if (!settings.enabled()) {
            resourcePackInfo = null;
            logger.info("Velocity resource pack offer disabled.");
            return null;
        }

        if (settings.url().isBlank() || settings.sha1().isBlank()) {
            resourcePackInfo = null;
            logger.warn("Resource pack url or sha1 is empty. Resource pack offer is disabled.");
            return null;
        }

        try {
            ResourcePackInfo.Builder builder = proxy.createResourcePackBuilder(settings.url())
                    .setHash(settings.sha1Bytes())
                    .setShouldForce(settings.force());
            if (!settings.prompt().isBlank()) {
                builder.setPrompt(Component.text(settings.prompt()));
            }
            resourcePackInfo = builder.build();
            logger.info("Velocity resource pack offer enabled: {}", settings.url());
            return resourcePackInfo;
        } catch (IllegalArgumentException exception) {
            resourcePackInfo = null;
            logger.warn("Invalid resource pack settings. Resource pack offer is disabled.", exception);
            return null;
        }
    }

    public ResourcePackInfo current() {
        return resourcePackInfo;
    }
}
