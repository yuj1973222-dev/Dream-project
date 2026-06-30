package me.leeseol.town.service;

public record TerritorySnapshot(String key, String label, String colorPrefix) {
    private static final TerritorySnapshot WILDERNESS = new TerritorySnapshot("wilderness", "야생", "&a");

    public static TerritorySnapshot wilderness() {
        return WILDERNESS;
    }

    public static TerritorySnapshot nation(String id, String name) {
        return nation(id, name, "&#8EC5FF");
    }

    public static TerritorySnapshot nation(String id, String name, String colorPrefix) {
        return new TerritorySnapshot("nation:" + id, name == null || name.isBlank() ? id : name, colorPrefix);
    }

    public boolean nation() {
        return key != null && key.startsWith("nation:");
    }

    public String coloredLabel() {
        return (colorPrefix == null ? "" : colorPrefix) + label;
    }
}
