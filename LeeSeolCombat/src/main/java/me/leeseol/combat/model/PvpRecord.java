package me.leeseol.combat.model;

import java.util.UUID;

public final class PvpRecord {
    private final UUID uuid;
    private String name;
    private int points;
    private int kills;

    public PvpRecord(UUID uuid, String name, int points, int kills) {
        this.uuid = uuid;
        this.name = name;
        this.points = Math.max(0, points);
        this.kills = Math.max(0, kills);
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

    public int points() {
        return points;
    }

    public void setPoints(int points) {
        this.points = Math.max(0, points);
    }

    public void addPoints(int amount) {
        setPoints(points + amount);
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
    }
}
