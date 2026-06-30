package me.leeseol.proxy.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import me.leeseol.proxy.queue.QueuePluginMessage;
import me.leeseol.proxy.queue.QueueSettings;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;
import org.junit.Test;

public final class ProxySharedContractSmokeTest {
    @Test
    public void descriptorKeepsVelocityPluginIdentity() throws IOException {
        String descriptor = readText("src/main/resources/velocity-plugin.json");

        assertTrue(descriptor.contains("\"id\": \"leeseolproxy\""));
        assertTrue(descriptor.contains("\"name\": \"LeeSeolProxy\""));
        assertTrue(descriptor.contains("\"version\": \"0.1.0\""));
        assertTrue(descriptor.contains("\"main\": \"me.leeseol.proxy.LeeSeolProxyPlugin\""));
    }

    @Test
    public void proxyOwnsVelocityCommandSurfaceAndPropertiesFiles() throws IOException {
        String pluginSource = readText("src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java");

        assertTrue(pluginSource.contains("metaBuilder(\"servers\")"));
        assertTrue(pluginSource.contains(".aliases(\"serverlist\", \"network\")"));
        assertTrue(pluginSource.contains("metaBuilder(\"lobby\")"));
        assertTrue(pluginSource.contains(".aliases(\"hub\""));
        assertTrue(pluginSource.contains("metaBuilder(\"survival\")"));
        assertTrue(pluginSource.contains(".aliases(\"wild\""));
        assertTrue(pluginSource.contains("\"network.properties\""));
        assertTrue(pluginSource.contains("\"queue.properties\""));
        assertTrue(pluginSource.contains("\"resourcepack.properties\""));
    }

    @Test
    public void queueAndLobbyPluginMessageContractStaysStable() {
        assertEquals("leeseol:queue", QueueSettings.defaults().pluginMessageChannel());
        assertEquals("limbo-request", QueuePluginMessage.LIMBO_REQUEST);
        assertEquals("limbo-result", QueuePluginMessage.LIMBO_RESULT);
        assertEquals("lobby-request", QueuePluginMessage.LOBBY_REQUEST);
        assertEquals("queue-leave", QueuePluginMessage.QUEUE_LEAVE);
    }

    @Test
    public void resourcePackOfferContractStaysVelocityOwned() throws IOException {
        ResourcePackSettings settings = ResourcePackSettings.defaults();
        String proxySources = readJavaSources("src/main/java/me/leeseol/proxy");

        assertTrue(settings.enabled());
        assertEquals("http://34.64.126.179:8163/generated.zip", settings.url());
        assertEquals("6484feef71105bfd2a2d6acdcc2af6a1bde2f598", settings.sha1());
        assertTrue(proxySources.contains("sendResourcePackOffer"));
        assertTrue(proxySources.contains("ResourcePackOfferService"));
        assertFalse(proxySources.contains("ItemsAdder"));
    }

    @Test
    public void proxyDoesNotTakePaperCoreResponsibilities() throws IOException {
        String proxySources = readJavaSources("src/main/java/me/leeseol/proxy");

        assertFalse(proxySources.contains("org.bukkit"));
        assertFalse(proxySources.contains("plugin.yml"));
        assertFalse(proxySources.contains("/content"));
        assertFalse(proxySources.contains("contents.yml"));
        assertFalse(proxySources.contains("survival-spawn-return"));
    }

    private static String readText(String path) throws IOException {
        return Files.readString(projectPath(path), StandardCharsets.UTF_8);
    }

    private static String readJavaSources(String path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (var stream = Files.walk(projectPath(path))) {
            for (Path source : stream.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                builder.append(Files.readString(source, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return builder.toString();
    }

    private static Path projectPath(String path) {
        Path modulePath = Path.of(path);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("LeeSeolProxy").resolve(path);
    }
}
