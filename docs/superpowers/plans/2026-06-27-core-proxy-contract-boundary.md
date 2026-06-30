# Core Proxy Contract Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split LeeSeolCore's Paper-side movement request boundary from LeeSeolProxy's Velocity-side routing/config/resource-pack ownership without changing player-facing commands or deploy targets.

**Architecture:** Keep the current jars and descriptors. LeeSeolCore gains a narrow `networkmove` port that emits BungeeCord-compatible movement requests, while LeeSeolProxy extracts settings and resource-pack handling into focused classes so the plugin class wires lifecycle instead of owning parsing and policy details.

**Tech Stack:** Java 25 for LeeSeolCore, Java 21 for LeeSeolProxy, Paper API, Velocity API, JUnit 4, Maven.

---

## Scope

This plan covers the first executable slice of the approved decomposition design:

- Core movement boundary.
- Proxy settings/resource-pack extraction.
- Proxy lifecycle wiring cleanup.
- Contract verification for Core movement and Proxy queue/resource-pack surfaces.

This plan does not split jars, deploy servers, restart services, or refactor LeeSeolTown. LeeSeolTown gets a separate plan after this Core/Proxy slice is complete.

## Files

Create:

- `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordConnectPayload.java`
- `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/NetworkMovePort.java`
- `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordNetworkMovePort.java`
- `LeeSeolCore/src/test/java/me/leeseol/core/networkmove/BungeeCordConnectPayloadTest.java`
- `LeeSeolProxy/src/main/java/me/leeseol/proxy/config/PropertiesConfigFile.java`
- `LeeSeolProxy/src/main/java/me/leeseol/proxy/network/NetworkSettings.java`
- `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackSettings.java`
- `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackOfferService.java`
- `LeeSeolProxy/src/test/java/me/leeseol/proxy/network/NetworkSettingsTest.java`
- `LeeSeolProxy/src/test/java/me/leeseol/proxy/resourcepack/ResourcePackSettingsTest.java`

Modify:

- `LeeSeolCore/src/main/java/me/leeseol/core/LeeSeolCorePlugin.java`
- `LeeSeolCore/src/main/java/me/leeseol/core/portal/PortalManager.java`
- `LeeSeolCore/src/main/java/me/leeseol/core/listener/RespawnToLobbyListener.java`
- `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`

Do not modify:

- `LeeSeolTown/**`
- `LeeSeolLobby/**`
- Remote server files
- Deployed jars

## Task 1: Add Core Movement Payload Contract

**Files:**

- Create: `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordConnectPayload.java`
- Create: `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/NetworkMovePort.java`
- Create: `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordNetworkMovePort.java`
- Test: `LeeSeolCore/src/test/java/me/leeseol/core/networkmove/BungeeCordConnectPayloadTest.java`

- [ ] **Step 1: Write the failing payload contract test**

Create `LeeSeolCore/src/test/java/me/leeseol/core/networkmove/BungeeCordConnectPayloadTest.java`:

```java
package me.leeseol.core.networkmove;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import org.junit.Test;

public final class BungeeCordConnectPayloadTest {
    @Test
    public void connectPayloadUsesBungeeCordConnectSubchannel() throws Exception {
        byte[] payload = BungeeCordConnectPayload.connect("survival");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("survival", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test
    public void connectPayloadTrimsTargetServer() throws Exception {
        byte[] payload = BungeeCordConnectPayload.connect("  lobby  ");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            assertEquals("Connect", input.readUTF());
            assertEquals("lobby", input.readUTF());
            assertEquals(0, input.available());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectPayloadRejectsBlankTargetServer() {
        BungeeCordConnectPayload.connect(" ");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -f LeeSeolCore/pom.xml -Dtest=BungeeCordConnectPayloadTest test
```

Expected: FAIL because `BungeeCordConnectPayload` does not exist.

- [ ] **Step 3: Create the payload helper**

Create `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordConnectPayload.java`:

```java
package me.leeseol.core.networkmove;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class BungeeCordConnectPayload {
    private BungeeCordConnectPayload() {
    }

    public static byte[] connect(String targetServer) {
        if (targetServer == null || targetServer.isBlank()) {
            throw new IllegalArgumentException("targetServer must not be blank");
        }

        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(byteStream)) {
                output.writeUTF("Connect");
                output.writeUTF(targetServer.trim());
            }
            return byteStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode BungeeCord Connect payload", exception);
        }
    }
}
```

