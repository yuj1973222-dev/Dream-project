package me.leeseol.town.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class NationColorPalette {
    private static final List<NationColor> DEFAULT_COLORS = List.of(
            new NationColor("red", "빨강", "#FF5C7A"),
            new NationColor("orange", "주황", "#FFB86B"),
            new NationColor("yellow", "노랑", "#FFD166"),
            new NationColor("green", "초록", "#8FD9A8"),
            new NationColor("blue", "파랑", "#8EC5FF"),
            new NationColor("purple", "보라", "#C7A8FF")
    );
    private static final Map<String, String> LEGACY_TYPE_COLORS = Map.of(
            "republic", "blue",
            "empire", "red",
            "federation", "yellow"
    );

    private final LinkedHashMap<String, NationColor> colors;

    private NationColorPalette(LinkedHashMap<String, NationColor> colors) {
        this.colors = colors;
    }

    public static NationColorPalette from(ConfigurationSection config) {
        LinkedHashMap<String, NationColor> colors = defaults();
        ConfigurationSection section = config == null ? null : config.getConfigurationSection("nation.colors");
        if (section == null) {
            return new NationColorPalette(colors);
        }

        LinkedHashMap<String, NationColor> configured = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String base = key + ".";
            configured.put(NationColor.normalizeKey(key), new NationColor(
                    key,
                    section.getString(base + "display-name", key),
                    section.getString(base + "hex", "#8EC5FF"),
                    section.getStringList(base + "gradient")
            ));
        }
        return new NationColorPalette(configured.isEmpty() ? colors : configured);
    }

    public List<String> keys() {
        return new ArrayList<>(colors.keySet());
    }

    public NationColor resolve(String key, Set<String> usedKeys) {
        NationColor color = colors.get(NationColor.normalizeKey(key));
        if (color == null || usedKeys.contains(color.key())) {
            return null;
        }
        return color;
    }

    public NationColor resolveSaved(ConfigurationSection section, String legacyType, String nationId, Set<String> usedKeys) {
        if (section != null) {
            String key = NationColor.normalizeKey(section.getString("key"));
            if (!usedKeys.contains(key)) {
                NationColor configured = colors.get(key);
                if (configured != null) {
                    return configured;
                }
                return new NationColor(
                        key,
                        section.getString("display-name", key),
                        section.getString("hex", generatedHex(nationId)),
                        section.getStringList("gradient")
                );
            }
        }
        return resolveLegacy(legacyType, nationId, usedKeys);
    }

    public NationColor resolveLegacy(String legacyType, String nationId, Set<String> usedKeys) {
        String preferredKey = LEGACY_TYPE_COLORS.get(normalizeLegacyType(legacyType));
        if (preferredKey != null) {
            NationColor preferred = colors.get(preferredKey);
            if (preferred != null && !usedKeys.contains(preferred.key())) {
                return preferred;
            }
        }
        for (NationColor color : colors.values()) {
            if (!usedKeys.contains(color.key())) {
                return color;
            }
        }
        String id = NationColor.normalizeKey(nationId);
        return new NationColor("custom:" + id, "사용자 색상", generatedHex(id));
    }

    private static LinkedHashMap<String, NationColor> defaults() {
        LinkedHashMap<String, NationColor> colors = new LinkedHashMap<>();
        for (NationColor color : DEFAULT_COLORS) {
            colors.put(color.key(), color);
        }
        return colors;
    }

    private static String normalizeLegacyType(String legacyType) {
        return legacyType == null ? "" : legacyType.trim().toLowerCase();
    }

    private static String generatedHex(String seed) {
        int hash = Math.abs((seed == null ? "nation" : seed).hashCode());
        int red = 80 + hash % 120;
        int green = 80 + (hash / 7) % 120;
        int blue = 80 + (hash / 13) % 120;
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
