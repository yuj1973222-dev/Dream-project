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
        assertEquals("리소스팩을 적용하면 커스텀 아이템과 UI가 정상적으로 표시됩니다.", settings.prompt());
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
