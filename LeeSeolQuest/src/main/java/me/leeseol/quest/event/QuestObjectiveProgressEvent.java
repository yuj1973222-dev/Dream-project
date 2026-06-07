package me.leeseol.quest.event;

import me.leeseol.quest.model.ObjectiveType;
import me.leeseol.quest.model.Quest;
import me.leeseol.quest.model.QuestStage;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestObjectiveProgressEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Quest quest;
    private final QuestStage stage;
    private final ObjectiveType objectiveType;
    private final String target;
    private final int progress;
    private final int required;

    public QuestObjectiveProgressEvent(
            Player player,
            Quest quest,
            QuestStage stage,
            ObjectiveType objectiveType,
            String target,
            int progress,
            int required
    ) {
        this.player = player;
        this.quest = quest;
        this.stage = stage;
        this.objectiveType = objectiveType;
        this.target = target;
        this.progress = progress;
        this.required = required;
    }

    public Player player() {
        return player;
    }

    public Quest quest() {
        return quest;
    }

    public QuestStage stage() {
        return stage;
    }

    public ObjectiveType objectiveType() {
        return objectiveType;
    }

    public String target() {
        return target;
    }

    public int progress() {
        return progress;
    }

    public int required() {
        return required;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
