package me.leeseol.town.model;

import java.util.Locale;

public enum WarMode {
    INVASION("invasion", "\uCE68\uACF5", "\uCE68\uACF5"),
    TOTAL("total", "\uCD1D\uB825\uC804", "\uCD1D\uB825\uC804");

    private final String id;
    private final String displayName;
    private final String[] aliases;

    WarMode(String id, String displayName, String... aliases) {
        this.id = id;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static WarMode parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        for (WarMode mode : values()) {
            if (mode.id.equals(value) || mode.name().equalsIgnoreCase(value) || mode.displayName.equals(input.trim())) {
                return mode;
            }
            for (String alias : mode.aliases) {
                if (alias.equalsIgnoreCase(input.trim())) {
                    return mode;
                }
            }
        }
        return null;
    }

    public static WarMode parseOrDefault(String input) {
        WarMode mode = parse(input);
        return mode == null ? INVASION : mode;
    }
}
