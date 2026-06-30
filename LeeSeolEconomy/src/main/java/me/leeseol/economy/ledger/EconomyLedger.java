package me.leeseol.economy.ledger;

import me.leeseol.economy.LeeSeolEconomyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class EconomyLedger {
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> GROUPS = List.of("issued", "removed", "transferred");

    private final LeeSeolEconomyPlugin plugin;
    private Path dataPath;

    public EconomyLedger(LeeSeolEconomyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String configured = plugin.getConfig().getString("storage.ledger-file");
        dataPath = configured == null || configured.isBlank()
            ? plugin.getDataFolder().toPath().resolve("ledger.yml")
            : Path.of(configured);
        ensureFile();
    }

    public void recordIssued(String source, long amount) {
        record("issued", source, amount);
    }

    public void recordRemoved(String source, long amount) {
        record("removed", source, amount);
    }

    public void recordTransferred(String source, long amount) {
        record("transferred", source, amount);
    }

    public LedgerSnapshot today() {
        return snapshot(currentPeriod());
    }

    public LedgerSnapshot snapshot(String period) {
        String resolved = period == null || period.isBlank() ? currentPeriod() : period;
        return access(data -> {
            Map<String, Long> issued = readGroup(data, resolved, "issued");
            Map<String, Long> removed = readGroup(data, resolved, "removed");
            Map<String, Long> transferred = readGroup(data, resolved, "transferred");
            return new LedgerSnapshot(
                resolved,
                issued,
                removed,
                transferred,
                sum(issued),
                sum(removed),
                sum(transferred)
            );
        }, false);
    }

    private void record(String group, String source, long amount) {
        if (!plugin.getConfig().getBoolean("features.ledger", true) || amount <= 0L || !GROUPS.contains(group)) {
            return;
        }
        String normalized = normalizeSource(source);
        access(data -> {
            String base = "periods." + currentPeriod() + "." + group + "." + normalized;
            data.set(base + ".amount", safeAdd(data.getLong(base + ".amount", 0L), amount));
            data.set(base + ".count", safeAdd(data.getLong(base + ".count", 0L), 1L));
            data.set("updated-at", System.currentTimeMillis());
            return null;
        }, true);
    }

    private Map<String, Long> readGroup(YamlConfiguration data, String period, String group) {
        Map<String, Long> values = new LinkedHashMap<>();
        ConfigurationSection section = data.getConfigurationSection("periods." + period + "." + group);
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            long amount = Math.max(0L, section.getLong(key + ".amount", 0L));
            if (amount > 0L) {
                values.put(key, amount);
            }
        }
        return values;
    }

    private <T> T access(Function<YamlConfiguration, T> operation, boolean save) {
        ensureFile();
        try (FileChannel channel = FileChannel.open(dataPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            File file = dataPath.toFile();
            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
            T result = operation.apply(data);
            if (save) {
                data.save(file);
            }
            return result;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to access economy ledger file: " + exception.getMessage());
            return operation.apply(new YamlConfiguration());
        }
    }

    private String currentPeriod() {
        String zoneRaw = plugin.getConfig().getString("ledger.time-zone", "Asia/Seoul");
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneRaw);
        } catch (Exception exception) {
            zone = ZoneId.of("Asia/Seoul");
        }
        return LocalDate.now(zone).format(PERIOD_FORMAT);
    }

    private void ensureFile() {
        try {
            Path parent = dataPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(dataPath)) {
                Files.createFile(dataPath);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to prepare economy ledger file " + dataPath + ": " + exception.getMessage());
        }
    }

    private String normalizeSource(String source) {
        String value = source == null || source.isBlank() ? "unknown" : source.trim().toLowerCase();
        return value.replaceAll("[^a-z0-9_\\-]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private long sum(Map<String, Long> values) {
        long total = 0L;
        for (long value : values.values()) {
            total = safeAdd(total, value);
        }
        return total;
    }

    private long safeAdd(long left, long right) {
        long result = left + right;
        return result < left ? Long.MAX_VALUE : result;
    }
}
