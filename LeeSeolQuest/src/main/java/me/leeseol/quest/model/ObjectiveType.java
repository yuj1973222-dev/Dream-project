package me.leeseol.quest.model;

import java.util.Locale;

public enum ObjectiveType {
    NPC_DIALOGUE("npc-dialogue"),
    OPEN_GUI("open-gui"),
    CRAFT_ITEM("craft-item"),
    MINE_BLOCK("mine-block"),
    HARVEST_CROP("harvest-crop"),
    FISH("fish"),
    DUNGEON_ENTER("dungeon-enter"),
    KILL_PLAYER("kill-player"),
    EARN_MONEY("earn-money"),
    RANK_UP("rank-up");

    private final String configName;

    ObjectiveType(String configName) {
        this.configName = configName;
    }

    public String configName() {
        return configName;
    }

    public static ObjectiveType fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ObjectiveType type : values()) {
            if (type.configName.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
