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
