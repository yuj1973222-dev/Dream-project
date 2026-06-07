package me.leeseol.quest.event;

import me.leeseol.quest.model.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class QuestStartedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Quest quest;

    public QuestStartedEvent(Player player, Quest quest) {
        this.player = player;
        this.quest = quest;
    }

    public Player player() {
        return player;
    }

    public Quest quest() {
        return quest;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
