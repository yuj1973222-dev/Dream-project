package me.leeseol.quest.model;

import java.util.HashSet;
import java.util.Set;

public final class PlayerQuestData {
    private String activeQuestId;
    private int stageNumber;
    private int progress;
    private final Set<String> completedQuests = new HashSet<>();

    public String activeQuestId() {
        return activeQuestId;
    }

    public void activeQuestId(String activeQuestId) {
        this.activeQuestId = activeQuestId;
    }

    public int stageNumber() {
        return stageNumber;
    }

    public void stageNumber(int stageNumber) {
        this.stageNumber = stageNumber;
    }

    public int progress() {
        return progress;
    }

    public void progress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public Set<String> completedQuests() {
        return completedQuests;
    }

    public boolean hasActiveQuest() {
        return activeQuestId != null && !activeQuestId.isBlank();
    }

    public void clearActiveQuest() {
        activeQuestId = null;
        stageNumber = 0;
        progress = 0;
    }
}