- [ ] **Step 4: Create the movement port interface**

Create `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/NetworkMovePort.java`:

```java
package me.leeseol.core.networkmove;

import org.bukkit.entity.Player;

public interface NetworkMovePort {
    void requestMove(Player player, String targetServer);
}
```

- [ ] **Step 5: Create the BungeeCord movement adapter**

Create `LeeSeolCore/src/main/java/me/leeseol/core/networkmove/BungeeCordNetworkMovePort.java`:

```java
package me.leeseol.core.networkmove;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class BungeeCordNetworkMovePort implements NetworkMovePort {
    public static final String CHANNEL = "BungeeCord";

    private final Plugin plugin;

    public BungeeCordNetworkMovePort(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void requestMove(Player player, String targetServer) {
        if (player == null || targetServer == null || targetServer.isBlank()) {
            return;
        }

        player.sendPluginMessage(plugin, CHANNEL, BungeeCordConnectPayload.connect(targetServer));
    }
}
```

- [ ] **Step 6: Run the Core payload test**

Run:

```powershell
mvn -f LeeSeolCore/pom.xml -Dtest=BungeeCordConnectPayloadTest test
```

Expected: PASS, 3 tests.

- [ ] **Step 7: Commit Task 1**

Run:

```powershell
git add -- LeeSeolCore/src/main/java/me/leeseol/core/networkmove LeeSeolCore/src/test/java/me/leeseol/core/networkmove/BungeeCordConnectPayloadTest.java
git commit -m "refactor(core): add network move boundary"
```

## Task 2: Route Core Movement Through the Port

**Files:**

- Modify: `LeeSeolCore/src/main/java/me/leeseol/core/LeeSeolCorePlugin.java`
- Modify: `LeeSeolCore/src/main/java/me/leeseol/core/portal/PortalManager.java`
- Modify: `LeeSeolCore/src/main/java/me/leeseol/core/listener/RespawnToLobbyListener.java`
- Test: `LeeSeolCore/src/test/java/me/leeseol/core/networkmove/BungeeCordConnectPayloadTest.java`

- [ ] **Step 1: Update LeeSeolCorePlugin imports and fields**

In `LeeSeolCore/src/main/java/me/leeseol/core/LeeSeolCorePlugin.java`, add these imports:

```java
import me.leeseol.core.networkmove.BungeeCordNetworkMovePort;
import me.leeseol.core.networkmove.NetworkMovePort;
```

Remove these imports:

```java
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
```

Add this field next to the other service fields:

```java
private NetworkMovePort networkMovePort;
```

- [ ] **Step 2: Instantiate the movement port during enable**

In `onEnable`, after `survivalSpawnManager = new SurvivalSpawnManager(this);`, add:

```java
networkMovePort = new BungeeCordNetworkMovePort(this);
```

- [ ] **Step 3: Replace sendPlayerToServer with a delegating compatibility method**

Replace the existing `sendPlayerToServer` method in `LeeSeolCorePlugin.java` with:

```java
public void sendPlayerToServer(Player player, String targetServer) {
    networkMovePort.requestMove(player, targetServer);
}

public NetworkMovePort networkMovePort() {
    return networkMovePort;
}
```

- [ ] **Step 4: Replace PortalManager direct BungeeCord encoding**

In `LeeSeolCore/src/main/java/me/leeseol/core/portal/PortalManager.java`, remove these imports:

```java
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
```

Replace the entire `connectToVelocityServer` method with:

```java
private void connectToVelocityServer(Player player, String targetServer) {
    plugin.sendPlayerToServer(player, targetServer);
}
```

- [ ] **Step 5: Replace RespawnToLobbyListener direct BungeeCord encoding**

In `LeeSeolCore/src/main/java/me/leeseol/core/listener/RespawnToLobbyListener.java`, remove these imports:

```java
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
```

Replace this block:

```java
// Velocity 환경에서 동작 확인 필요: BungeeCord 호환 Connect subchannel을 사용한다.
try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
     DataOutputStream output = new DataOutputStream(byteStream)) {
    output.writeUTF("Connect");
    output.writeUTF(targetServer);
    player.sendPluginMessage(plugin, "BungeeCord", byteStream.toByteArray());
} catch (IOException exception) {
    plugin.getLogger().warning("Failed to send respawn lobby connect message: " + exception.getMessage());
}
```

