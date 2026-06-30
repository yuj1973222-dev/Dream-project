package me.leeseol.auction.storage;

import me.leeseol.auction.model.AuctionLot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuctionStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<Long, AuctionLot> lots = new LinkedHashMap<>();
    private long nextId = 1L;

    public AuctionStore(JavaPlugin plugin) {
        this.plugin = plugin;
        String dataPath = plugin.getConfig().getString("settings.data-file", "/opt/minecraft/shared/auction/data.yml");
        File configured = new File(dataPath == null || dataPath.isBlank() ? "data.yml" : dataPath);
        this.file = configured.isAbsolute() ? configured : new File(plugin.getDataFolder(), configured.getPath());
    }

    public void load() {
        lots.clear();
        nextId = 1L;
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        nextId = Math.max(1L, data.getLong("next-id", 1L));
        ConfigurationSection section = data.getConfigurationSection("lots");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            long id;
            try {
                id = Long.parseLong(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            String base = "lots." + key + ".";
            UUID sellerUuid = parseUuid(data.getString(base + "seller-uuid"));
            ItemStack item = data.getItemStack(base + "item");
            if (sellerUuid == null || item == null || item.getType().isAir()) {
                plugin.getLogger().warning("Skipping invalid auction lot: " + key);
                continue;
            }
            AuctionLot.Status status = parseStatus(data.getString(base + "status"));
            AuctionLot lot = new AuctionLot(
                    id,
                    sellerUuid,
                    data.getString(base + "seller-name", "unknown"),
                    item,
                    status,
                    data.getLong(base + "created-at", System.currentTimeMillis())
            );
            lots.put(id, lot);
            nextId = Math.max(nextId, id + 1L);
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("next-id", nextId);
        for (AuctionLot lot : lots.values()) {
            String base = "lots." + lot.id() + ".";
            data.set(base + "seller-uuid", lot.sellerUuid().toString());
            data.set(base + "seller-name", lot.sellerName());
            data.set(base + "status", lot.status().name());
            data.set(base + "created-at", lot.createdAt());
            data.set(base + "item", lot.item());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save auction data: " + exception.getMessage());
        }
    }

    public AuctionLot createLot(UUID sellerUuid, String sellerName, ItemStack item) {
        AuctionLot lot = new AuctionLot(nextId++, sellerUuid, sellerName, item, AuctionLot.Status.SUBMITTED, System.currentTimeMillis());
        lots.put(lot.id(), lot);
        return lot;
    }

    public AuctionLot lot(long id) {
        return lots.get(id);
    }

    public List<AuctionLot> submittedLots() {
        return lots.values().stream()
                .filter(lot -> lot.status() == AuctionLot.Status.SUBMITTED)
                .sorted(Comparator.comparingLong(AuctionLot::id))
                .toList();
    }

    public List<AuctionLot> lotsBySeller(UUID uuid) {
        return lots.values().stream()
                .filter(lot -> lot.sellerUuid().equals(uuid))
                .sorted(Comparator.comparingLong(AuctionLot::id))
                .toList();
    }

    public List<AuctionLot> lots() {
        return new ArrayList<>(lots.values());
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private AuctionLot.Status parseStatus(String value) {
        if (value == null) {
            return AuctionLot.Status.SUBMITTED;
        }
        try {
            return AuctionLot.Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return AuctionLot.Status.SUBMITTED;
        }
    }
}
