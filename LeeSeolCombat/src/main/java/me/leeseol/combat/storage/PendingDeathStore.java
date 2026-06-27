package me.leeseol.combat.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PendingDeathStore {
    private final File file;
    private final Set<UUID> pendingDeaths = new HashSet<>();

    public PendingDeathStore(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "pending-deaths.yml");
    }

    public void load() {
        pendingDeaths.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String value : yaml.getStringList("players")) {
            try {
                pendingDeaths.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // Bad old data should not stop the server.
            }
        }
    }

    public void mark(UUID uuid) {
        pendingDeaths.add(uuid);
        save();
    }

    public boolean consume(UUID uuid) {
        boolean removed = pendingDeaths.remove(uuid);
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean contains(UUID uuid) {
        return pendingDeaths.contains(uuid);
    }

    public int count() {
        return pendingDeaths.size();
    }

    public File file() {
        return file;
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("players", pendingDeaths.stream().map(UUID::toString).sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save pending deaths", exception);
        }
    }
}
