package me.leeseol.quest.model;

import org.bukkit.Material;

public record QuestObjective(
    ObjectiveType type,
    String target,
    Material material,
    int amount
) {
    public int requiredAmount() {
        return Math.max(1, amount);
    }

    public boolean matches(ObjectiveType eventType, String eventTarget, Material eventMaterial) {
        if (type != eventType) {
            return false;
        }
        if (target != null && !target.isBlank()) {
            if (eventTarget == null || !target.equalsIgnoreCase(eventTarget)) {
                return false;
            }
        }
        return material == null || material == eventMaterial;
    }
}
