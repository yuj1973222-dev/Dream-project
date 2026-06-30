package me.leeseol.core.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import me.leeseol.core.content.ContentArea;
import me.leeseol.core.content.ContentEntry;
import me.leeseol.core.content.ContentRegistry;
import me.leeseol.core.content.ContentSpawn;
import me.leeseol.core.content.ContentType;
import me.leeseol.core.networkmove.BungeeCordConnectPayload;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class CoreSharedContractSmokeTest {
    @Test
    public void pluginDescriptorKeepsCoreCommandAndBridgeContracts() {
        YamlConfiguration plugin = loadYaml("src/main/resources/plugin.yml");

        assertEquals("me.leeseol.core.LeeSeolCorePlugin", plugin.getString("main"));
        assertEquals("serverinfo", plugin.getConfigurationSection("commands").getKeys(false).stream()
                .filter("serverinfo"::equals)
                .findFirst()
                .orElseThrow());
        assertTrue(plugin.contains("commands.survivalspawn"));
        assertTrue(plugin.contains("commands.lscore"));
        assertTrue(plugin.contains("commands.leeseolcore"));
        assertTrue(plugin.contains("commands.content"));
        assertEquals(List.of("returnspawn", "lsspawn"), plugin.getStringList("commands.survivalspawn.aliases"));

        assertTrue(plugin.getStringList("softdepend").contains("WorldGuard"));
        assertTrue(plugin.getStringList("softdepend").contains("BlueMap"));
        assertTrue(plugin.getStringList("softdepend").contains("Citizens"));
        assertTrue(plugin.contains("permissions.leeseolcore.servermenu"));
        assertTrue(plugin.contains("permissions.leeseolcore.survivalspawn"));
    }

    @Test
    public void configKeepsSharedDataAndDisplaySurfaceContracts() {
        YamlConfiguration config = loadYaml("src/main/resources/config.yml");

        assertEquals("contents.yml", config.getString("content-registry.data-file"));
        assertTrue(config.getBoolean("content-registry.worldguard.enabled"));
        assertEquals("leeseol-content-areas", config.getString("content-registry.bluemap.marker-set-id"));
        assertTrue(config.contains("survival-spawn-return"));
        assertTrue(config.contains("server-menu"));
        assertTrue(config.contains("server-npcs"));
        assertTrue(config.contains("portal-triggers"));
        assertTrue(config.contains("launch-pads"));

        assertFalse("Respawn-to-lobby remains inactive legacy.", config.getBoolean("respawn-to-lobby.enabled"));
    }

    @Test
    public void contentRegistryKeepsTownNeutralZoneContractStable() {
        assertTrue(ContentType.fromKey("neutral").isPresent());

        ContentRegistry registry = new ContentRegistry();
        ContentEntry entry = registry.add(
                ContentType.NEUTRAL,
                "safe_zone",
                "Safe Zone",
                new ContentArea("world", -8, 60, -8, 8, 80, 8),
                new ContentSpawn("world", 0.5D, 65.0D, 0.5D, 0.0F, 0.0F)
        );

        assertEquals("leeseol_content_neutral_safe_zone", entry.regionId());

        YamlConfiguration yaml = new YamlConfiguration();
        registry.saveTo(yaml);
        assertEquals("neutral", yaml.getString("contents.neutral.safe_zone.type"));
        assertEquals("leeseol_content_neutral_safe_zone", yaml.getString("contents.neutral.safe_zone.region-id"));
    }

    @Test
    public void networkMoveBridgeRemainsPaperConnectOnly() throws Exception {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(BungeeCordConnectPayload.connect("lobby")))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("lobby", input.readUTF());
        }

        String coreSources = readJavaSources("src/main/java/me/leeseol/core");
        assertTrue(coreSources.contains("registerOutgoingPluginChannel(this, BungeeCordNetworkMovePort.CHANNEL)"));
        assertFalse(coreSources.contains("leeseol:queue"));
        assertFalse(coreSources.toLowerCase().contains("resource-pack"));
        assertFalse(coreSources.toLowerCase().contains("resourcepack"));
    }

    @Test
    public void respawnToLobbyListenerStaysUnregisteredLegacy() throws IOException {
        String pluginSource = readText("src/main/java/me/leeseol/core/LeeSeolCorePlugin.java");

        assertTrue(Files.exists(projectPath("src/main/java/me/leeseol/core/listener/RespawnToLobbyListener.java")));
        assertFalse(pluginSource.contains("new RespawnToLobbyListener"));
    }

    @Test
    public void bootstrapDelegatesDomainManagersToCoreServicesFacade() throws IOException {
        String pluginSource = readText("src/main/java/me/leeseol/core/LeeSeolCorePlugin.java");
        String servicesSource = readText("src/main/java/me/leeseol/core/CoreServices.java");

        assertTrue(pluginSource.contains("private CoreServices services;"));
        assertTrue(pluginSource.contains("services = new CoreServices(this);"));
        assertTrue(pluginSource.contains("services.reloadAll();"));
        assertTrue(servicesSource.contains("final class CoreServices"));
        assertTrue(servicesSource.contains("void reloadAll()"));
        assertTrue(servicesSource.contains("void disableAll()"));
        assertTrue(servicesSource.contains("ContentService contentService()"));
        assertTrue(servicesSource.contains("NetworkMovePort networkMovePort()"));
    }

    @Test
    public void networkMoveServiceWrapsOnlyThePaperConnectBridge() throws IOException {
        String pluginSource = readText("src/main/java/me/leeseol/core/LeeSeolCorePlugin.java");
        String servicesSource = readText("src/main/java/me/leeseol/core/CoreServices.java");
        String networkMoveSource = readText("src/main/java/me/leeseol/core/networkmove/NetworkMoveService.java");

        assertTrue(servicesSource.contains("new NetworkMoveService(new BungeeCordNetworkMovePort(plugin))"));
        assertTrue(pluginSource.contains("services.networkMoveService().move(player, targetServer);"));
        assertTrue(networkMoveSource.contains("move(Player player, String targetServer)"));
        assertTrue(networkMoveSource.contains("movePort.requestMove(player, targetServer);"));
        assertFalse(networkMoveSource.toLowerCase().contains("queue"));
        assertFalse(networkMoveSource.toLowerCase().contains("resource"));
    }

    private static YamlConfiguration loadYaml(String path) {
        return YamlConfiguration.loadConfiguration(projectPath(path).toFile());
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
        return Path.of("LeeSeolCore").resolve(path);
    }
}
