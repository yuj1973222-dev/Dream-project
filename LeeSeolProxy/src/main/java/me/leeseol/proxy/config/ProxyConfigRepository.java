package me.leeseol.proxy.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import me.leeseol.proxy.network.NetworkSettings;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;

public final class ProxyConfigRepository {
    private final PropertiesConfigFile configFiles;

    public ProxyConfigRepository(Path dataDirectory) {
        this.configFiles = new PropertiesConfigFile(dataDirectory);
    }

    public NetworkSettings loadNetworkSettings() throws IOException {
        Properties defaults = new Properties();
        NetworkSettings.defaults().writeDefaultsTo(defaults);
        return NetworkSettings.from(configFiles.load(
                "network.properties",
                defaults,
                "LeeSeolProxy network settings"
        ));
    }

    public QueueSettings loadQueueSettings() throws IOException {
        Properties defaults = new Properties();
        QueueSettings.defaults().writeDefaultsTo(defaults);
        return QueueSettings.from(configFiles.load(
                "queue.properties",
                defaults,
                "LeeSeolProxy survival queue settings"
        ));
    }

    public ResourcePackSettings loadResourcePackSettings() throws IOException {
        Properties defaults = new Properties();
        ResourcePackSettings.defaults().writeDefaultsTo(defaults);
        return ResourcePackSettings.from(configFiles.load(
                "resourcepack.properties",
                defaults,
                "LeeSeolProxy resource pack settings"
        ));
    }
}
