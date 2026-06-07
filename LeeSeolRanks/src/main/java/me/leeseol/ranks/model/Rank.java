package me.leeseol.ranks.model;

import java.util.Locale;

public enum Rank {
    PLAYER("PLAYER", 0, "player"),
    D("D", 1, "rank_d"),
    C("C", 2, "rank_c"),
    B("B", 3, "rank_b"),
    A("A", 4, "rank_a"),
    S("S", 5, "rank_s"),
    ADMIN("ADMIN", 100, "admin"),
    DEV("DEV", 101, "dev");

    private final String display;
    private final int order;
    private final String imageId;

    Rank(String display, int order, String imageId) {
        this.display = display;
        this.order = order;
        this.imageId = imageId;
    }

    public String display() {
        return display;
    }

    public Rank next() {
        return switch (this) {
            case PLAYER -> D;
            case D -> C;
            case C -> B;
            case B -> A;
            case A -> S;
            case S, ADMIN, DEV -> null;
        };
    }

    public String permission() {
        return "leeseolranks.rank." + name().toLowerCase(Locale.ROOT);
    }

    public String imagePlaceholder() {
        return "%img_" + imageId + "%";
    }

    public boolean staff() {
        return this == ADMIN || this == DEV;
    }

    public boolean progressionRank() {
        return this != ADMIN && this != DEV;
    }

    public static Rank parse(String value) {
        if (value == null) {
            return null;
        }
        if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("default")) {
            return PLAYER;
        }
        try {
            return Rank.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
