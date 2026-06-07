package me.leeseol.ranks.service;

import java.util.List;
import me.leeseol.ranks.model.Rank;

public record RequirementResult(Rank targetRank, List<RequirementLine> lines, boolean met) {
}
