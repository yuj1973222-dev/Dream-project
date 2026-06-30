package me.leeseol.enchanting.model;

import java.util.List;

public record EnchantBand(
    String id,
    int minBookshelves,
    double chancePercent,
    EnchantOutput output,
    List<EnchantCandidate> candidates
) {
}
