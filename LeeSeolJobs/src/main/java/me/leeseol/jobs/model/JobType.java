package me.leeseol.jobs.model;

public enum JobType {
    MINING("mining", "광질"),
    FARMING("farming", "농사"),
    FISHING("fishing", "낚시"),
    EXPLORATION("exploration", "탐험");

    private final String configKey;
    private final String displayName;

    JobType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }
}
