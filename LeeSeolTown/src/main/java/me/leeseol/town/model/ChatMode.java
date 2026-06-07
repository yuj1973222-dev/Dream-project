package me.leeseol.town.model;

public enum ChatMode {
    GLOBAL("global", "전체"),
    TOWN("town", "파티", "party", "파티"),
    NATION("nation", "국가");

    private final String id;
    private final String displayName;
    private final String[] aliases;

    ChatMode(String id, String displayName, String... aliases) {
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

    public static ChatMode parse(String input) {
        if (input == null) {
            return null;
        }
        String value = input.toLowerCase();
        for (ChatMode mode : values()) {
            if (mode.id.equals(value) || mode.name().equalsIgnoreCase(value) || mode.displayName.equals(value)) {
                return mode;
            }
            for (String alias : mode.aliases) {
                if (alias.equalsIgnoreCase(input)) {
                    return mode;
                }
            }
        }
        return null;
    }
}
