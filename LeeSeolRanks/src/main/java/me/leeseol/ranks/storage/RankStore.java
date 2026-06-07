package me.leeseol.ranks.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.leeseol.ranks.model.Rank;
import me.leeseol.ranks.model.RankData;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class RankStore {
    private final File file;
    private final Map<UUID, RankData> players = new HashMap<>();

    public RankStore(JavaPlugin plugin) {
        String configuredPath = plugin.getConfig().getString("storage.data-file", "");
        if (configuredPath == null || configuredPath.isBlank()) {
            this.file = new File(plugin.getDataFolder(), "ranks.yml");
        } else {
            this.file = new File(configuredPath);
        }
    }

    public void load() {
        players.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key + ".";
                Rank rank = Rank.parse(yaml.getString(path + "rank", "PLAYER"));
                if (rank == null) {
                    rank = Rank.PLAYER;
                }
                if (yaml.getBoolean(path + "dev", false)) {
                    rank = Rank.DEV;
                }
                players.put(uuid, new RankData(
                        uuid,
                        yaml.getString(path + "name", "unknown"),
                        rank,
                        yaml.getInt(path + "kills", 0),
                        false
                ));
            } catch (IllegalArgumentException ignored) {
                // Bad old data should not block plugin startup.
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (RankData data : players.values()) {
            String path = "players." + data.uuid() + ".";
            yaml.set(path + "name", data.name());
            yaml.set(path + "rank", data.rank().name().toLowerCase(Locale.ROOT));
            yaml.set(path + "kills", data.kills());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parent);
            }
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save ranks.yml", exception);
        }
    }

    public RankData getOrCreate(OfflinePlayer player) {
        RankData data = players.get(player.getUniqueId());
        if (data != null) {
            data.setName(player.getName());
            return data;
        }
        RankData created = new RankData(player.getUniqueId(), player.getName(), Rank.PLAYER, 0, false);
        players.put(player.getUniqueId(), created);
        return created;
    }

    public RankData findByName(String name) {
        if (name == null) {
            return null;
        }
        for (RankData data : players.values()) {
            if (name.equalsIgnoreCase(data.name())) {
                return data;
            }
        }
        return null;
    }

    public Collection<RankData> all() {
        return players.values();
    }
}