with:

```java
plugin.sendPlayerToServer(player, targetServer);
```

- [ ] **Step 6: Run Core tests and package**

Run:

```powershell
mvn -f LeeSeolCore/pom.xml test package
```

Expected: PASS. Existing content tests and the new networkmove test pass, and `LeeSeolCore/target/LeeSeolCore-0.1.0.jar` is produced.

- [ ] **Step 7: Commit Task 2**

Run:

```powershell
git add -- LeeSeolCore/src/main/java/me/leeseol/core/LeeSeolCorePlugin.java LeeSeolCore/src/main/java/me/leeseol/core/portal/PortalManager.java LeeSeolCore/src/main/java/me/leeseol/core/listener/RespawnToLobbyListener.java
git commit -m "refactor(core): route movement through network port"
```

## Task 3: Extract Proxy Network Settings

**Files:**

- Create: `LeeSeolProxy/src/main/java/me/leeseol/proxy/network/NetworkSettings.java`
- Test: `LeeSeolProxy/src/test/java/me/leeseol/proxy/network/NetworkSettingsTest.java`
- Modify: `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`

- [ ] **Step 1: Write the failing NetworkSettings test**

Create `LeeSeolProxy/src/test/java/me/leeseol/proxy/network/NetworkSettingsTest.java`:

```java
package me.leeseol.proxy.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Set;
import org.junit.Test;

public final class NetworkSettingsTest {
    @Test
    public void defaultsKeepCurrentNetworkContract() {
        NetworkSettings settings = NetworkSettings.defaults();

        assertFalse(settings.maintenance());
        assertTrue(settings.fallbackEnabled());
        assertEquals("lobby", settings.fallbackServer());
        assertEquals(Set.of("survival", "newworld"), settings.fallbackFrom());
    }

    @Test
    public void fromPropertiesNormalizesFallbackSources() {
        Properties properties = new Properties();
        NetworkSettings.defaults().writeDefaultsTo(properties);
        properties.setProperty("maintenance", "true");
        properties.setProperty("fallback-server", "  lobby  ");
        properties.setProperty("fallback-from", " Survival, NEWWORLD, , ");

        NetworkSettings settings = NetworkSettings.from(properties);

        assertTrue(settings.maintenance());
        assertEquals("lobby", settings.fallbackServer());
        assertEquals(Set.of("survival", "newworld"), settings.fallbackFrom());
    }

    @Test
    public void blankFallbackServerUsesLobby() {
        Properties properties = new Properties();
        NetworkSettings.defaults().writeDefaultsTo(properties);
        properties.setProperty("fallback-server", " ");

        assertEquals("lobby", NetworkSettings.from(properties).fallbackServer());
    }
}
```

