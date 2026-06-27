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
