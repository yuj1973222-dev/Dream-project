package me.leeseol.core.content;

import java.util.Locale;

public final class ContentBlueMapMarkerPolicy {
    private ContentBlueMapMarkerPolicy() {
    }

    public static boolean visibleOnBlueMap(ContentType type) {
        return type == ContentType.CASINO || type == ContentType.DUNGEON;
    }

    public static String markerId(ContentType type, String id) {
        return "content_" + type.key() + "_" + safeId(id);
    }

    private static String safeId(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
