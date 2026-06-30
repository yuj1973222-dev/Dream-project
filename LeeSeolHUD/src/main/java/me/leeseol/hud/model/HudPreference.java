package me.leeseol.hud.model;

public final class HudPreference {
    private boolean compass;
    private boolean targetHealth;

    public HudPreference(boolean compass, boolean targetHealth) {
        this.compass = compass;
        this.targetHealth = targetHealth;
    }

    public boolean compass() {
        return compass;
    }

    public void setCompass(boolean compass) {
        this.compass = compass;
    }

    public boolean targetHealth() {
        return targetHealth;
    }

    public void setTargetHealth(boolean targetHealth) {
        this.targetHealth = targetHealth;
    }
}
