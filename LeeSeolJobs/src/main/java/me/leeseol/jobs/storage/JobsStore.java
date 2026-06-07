package me.leeseol.jobs.storage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.leeseol.jobs.LeeSeolJobsPlugin;
import me.leeseol.jobs.model.JobType;
import me.leeseol.jobs.model.PlayerJobStats;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

public final class JobsStore {
    private final LeeSeolJobsPlugin plugin;
    private final Map<UUID, PlayerJobStats> players = new HashMap<>();
    private File file;

    public JobsStore(LeeSeolJobsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        String configuredPath = plugin.getConfig().getString("storage.data-file", "");
        file = configuredPath == null || configuredPath.isBlank()
            ? new File(plugin.getDataFolder(), "jobs.yml")
            : new File(configuredPath);
        load();
    }

    public void load() {
        players.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key + ".";
                PlayerJobStats stats = new PlayerJobStats(
                    uuid,
                    yaml.getString(path + "name", "unknown"),
                    yaml.getString(path + "daily.date", today())
                );
                for (JobType type : JobType.values()) {
                    stats.total(type, yaml.getLong(path + "totals." + type.configKey(), 0L));
                    stats.daily(type, yaml.getLong(path + "daily." + type.configKey(), 0L));
                }
                if (!today().equals(stats.dailyDate())) {
                    stats.resetDaily(today());
                }
                players.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {
                // Bad old data should not block plugin startup.
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerJobStats stats : players.values()) {
            String path = "players." + stats.uuid() + ".";
            yaml.set(path + "name", stats.name());
            for (JobType type : JobType.values()) {
                yaml.set(path + "totals." + type.configKey(), stats.total(type));
                yaml.set(path + "daily." + type.configKey(), stats.daily(type));
            }
            yaml.set(path + "daily.date", stats.dailyDate());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parent);
            }
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save jobs data", exception);
        }
    }

    public PlayerJobStats getOrCreate(OfflinePlayer player) {
        PlayerJobStats stats = players.get(player.getUniqueId());
        if (stats == null) {
            stats = new PlayerJobStats(player.getUniqueId(), player.getName(), today());
            players.put(player.getUniqueId(), stats);
        }
        stats.name(player.getName());
        if (!today().equals(stats.dailyDate())) {
            stats.resetDaily(today());
        }
        return stats;
    }

    public PlayerJobStats findByName(String name) {
        if (name == null) {
            return null;
        }
        for (PlayerJobStats stats : players.values()) {
            if (name.equalsIgnoreCase(stats.name())) {
                return stats;
            }
        }
        return null;
    }

    public void reset(OfflinePlayer player) {
        players.remove(player.getUniqueId());
    }

    public Collection<PlayerJobStats> all() {
        return players.values();
    }

    private static String today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
    }
}
