package me.leeseol.economy.market;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

public final class MarketOffer {
    private final String id;
    private final Material material;
    private final String category;
    private final String displayName;
    private final long basePrice;
    private final long minPrice;
    private final long dailyTargetAmount;
    private final long personalSoftAmount;

    private MarketOffer(
        String id,
        Material material,
        String category,
        String displayName,
        long basePrice,
        long minPrice,
        long dailyTargetAmount,
        long personalSoftAmount
    ) {
        this.id = id;
        this.material = material;
        this.category = category;
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.dailyTargetAmount = dailyTargetAmount;
        this.personalSoftAmount = personalSoftAmount;
    }

    public static MarketOffer fromConfig(String id, ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null || material.isAir()) {
            return null;
        }
        long basePrice = Math.max(0L, section.getLong("base-price", 0L));
        if (basePrice <= 0L) {
            return null;
        }
        long minPrice = Math.max(1L, Math.min(basePrice, section.getLong("min-price", Math.max(1L, basePrice / 4L))));
        return new MarketOffer(
            id.toLowerCase(Locale.ROOT),
            material,
            section.getString("category", "general").toLowerCase(Locale.ROOT),
            section.getString("name", material.name()),
            basePrice,
            minPrice,
            Math.max(1L, section.getLong("daily-target-amount", 1000L)),
            Math.max(1L, section.getLong("personal-soft-amount", 100L))
        );
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public String category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public long basePrice() {
        return basePrice;
    }

    public long minPrice() {
        return minPrice;
    }

    public long dailyTargetAmount() {
        return dailyTargetAmount;
    }

    public long personalSoftAmount() {
        return personalSoftAmount;
    }
}