- [ ] **Step 2: Run the NetworkSettings test to verify it fails**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml -Dtest=NetworkSettingsTest test
```

Expected: FAIL because `NetworkSettings` does not exist.

- [ ] **Step 3: Create NetworkSettings**

Create `LeeSeolProxy/src/main/java/me/leeseol/proxy/network/NetworkSettings.java`:

```java
package me.leeseol.proxy.network;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public record NetworkSettings(
        boolean maintenance,
        String maintenanceMessage,
        boolean fallbackEnabled,
        String fallbackServer,
        Set<String> fallbackFrom,
        String fallbackMessage,
        String fallbackUnavailableMessage
) {
    public static NetworkSettings defaults() {
        return new NetworkSettings(
                false,
                "濡쒕퉬 ?먭? 以묒엯?덈떎. ?좎떆 ???ㅼ떆 ?묒냽?댁＜?몄슂.",
                true,
                "lobby",
                Set.of("survival", "newworld"),
                "?쒕쾭 ?곌껐???딄꺼 濡쒕퉬濡??대룞?⑸땲??",
                "濡쒕퉬媛 ?대젮 ?덉? ?딆븘 ?묒냽?????놁뒿?덈떎."
        );
    }

    public static NetworkSettings from(Properties properties) {
        NetworkSettings defaults = defaults();
        boolean maintenance = Boolean.parseBoolean(properties.getProperty("maintenance", Boolean.toString(defaults.maintenance())));
        boolean fallbackEnabled = Boolean.parseBoolean(properties.getProperty("fallback-enabled", Boolean.toString(defaults.fallbackEnabled())));
        String fallbackServer = text(properties, "fallback-server", defaults.fallbackServer());
        Set<String> fallbackFrom = Arrays.stream(properties.getProperty("fallback-from", "survival,newworld").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        return new NetworkSettings(
                maintenance,
                text(properties, "maintenance-message", defaults.maintenanceMessage()),
                fallbackEnabled,
                fallbackServer.isBlank() ? defaults.fallbackServer() : fallbackServer,
                fallbackFrom.isEmpty() ? defaults.fallbackFrom() : fallbackFrom,
                text(properties, "fallback-message", defaults.fallbackMessage()),
                text(properties, "fallback-unavailable-message", defaults.fallbackUnavailableMessage())
        );
    }

    public void writeDefaultsTo(Properties properties) {
        properties.setProperty("maintenance", Boolean.toString(maintenance));
        properties.setProperty("maintenance-message", maintenanceMessage);
        properties.setProperty("fallback-enabled", Boolean.toString(fallbackEnabled));
        properties.setProperty("fallback-server", fallbackServer);
        properties.setProperty("fallback-from", String.join(",", fallbackFrom));
        properties.setProperty("fallback-message", fallbackMessage);
        properties.setProperty("fallback-unavailable-message", fallbackUnavailableMessage);
    }

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key, fallback);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
```

- [ ] **Step 4: Replace the inner NetworkSettings record**

In `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`:

Add this import:

```java
import me.leeseol.proxy.network.NetworkSettings;
```

Remove these imports when they are no longer used:

```java
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
```

Delete the private inner `NetworkSettings` record at the bottom of the file.

- [ ] **Step 5: Update loadNetworkSettings to use the extracted record**

Replace the body of `loadNetworkSettings()` with:

```java
private NetworkSettings loadNetworkSettings() {
    Path configPath = dataDirectory.resolve("network.properties");
    Properties properties = new Properties();
    NetworkSettings.defaults().writeDefaultsTo(properties);

    try {
        Files.createDirectories(dataDirectory);
        if (Files.exists(configPath)) {
            try (var reader = Files.newBufferedReader(configPath)) {
                properties.load(reader);
            }
        } else {
            try (var writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "LeeSeolProxy network settings");
            }
        }
    } catch (IOException exception) {
        logger.warn("Failed to load network settings. Using safe fallback defaults.", exception);
    }

    return NetworkSettings.from(properties);
}
```

- [ ] **Step 6: Run Proxy NetworkSettings tests**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml -Dtest=NetworkSettingsTest test
```

Expected: PASS, 3 tests.

- [ ] **Step 7: Run all existing Proxy tests**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml test
```

Expected: PASS. Existing queue and plugin-message contract tests still pass.

- [ ] **Step 8: Commit Task 3**

Run:

```powershell
git add -- LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java LeeSeolProxy/src/main/java/me/leeseol/proxy/network/NetworkSettings.java LeeSeolProxy/src/test/java/me/leeseol/proxy/network/NetworkSettingsTest.java
git commit -m "refactor(proxy): extract network settings"
```

## Task 4: Extract Proxy Resource Pack Settings and Offer Service

**Files:**

- Create: `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackSettings.java`
- Create: `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackOfferService.java`
- Test: `LeeSeolProxy/src/test/java/me/leeseol/proxy/resourcepack/ResourcePackSettingsTest.java`
- Modify: `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`

- [ ] **Step 1: Write the failing ResourcePackSettings test**

Create `LeeSeolProxy/src/test/java/me/leeseol/proxy/resourcepack/ResourcePackSettingsTest.java`:

```java
package me.leeseol.proxy.resourcepack;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;

public final class ResourcePackSettingsTest {
    @Test
    public void defaultsKeepNetworkPackContract() {
        ResourcePackSettings settings = ResourcePackSettings.defaults();

        assertTrue(settings.enabled());
        assertEquals("http://34.64.126.179:8163/generated.zip", settings.url());
        assertEquals("6484feef71105bfd2a2d6acdcc2af6a1bde2f598", settings.sha1());
        assertFalse(settings.force());
    }

