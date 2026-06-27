package me.leeseol.lobby.limbo;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class LimboCommandPolicy {
    private final Set<String> allowedCommands;

    public LimboCommandPolicy(Set<String> allowedCommands) {
        this.allowedCommands = allowedCommands.stream()
                .map(LimboCommandPolicy::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAllowed(String rawCommand) {
        if (rawCommand == null || !rawCommand.startsWith("/")) {
            return false;
        }

        String withoutSlash = rawCommand.substring(1).trim();
        if (withoutSlash.isEmpty()) {
            return false;
        }

        String label = withoutSlash.split("\\s+", 2)[0];
        int namespaceSeparator = label.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < label.length()) {
            label = label.substring(namespaceSeparator + 1);
        }
        return allowedCommands.contains(normalize(label));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
