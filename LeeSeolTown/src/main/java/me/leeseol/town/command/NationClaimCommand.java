package me.leeseol.town.command;

import java.util.Locale;

enum NationClaimCommand {
    CLAIM,
    PRICE,
    UNCLAIM;

    static NationClaimCommand parse(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "claim", "buy", "purchase" -> CLAIM;
            case "claimprice", "claimcost", "price", "cost" -> PRICE;
            case "unclaim" -> UNCLAIM;
            default -> null;
        };
    }
}
