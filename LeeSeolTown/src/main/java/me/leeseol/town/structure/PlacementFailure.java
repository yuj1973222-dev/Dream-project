package me.leeseol.town.structure;

public enum PlacementFailure {
    NOT_IN_NATION,
    PLACEMENT_CHUNK_NOT_OWNED,
    NATION_IN_WAR,
    NATION_CORE_OUTSIDE_CHUNK,
    OUTSIDE_NATION_TERRITORY,
    OBSTRUCTED,
    MISSING_SCHEMATIC,
    NO_PERMISSION
}
