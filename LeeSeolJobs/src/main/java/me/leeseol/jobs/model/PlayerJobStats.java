package me.leeseol.jobs.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerJobStats {
    private final UUID uuid;
    private String name;
    private String dailyDate;
    private final Map<JobType, Long> totals = new EnumMap<>(JobType.class);
    private final Map<JobType, Long> daily = new EnumMap<>(JobType.class);
    private final Set<String> dailyExplorationBiomes = new HashSet<>();

    public PlayerJobStats(UUID uuid, String name, String dailyDate) {
        this.uuid = uuid;
        this.name = name;
        this.dailyDate = dailyDate;
        for (JobType type : JobType.values()) {
            totals.put(type, 0L);
            daily.put(type, 0L);
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public String dailyDate() {
        return dailyDate;
    }

    public void resetDaily(String date) {
        this.dailyDate = date;
        for (JobType type : JobType.values()) {
            daily.put(type, 0L);
        }
        dailyExplorationBiomes.clear();
    }

    public long total(JobType type) {
        return totals.getOrDefault(type, 0L);
    }

    public long daily(JobType type) {
        return daily.getOrDefault(type, 0L);
    }

    public void total(JobType type, long amount) {
        totals.put(type, Math.max(0L, amount));
    }

    public void daily(JobType type, long amount) {
        daily.put(type, Math.max(0L, amount));
    }

    public void add(JobType type, long amount) {
        totals.put(type, total(type) + amount);
        daily.put(type, daily(type) + amount);
    }

    public boolean markDailyExplorationBiome(String biomeKey) {
        if (biomeKey == null || biomeKey.isBlank()) {
            return false;
        }
        return dailyExplorationBiomes.add(biomeKey.toLowerCase(Locale.ROOT));
    }

    public boolean hasDailyExplorationBiome(String biomeKey) {
        return biomeKey != null && dailyExplorationBiomes.contains(biomeKey.toLowerCase(Locale.ROOT));
    }

    public void dailyExplorationBiomes(Collection<String> biomeKeys) {
        dailyExplorationBiomes.clear();
        if (biomeKeys == null) {
            return;
        }
        for (String biomeKey : biomeKeys) {
            markDailyExplorationBiome(biomeKey);
        }
    }

    public Set<String> dailyExplorationBiomes() {
        return Set.copyOf(dailyExplorationBiomes);
    }
}
