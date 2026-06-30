package me.leeseol.proxy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import me.leeseol.proxy.network.NetworkSettings;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ProxyConfigRepositoryTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsDefaultSettingsAndCreatesExpectedFiles() throws Exception {
        Path dataDirectory = temporaryFolder.newFolder("proxy-data").toPath();
        ProxyConfigRepository repository = new ProxyConfigRepository(dataDirectory);

        NetworkSettings network = repository.loadNetworkSettings();
        QueueSettings queue = repository.loadQueueSettings();
        ResourcePackSettings resourcePack = repository.loadResourcePackSettings();

        assertEquals("lobby", network.fallbackServer());
        assertTrue(network.fallbackFrom().contains("newworld"));
        assertEquals("leeseol:queue", queue.pluginMessageChannel());
        assertEquals("http://34.64.126.179:8163/generated.zip", resourcePack.url());
        assertTrue(dataDirectory.resolve("network.properties").toFile().isFile());
        assertTrue(dataDirectory.resolve("queue.properties").toFile().isFile());
        assertTrue(dataDirectory.resolve("resourcepack.properties").toFile().isFile());
    }
}
