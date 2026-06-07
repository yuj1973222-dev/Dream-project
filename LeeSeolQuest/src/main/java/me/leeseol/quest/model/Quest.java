package me.leeseol.quest.model;

import java.util.List;

public record Quest(
    String id,
    String displayName,
    boolean autoStart,
    List<QuestStage> stages,
    long rewardMoney,
    List<String> rewardCommands
) {
    public QuestStage firstStage() {
        return stages.isEmpty() ? null : stages.get(0);
    }

    public QuestStage stage(int number) {
        for (QuestStage stage : stages) {
            if (stage.number() == number) {
                return stage;
            }
        }
        return null;
    }

    public QuestStage nextStage(int currentNumber) {
        for (QuestStage stage : stages) {
            if (stage.number() > currentNumber) {
                return stage;
            }
        }
        return null;
    }
}
