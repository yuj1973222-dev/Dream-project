package me.leeseol.core.content;

import java.util.Arrays;
import java.util.Optional;

public enum ContentType {
    NEUTRAL("neutral"),
    CASINO("casino"),
    DUNGEON("dungeon");

    private final String key;

    ContentType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<ContentType> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.key.equalsIgnoreCase(key))
                .findFirst();
    }
}
