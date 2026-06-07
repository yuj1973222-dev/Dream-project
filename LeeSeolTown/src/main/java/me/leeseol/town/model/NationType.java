package me.leeseol.town.model;

public enum NationType {
    REPUBLIC("republic", "민국", "공화국"),
    EMPIRE("empire", "제국"),
    FEDERATION("federation", "연방");

    private final String id;
    private final String displayName;
    private final String[] aliases;

    NationType(String id, String displayName, String... aliases) {
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

    public static NationType parse(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim().toLowerCase();
        for (NationType type : values()) {
            if (type.id.equals(value) || type.name().equalsIgnoreCase(value) || type.displayName.equals(value)) {
                return type;
            }
            for (String alias : type.aliases) {
                if (alias.equalsIgnoreCase(input.trim())) {
                    return type;
                }
            }
        }
        return null;
    }
}
