package me.leeseol.quest.event;

import me.leeseol.quest.model.Quest;
import me.leeseol.quest.model.QuestStage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestStageAdvancedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Quest quest;
    private final QuestStage previousStage;
    private final QuestStage nextStage;

    public QuestStageAdvancedEvent(Player player, Quest quest, QuestStage previousStage, QuestStage nextStage) {
        this.player = player;
        this.quest = quest;
        this.previousStage = previousStage;
        this.nextStage = nextStage;
    }

    public Player player() {
        return player;
    }

    public Quest quest() {
        return quest;
    }

    public QuestStage previousStage() {
        return previousStage;
    }

    public QuestStage nextStage() {
        return nextStage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