    @Test
    public void fromPropertiesTrimsTextFields() {
        Properties properties = new Properties();
        ResourcePackSettings.defaults().writeDefaultsTo(properties);
        properties.setProperty("enabled", "false");
        properties.setProperty("url", "  https://example.test/pack.zip  ");
        properties.setProperty("sha1", "  0000000000000000000000000000000000000000  ");
        properties.setProperty("force", "true");
        properties.setProperty("prompt", "  Use this pack  ");

        ResourcePackSettings settings = ResourcePackSettings.from(properties);

        assertFalse(settings.enabled());
        assertEquals("https://example.test/pack.zip", settings.url());
        assertEquals("0000000000000000000000000000000000000000", settings.sha1());
        assertTrue(settings.force());
        assertEquals("Use this pack", settings.prompt());
    }

    @Test
    public void sha1BytesDecodesHex() {
        ResourcePackSettings settings = new ResourcePackSettings(
                true,
                "https://example.test/pack.zip",
                "000102030405060708090a0b0c0d0e0f10111213",
                false,
                ""
        );

        assertArrayEquals(
                new byte[] {
                        0x00, 0x01, 0x02, 0x03, 0x04,
                        0x05, 0x06, 0x07, 0x08, 0x09,
                        0x0a, 0x0b, 0x0c, 0x0d, 0x0e,
                        0x0f, 0x10, 0x11, 0x12, 0x13
                },
                settings.sha1Bytes()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void sha1BytesRejectsInvalidLength() {
        new ResourcePackSettings(true, "https://example.test/pack.zip", "abc", false, "").sha1Bytes();
    }
}
```

- [ ] **Step 2: Run the ResourcePackSettings test to verify it fails**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml -Dtest=ResourcePackSettingsTest test
```

Expected: FAIL because `ResourcePackSettings` does not exist.

- [ ] **Step 3: Create ResourcePackSettings**

Create `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackSettings.java`:

```java
package me.leeseol.proxy.resourcepack;

import java.util.Properties;

public record ResourcePackSettings(
        boolean enabled,
        String url,
        String sha1,
        boolean force,
        String prompt
) {
    public static ResourcePackSettings defaults() {
        return new ResourcePackSettings(
                true,
                "http://34.64.126.179:8163/generated.zip",
                "6484feef71105bfd2a2d6acdcc2af6a1bde2f598",
                false,
                "?듭뒪?섎뵒???쒕쾭 由ъ냼?ㅽ뙥???곸슜?⑸땲??"
        );
    }

    public static ResourcePackSettings from(Properties properties) {
        ResourcePackSettings defaults = defaults();
        return new ResourcePackSettings(
                Boolean.parseBoolean(properties.getProperty("enabled", Boolean.toString(defaults.enabled()))),
                text(properties, "url", defaults.url()),
                text(properties, "sha1", defaults.sha1()),
                Boolean.parseBoolean(properties.getProperty("force", Boolean.toString(defaults.force()))),
                text(properties, "prompt", defaults.prompt())
        );
    }

    public void writeDefaultsTo(Properties properties) {
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("url", url);
        properties.setProperty("sha1", sha1);
        properties.setProperty("force", Boolean.toString(force));
        properties.setProperty("prompt", prompt);
    }

    public byte[] sha1Bytes() {
        String normalized = sha1.trim();
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

    private static String text(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key, fallback);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
```

- [ ] **Step 4: Create ResourcePackOfferService**

Create `LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack/ResourcePackOfferService.java`:

```java
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
```

- [ ] **Step 5: Update LeeSeolProxyPlugin imports and field**

In `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`, add:

```java
import me.leeseol.proxy.resourcepack.ResourcePackOfferService;
import me.leeseol.proxy.resourcepack.ResourcePackSettings;
```

Remove the static default resource-pack constants:

```java
private static final String DEFAULT_RESOURCE_PACK_URL = "http://34.64.126.179:8163/generated.zip";
private static final String DEFAULT_RESOURCE_PACK_SHA1 = "6484feef71105bfd2a2d6acdcc2af6a1bde2f598";
```

Add this field next to `private ResourcePackInfo resourcePackInfo;`:

```java
private ResourcePackOfferService resourcePackOfferService;
```

- [ ] **Step 6: Initialize ResourcePackOfferService**

In `onProxyInitialize`, before `loadResourcePackInfo();`, add:

```java
resourcePackOfferService = new ResourcePackOfferService(proxy, logger);
```

- [ ] **Step 7: Replace loadResourcePackInfo parsing**

Replace the body of `loadResourcePackInfo()` with:

```java
private void loadResourcePackInfo() {
    Path configPath = dataDirectory.resolve("resourcepack.properties");
    Properties properties = new Properties();
    ResourcePackSettings.defaults().writeDefaultsTo(properties);

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

    resourcePackInfo = resourcePackOfferService.reload(ResourcePackSettings.from(properties));
}
```

- [ ] **Step 8: Delete hexToBytes**

Delete the private static `hexToBytes(String hex)` method from `LeeSeolProxyPlugin.java`; the logic now lives in `ResourcePackSettings.sha1Bytes()`.

- [ ] **Step 9: Run ResourcePackSettings tests**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml -Dtest=ResourcePackSettingsTest test
```

Expected: PASS, 4 tests.

- [ ] **Step 10: Run all Proxy tests and package**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml test package
```

Expected: PASS. Existing queue tests, NetworkSettings tests, and ResourcePackSettings tests pass, and `LeeSeolProxy/target/LeeSeolProxy-0.1.0.jar` is produced.

- [ ] **Step 11: Commit Task 4**

Run:

```powershell
git add -- LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java LeeSeolProxy/src/main/java/me/leeseol/proxy/resourcepack LeeSeolProxy/src/test/java/me/leeseol/proxy/resourcepack/ResourcePackSettingsTest.java
git commit -m "refactor(proxy): extract resource pack offer service"
```

## Task 5: Extract Shared Proxy Properties Loading

**Files:**

- Create: `LeeSeolProxy/src/main/java/me/leeseol/proxy/config/PropertiesConfigFile.java`
- Modify: `LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java`

- [ ] **Step 1: Create PropertiesConfigFile**

Create `LeeSeolProxy/src/main/java/me/leeseol/proxy/config/PropertiesConfigFile.java`:

```java
package me.leeseol.proxy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesConfigFile {
    private final Path dataDirectory;

    public PropertiesConfigFile(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public Properties load(String fileName, Properties defaults, String header) throws IOException {
        Properties properties = new Properties();
        properties.putAll(defaults);
        Path configPath = dataDirectory.resolve(fileName);

        Files.createDirectories(dataDirectory);
        if (Files.exists(configPath)) {
            try (var reader = Files.newBufferedReader(configPath)) {
                properties.load(reader);
            }
        } else {
            try (var writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, header);
            }
        }

        return properties;
    }
}
```

- [ ] **Step 2: Add the config loader field**

In `LeeSeolProxyPlugin.java`, add:

```java
import me.leeseol.proxy.config.PropertiesConfigFile;
```

Add this field:

```java
private final PropertiesConfigFile configFiles;
```

Update the constructor:

```java
@Inject
public LeeSeolProxyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
    this.proxy = proxy;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.configFiles = new PropertiesConfigFile(dataDirectory);
}
```

- [ ] **Step 3: Replace resourcepack.properties loading with PropertiesConfigFile**

Replace the body of `loadResourcePackInfo()` with:

```java
private void loadResourcePackInfo() {
    Properties defaults = new Properties();
    ResourcePackSettings.defaults().writeDefaultsTo(defaults);
    try {
        Properties properties = configFiles.load(
                "resourcepack.properties",
                defaults,
                "LeeSeolProxy resource pack settings"
        );
        resourcePackInfo = resourcePackOfferService.reload(ResourcePackSettings.from(properties));
    } catch (IOException exception) {
        logger.warn("Failed to load resource pack config. Resource pack offer is disabled.", exception);
        resourcePackInfo = null;
    }
}
```

- [ ] **Step 4: Replace network.properties loading with PropertiesConfigFile**

Replace the body of `loadNetworkSettings()` with:

```java
private NetworkSettings loadNetworkSettings() {
    Properties defaults = new Properties();
    NetworkSettings.defaults().writeDefaultsTo(defaults);
    try {
        return NetworkSettings.from(configFiles.load(
                "network.properties",
                defaults,
                "LeeSeolProxy network settings"
        ));
    } catch (IOException exception) {
        logger.warn("Failed to load network settings. Using safe fallback defaults.", exception);
        return NetworkSettings.from(defaults);
    }
}
```

- [ ] **Step 5: Replace queue.properties loading with PropertiesConfigFile**

Replace the body of `loadQueueSettings()` with:

```java
private QueueSettings loadQueueSettings() {
    Properties defaults = new Properties();
    QueueSettings.defaults().writeDefaultsTo(defaults);
    try {
        return QueueSettings.from(configFiles.load(
                "queue.properties",
                defaults,
                "LeeSeolProxy survival queue settings"
        ));
    } catch (IOException exception) {
        logger.warn("Failed to load queue settings. Using safe defaults.", exception);
        return QueueSettings.from(defaults);
    }
}
```

- [ ] **Step 6: Keep required imports and remove only unused imports**

Keep this import because the loader call still catches checked failures:

```java
import java.io.IOException;
```

Remove this import from `LeeSeolProxyPlugin.java` if the compiler reports it unused:

```java
import java.nio.file.Files;
```

- [ ] **Step 7: Run Proxy tests and package**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml test package
```

Expected: PASS and `LeeSeolProxy/target/LeeSeolProxy-0.1.0.jar` is produced.

- [ ] **Step 8: Commit Task 5**

Run:

```powershell
git add -- LeeSeolProxy/src/main/java/me/leeseol/proxy/LeeSeolProxyPlugin.java LeeSeolProxy/src/main/java/me/leeseol/proxy/config/PropertiesConfigFile.java
git commit -m "refactor(proxy): share properties config loading"
```

## Task 6: Contract Documentation and Final Verification

**Files:**

- Inspect: `PLUGIN_INDEX.md`
- Modify: `PLUGIN_INDEX.md` only if the current contract map no longer matches code

- [ ] **Step 1: Check whether PLUGIN_INDEX.md still matches code**

Run:

```powershell
rg -n "LeeSeolCore|LeeSeolProxy|BungeeCord|leeseol:queue|network.properties|queue.properties|resourcepack.properties|Contract Smoke Tests" PLUGIN_INDEX.md
```

Expected: The current map still lists Core as the Paper-side owner for BungeeCord movement requests and Proxy as the owner of `leeseol:queue`, `network.properties`, `queue.properties`, and `resourcepack.properties`.

- [ ] **Step 2: Update PLUGIN_INDEX.md only if needed**

If the previous step shows stale file paths because new focused packages were created, update only the affected rows:

```markdown
`LeeSeolCorePlugin.java`, `networkmove/`, `command/`, `content/`, `portal/`, `launchpad/`, `menu/`, `servernpc/`, `spawn/`, `listener/`, `config.yml`, `plugin.yml`
```

and:

```markdown
`LeeSeolProxyPlugin.java`, `command/`, `queue/`, `network/`, `resourcepack/`, `config/`, `velocity-plugin.json`
```

- [ ] **Step 3: Run Core verification**

Run:

```powershell
mvn -f LeeSeolCore/pom.xml test package
```

Expected: PASS and `LeeSeolCore/target/LeeSeolCore-0.1.0.jar` exists.

- [ ] **Step 4: Run Proxy verification**

Run:

```powershell
mvn -f LeeSeolProxy/pom.xml test package
```

Expected: PASS and `LeeSeolProxy/target/LeeSeolProxy-0.1.0.jar` exists.

- [ ] **Step 5: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` prints no output. `git status --short` shows only intentional files before the final commit.

- [ ] **Step 6: Commit documentation changes if PLUGIN_INDEX.md changed**

If `PLUGIN_INDEX.md` changed, run:

```powershell
git add -- PLUGIN_INDEX.md
git commit -m "docs: update core proxy contract map"
```

If `PLUGIN_INDEX.md` did not change, skip this commit.

- [ ] **Step 7: Final clean check**

Run:

```powershell
git status --short
git log --oneline -5
```

Expected: `git status --short` is empty after the intended commits. The recent log includes the Core movement boundary and Proxy extraction commits.

## Execution Notes

- Keep this implementation sequential. Core and Proxy are high-risk integration owners.
- Do not deploy or restart services during this plan.
- Do not change commands, permissions, plugin channel names, config file names, or deploy targets.
- If Maven fails because dependencies are unavailable, fix the local Maven environment and rerun the same command before reporting the task blocked.
- If any implementation changes a runtime contract, stop and update `PLUGIN_INDEX.md` plus the matching contract smoke row before continuing.
