package me.leeseol.crafting.model;

import java.util.Locale;

public enum RecipeType {
    CRAFTING("crafting"),
    PROCESSING("processing"),
    DISASSEMBLE("disassemble");

    private final String configName;

    RecipeType(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return configName;
    }

    public static RecipeType parse(String value) {
        if (value == null) {
            return CRAFTING;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (RecipeType type : values()) {
            if (type.configName.equals(normalized)) {
                return type;
            }
        }
        return CRAFTING;
    }
}
