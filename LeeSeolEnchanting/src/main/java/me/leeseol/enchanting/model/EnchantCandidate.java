package me.leeseol.enchanting.model;

public record EnchantCandidate(
    String enchant,
    int minLevel,
    int maxLevel,
    int weight
) {
}
