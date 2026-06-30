package me.leeseol.core.portal;

import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public final class PortalAction {
    private final Type type;
    private final String value;
    private final String target;
    private final String command;
    private final String title;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    private PortalAction(
            Type type,
            String value,
            String target,
            String command,
            String title,
            String subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        this.type = type;
        this.value = value;
        this.target = target;
        this.command = command;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }

    public static PortalAction fromMap(Map<?, ?> map) {
        Type type = Type.from(String.valueOf(map.get("type")));
        if (type == null) {
            return null;
        }

        return new PortalAction(
                type,
                stringValue(map.get("value")),
                stringValue(map.get("target")),
                stringValue(map.get("command")),
                stringValue(map.get("title")),
                stringValue(map.get("subtitle")),
                intValue(map.get("fadeIn"), 10),
                intValue(map.get("stay"), 40),
                intValue(map.get("fadeOut"), 10)
        );
    }

    public static PortalAction fromSection(ConfigurationSection section) {
        Type type = Type.from(section.getString("type", ""));
        if (type == null) {
            return null;
        }

        return new PortalAction(
                type,
                section.getString("value", ""),
                section.getString("target", ""),
                section.getString("command", ""),
                section.getString("title", ""),
                section.getString("subtitle", ""),
                section.getInt("fadeIn", 10),
                section.getInt("stay", 40),
                section.getInt("fadeOut", 10)
        );
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getTarget() {
        return target;
    }

    public String getCommand() {
        return command;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getFadeIn() {
        return fadeIn;
    }

    public int getStay() {
        return stay;
    }

    public int getFadeOut() {
        return fadeOut;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public enum Type {
        CONSOLE_COMMAND,
        PLAYER_COMMAND,
        VELOCITY_SERVER,
        MESSAGE,
        SOUND,
        TITLE;

        public static Type from(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }

            try {
                return Type.valueOf(raw.trim().replace('-', '_').toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
