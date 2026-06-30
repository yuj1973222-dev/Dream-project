package me.leeseol.town.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class NationColor {
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)^#?[0-9a-f]{6}$");

    private final String key;
    private final String displayName;
    private final String hex;
    private final List<String> gradient;

    public NationColor(String key, String displayName, String hex) {
        this(key, displayName, hex, List.of());
    }

    public NationColor(String key, String displayName, String hex, List<String> gradient) {
        this.key = normalizeKey(key);
        this.displayName = displayName == null || displayName.isBlank() ? this.key : displayName;
        this.hex = normalizeHex(hex, "#8EC5FF");
        this.gradient = gradient == null ? List.of() : List.copyOf(gradient);
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String hex() {
        return hex;
    }

    public List<String> gradient() {
        return gradient;
    }

    public String legacyPrefix() {
        return "&" + hex;
    }

    public static String normalizeKey(String key) {
        return key == null || key.isBlank() ? "blue" : key.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeHex(String hex, String fallback) {
        String value = hex == null || hex.isBlank() ? fallback : hex.trim();
        if (!HEX_PATTERN.matcher(value).matches()) {
            value = fallback;
        }
        if (!value.startsWith("#")) {
            value = "#" + value;
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
