package me.leeseol.quest.model;

public record QuestStage(
    int number,
    String message,
    QuestObjective objective
) {
}
