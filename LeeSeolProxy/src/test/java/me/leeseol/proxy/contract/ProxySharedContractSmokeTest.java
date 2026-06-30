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
        String proxySources = readJavaSources("src/main/java/me/leeseol/proxy");

        assertTrue(proxySources.contains("metaBuilder(\"servers\")"));
        assertTrue(proxySources.contains(".aliases(\"serverlist\", \"network\")"));
        assertTrue(proxySources.contains("metaBuilder(\"lobby\")"));
        assertTrue(proxySources.contains(".aliases(\"hub\""));
        assertTrue(proxySources.contains("metaBuilder(\"survival\")"));
        assertTrue(proxySources.contains(".aliases(\"wild\""));
        assertTrue(proxySources.contains("\"network.properties\""));
        assertTrue(proxySources.contains("\"queue.properties\""));
        assertTrue(proxySources.contains("\"resourcepack.properties\""));
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

    @Test
    public void pluginBootstrapDelegatesLifecycleToProxyServices() throws IOException {
        String pluginSource = readText("src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java");
        String servicesSource = readText("src/main/java/me/leeseol/proxy/ProxyServices.java");

        assertTrue(pluginSource.contains("private ProxyServices services;"));
        assertTrue(pluginSource.contains("services = new ProxyServices(this, proxy, logger, dataDirectory);"));
        assertTrue(pluginSource.contains("services.start();"));
        assertTrue(pluginSource.contains("services.close();"));
        assertTrue(servicesSource.contains("final class ProxyServices"));
        assertTrue(servicesSource.contains("void start()"));
        assertTrue(servicesSource.contains("void close()"));
        assertTrue(servicesSource.contains("SurvivalQueueController queueController()"));
    }

    @Test
    public void networkRouteServiceOwnsMaintenanceAndFallback() throws IOException {
        String servicesSource = readText("src/main/java/me/leeseol/proxy/ProxyServices.java");
        String routeSource = readText("src/main/java/me/leeseol/proxy/network/NetworkRouteService.java");

        assertTrue(servicesSource.contains("new NetworkRouteService(proxy, logger, configRepository)"));
        assertTrue(servicesSource.contains("networkRouteService.handleLogin(event);"));
        assertTrue(servicesSource.contains("networkRouteService.handleKickedFromServer(event);"));
        assertFalse(servicesSource.contains("KickedFromServerEvent.DisconnectPlayer"));

        assertTrue(routeSource.contains("void handleLogin(LoginEvent event)"));
        assertTrue(routeSource.contains("void handleKickedFromServer(KickedFromServerEvent event)"));
        assertTrue(routeSource.contains("configRepository.loadNetworkSettings()"));
        assertTrue(routeSource.contains("fallbackFrom().contains(kickedServer)"));
    }

    @Test
    public void commandRegistrarOwnsVelocityCommandsAndDropsLegacySwitchCommand() throws IOException {
        String servicesSource = readText("src/main/java/me/leeseol/proxy/ProxyServices.java");
        String registrarSource = readText("src/main/java/me/leeseol/proxy/command/ProxyCommandRegistrar.java");

        assertTrue(servicesSource.contains("new ProxyCommandRegistrar(proxy, plugin, queueController).registerAll();"));
        assertFalse(servicesSource.contains("metaBuilder(\"lobby\")"));
        assertFalse(servicesSource.contains("metaBuilder(\"survival\")"));
        assertFalse(servicesSource.contains("registerLobbyCommand"));
        assertFalse(servicesSource.contains("registerSurvivalCommand"));

        assertTrue(registrarSource.contains("metaBuilder(\"servers\")"));
        assertTrue(registrarSource.contains(".aliases(\"serverlist\", \"network\")"));
        assertTrue(registrarSource.contains("metaBuilder(\"lobby\")"));
        assertTrue(registrarSource.contains(".aliases(\"hub\")"));
        assertTrue(registrarSource.contains("metaBuilder(\"survival\")"));
        assertTrue(registrarSource.contains(".aliases(\"wild\")"));
        assertFalse(Files.exists(projectPath("src/main/java/me/leeseol/proxy/command/SwitchServerCommand.java")));
    }

    @Test
    public void resourcePackCoordinatorOwnsVelocityOfferFlow() throws IOException {
        String servicesSource = readText("src/main/java/me/leeseol/proxy/ProxyServices.java");
        String coordinatorSource = readText("src/main/java/me/leeseol/proxy/resourcepack/ResourcePackCoordinator.java");

        assertTrue(servicesSource.contains("new ResourcePackCoordinator(proxy, logger, configRepository)"));
        assertTrue(servicesSource.contains("resourcePackCoordinator.reload();"));
        assertTrue(servicesSource.contains("resourcePackCoordinator.handlePostLogin(event);"));
        assertTrue(servicesSource.contains("resourcePackCoordinator.handleResourcePackStatus(event);"));
        assertFalse(servicesSource.contains("sendResourcePackOffer"));
        assertFalse(servicesSource.contains("loadResourcePackInfo"));
        assertFalse(servicesSource.contains("ResourcePackInfo resourcePackInfo"));

        assertTrue(coordinatorSource.contains("configRepository.loadResourcePackSettings()"));
        assertTrue(coordinatorSource.contains("offerService.reload"));
        assertTrue(coordinatorSource.contains("player.sendResourcePackOffer(packInfo)"));
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
