package me.leeseol.town.structure;

public enum StructureCategory {
    NATION_CORE,
    NORMAL;

    public static StructureCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        return switch (raw.trim().toLowerCase()) {
            case "nation-core", "nation_core", "core" -> NATION_CORE;
            default -> NORMAL;
        };
    }
}
