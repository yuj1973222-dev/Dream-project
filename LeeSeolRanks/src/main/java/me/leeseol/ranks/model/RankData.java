package me.leeseol.ranks.model;

import java.util.UUID;

public final class RankData {
    private final UUID uuid;
    private String name;
    private Rank rank;
    private int kills;
    private boolean dev;

    public RankData(UUID uuid, String name, Rank rank, int kills, boolean dev) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank == null ? Rank.PLAYER : rank;
        this.kills = kills;
        this.dev = dev;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public Rank rank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank == null ? Rank.PLAYER : rank;
    }

    public int kills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        kills++;
    }

    public boolean dev() {
        return dev;
    }

    public void setDev(boolean dev) {
        this.dev = dev;
    }
}
