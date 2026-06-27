package me.leeseol.town.service;

import java.util.Objects;

public record TerritoryTransition(TerritorySnapshot previous, TerritorySnapshot current) {
    public static TerritoryTransition between(TerritorySnapshot previous, TerritorySnapshot current) {
        return new TerritoryTransition(previous, current);
    }

    public boolean changed() {
        return previous == null || current == null || !Objects.equals(previous.key(), current.key());
    }

    public boolean enteredNation() {
        return changed() && current != null && current.nation();
    }

    public boolean enteredWilderness() {
        return changed() && current != null && !current.nation();
    }
}
