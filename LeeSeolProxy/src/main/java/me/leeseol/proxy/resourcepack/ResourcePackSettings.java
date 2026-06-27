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
