package me.leeseol.jobs.service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.leeseol.jobs.model.JobType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class JobsConfig {
    private boolean enabled;
    private final Set<String> allowedWorlds = new HashSet<>();
    private final Set<String> blockedWorlds = new HashSet<>();
    private final Map<JobType, Boolean> jobEnabled = new EnumMap<>(JobType.class);
    private final Map<JobType, Long> dailyLimits = new EnumMap<>(JobType.class);
    private final Map<JobType, Map<String, Long>> rewards = new EnumMap<>(JobType.class);
    private final Map<String, Double> rankMultipliers = new HashMap<>();
    private long miningCooldownMillis;
    private long fishingCooldownMillis;
    private long explorationCooldownMillis;
    private long explorationNewBiomeReward;
    private boolean antiPlaceAbuse;
    private long placedBlockRememberMillis;
    private boolean farmingRequireFullyGrown;
    private boolean logRewards;
    private long saveIntervalSeconds;

    public void load(FileConfiguration config) {
        enabled = config.getBoolean("settings.enabled", true);
        allowedWorlds.clear();
        blockedWorlds.clear();
        readWorlds(config.getStringList("settings.allowed-worlds"), allowedWorlds);
        readWorlds(config.getStringList("settings.blocked-worlds"), blockedWorlds);
        saveIntervalSeconds = Math.max(10L, config.getLong("settings.save-interval-seconds", 60L));
        logRewards = config.getBoolean("economy.log-rewards", false);

        for (JobType type : JobType.values()) {
            jobEnabled.put(type, config.getBoolean(type.configKey() + ".enabled", true));
            dailyLimits.put(type, Math.max(0L, config.getLong("daily-limits." + type.configKey(), 0L)));
            rewards.put(type, loadRewards(config.getConfigurationSection(type.configKey() + ".rewards")));
        }

        miningCooldownMillis = Math.max(0L, config.getLong("mining.cooldown-millis", 250L));
        fishingCooldownMillis = Math.max(0L, config.getLong("fishing.cooldown-millis", 1000L));
        explorationCooldownMillis = Math.max(0L, config.getLong("exploration.cooldown-millis", 5000L));
        explorationNewBiomeReward = Math.max(0L, config.getLong("exploration.rewards.new-biome-daily", 600L));
        antiPlaceAbuse = config.getBoolean("mining.anti-place-abuse.enabled", true);
        long rememberMinutes = Math.max(0L, config.getLong("mining.anti-place-abuse.remember-minutes", 30L));
        placedBlockRememberMillis = rememberMinutes * 60_000L;
        farmingRequireFullyGrown = config.getBoolean("farming.require-fully-grown", true);

        rankMultipliers.clear();
        ConfigurationSection multipliers = config.getConfigurationSection("rank-multipliers");
        if (multipliers != null) {
            for (String key : multipliers.getKeys(false)) {
                rankMultipliers.put(key.toUpperCase(Locale.ROOT), Math.max(0.0D, multipliers.getDouble(key, 1.0D)));
            }
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) {
            return false;
        }
        String name = world.getName().toLowerCase(Locale.ROOT);
        if (blockedWorlds.contains(name)) {
            return false;
        }
        return allowedWorlds.isEmpty() || allowedWorlds.contains(name);
    }

    public boolean isJobEnabled(JobType type) {
        return jobEnabled.getOrDefault(type, false);
    }

    public long reward(JobType type, Material material) {
        if (material == null) {
            return 0L;
        }
        return rewards.getOrDefault(type, Map.of()).getOrDefault(material.name(), 0L);
    }

    public long fishingReward(String key) {
        return rewards.getOrDefault(JobType.FISHING, Map.of()).getOrDefault(key, 0L);
    }

    public long dailyLimit(JobType type) {
        return dailyLimits.getOrDefault(type, 0L);
    }

    public long miningCooldownMillis() {
        return miningCooldownMillis;
    }

    public long fishingCooldownMillis() {
        return fishingCooldownMillis;
    }

    public long explorationCooldownMillis() {
        return explorationCooldownMillis;
    }

    public long explorationNewBiomeReward() {
        return explorationNewBiomeReward;
    }

    public boolean antiPlaceAbuse() {
        return antiPlaceAbuse;
    }

    public long placedBlockRememberMillis() {
        return placedBlockRememberMillis;
    }

    public boolean farmingRequireFullyGrown() {
        return farmingRequireFullyGrown;
    }

    public boolean logRewards() {
        return logRewards;
    }

    public long saveIntervalSeconds() {
        return saveIntervalSeconds;
    }

    public double rankMultiplier(String rankName) {
        if (rankName == null || rankName.isBlank()) {
            return rankMultipliers.getOrDefault("PLAYER", 1.0D);
        }
        return rankMultipliers.getOrDefault(rankName.toUpperCase(Locale.ROOT), 1.0D);
    }

    private static void readWorlds(Iterable<String> values, Set<String> target) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.toLowerCase(Locale.ROOT));
            }
        }
    }

    private static Map<String, Long> loadRewards(ConfigurationSection section) {
        Map<String, Long> loaded = new HashMap<>();
        if (section == null) {
            return loaded;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("default") || key.equalsIgnoreCase("treasure")) {
                loaded.put(key.toLowerCase(Locale.ROOT), Math.max(0L, section.getLong(key, 0L)));
                continue;
            }
            Material material = Material.matchMaterial(key);
            if (material != null && !material.isAir()) {
                loaded.put(material.name(), Math.max(0L, section.getLong(key, 0L)));
            }
        }
        return loaded;
    }
}
