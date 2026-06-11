package me.leeseol.quest.model;

import java.util.Locale;

public enum QuestResetPeriod {
    ONCE("once", "1회"),
    DAILY("daily", "일일"),
    WEEKLY("weekly", "주간");

    private final String configName;
    private final String displayName;

    QuestResetPeriod(String configName, String displayName) {
        this.configName = configName;
        this.displayName = displayName;
    }

    public String configName() {
        return configName;
    }

    public String displayName() {
        return displayName;
    }

    public static QuestResetPeriod fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return ONCE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (QuestResetPeriod period : values()) {
            if (period.configName.equals(normalized)) {
                return period;
            }
        }
        return ONCE;
    }
}
