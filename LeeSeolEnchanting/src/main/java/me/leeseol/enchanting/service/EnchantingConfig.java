package me.leeseol.enchanting.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.leeseol.enchanting.LeeSeolEnchantingPlugin;
import me.leeseol.enchanting.model.EnchantBand;
import me.leeseol.enchanting.model.EnchantCandidate;
import me.leeseol.enchanting.model.EnchantOutput;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class EnchantingConfig {
    private final LeeSeolEnchantingPlugin plugin;
    private boolean enabled;
    private boolean debug;
    private boolean requireClearInnerSpace;
    private Set<String> allowedWorlds = Set.of();
    private Set<Material> bookshelfMaterials = Set.of(Material.BOOKSHELF);
    private List<EnchantBand> bands = List.of();
    private boolean loreDescriptionsEnabled;
    private boolean refreshLoreOnInventoryClick;
    private String loreDescriptionFormat = "&8- &7%description%";
    private Map<String, String> loreDescriptions = Map.of();

    public EnchantingConfig(LeeSeolEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("settings.enabled", true);
        debug = plugin.getConfig().getBoolean("settings.debug", false);
        requireClearInnerSpace = plugin.getConfig().getBoolean("bookshelves.require-clear-inner-space", true);
        allowedWorlds = lowerSet(plugin.getConfig().getStringList("settings.allowed-worlds"));
        bookshelfMaterials = loadMaterials(plugin.getConfig().getStringList("bookshelves.materials"));
        bands = loadBands();
        loreDescriptionsEnabled = plugin.getConfig().getBoolean("lore-descriptions.enabled", true);
        refreshLoreOnInventoryClick = plugin.getConfig().getBoolean("lore-descriptions.refresh-on-inventory-click", true);
        loreDescriptionFormat = plugin.getConfig().getString("lore-descriptions.line-format", "&8- &7%description%");
        loreDescriptions = loadLoreDescriptions();
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean debug() {
        return debug;
    }

    public boolean requireClearInnerSpace() {
        return requireClearInnerSpace;
    }

    public boolean allowed(World world) {
        return world != null && (allowedWorlds.isEmpty() || allowedWorlds.contains(world.getName().toLowerCase(Locale.ROOT)));
    }

    public boolean bookshelf(Material material) {
        return bookshelfMaterials.contains(material);
    }

    public List<EnchantBand> bands() {
        return bands;
    }

    public boolean loreDescriptionsEnabled() {
        return loreDescriptionsEnabled;
    }

    public boolean refreshLoreOnInventoryClick() {
        return refreshLoreOnInventoryClick;
    }

    public String loreDescriptionFormat() {
        return loreDescriptionFormat;
    }

    public String loreDescription(String enchant) {
        return loreDescriptions.getOrDefault(enchant.toLowerCase(Locale.ROOT), "");
    }

    public String allowedWorldsText() {
        return String.join(", ", allowedWorlds);
    }

    public EnchantBand bandFor(int bookshelves) {
        EnchantBand selected = null;
        for (EnchantBand band : bands) {
            if (bookshelves >= band.minBookshelves()) {
                selected = band;
            }
        }
        return selected;
    }

    private List<EnchantBand> loadBands() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rolls.bands");
        if (section == null) {
            return List.of();
        }
        List<EnchantBand> loaded = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection bandSection = section.getConfigurationSection(id);
            if (bandSection == null) {
                continue;
            }
            List<EnchantCandidate> candidates = new ArrayList<>();
            for (var raw : bandSection.getMapList("candidates")) {
                Object rawEnchant = raw.get("enchant");
                String enchant = String.valueOf(rawEnchant == null ? "" : rawEnchant).toLowerCase(Locale.ROOT);
                int minLevel = positiveInt(raw.get("min-level"), 1);
                int maxLevel = Math.max(minLevel, positiveInt(raw.get("max-level"), minLevel));
                int weight = positiveInt(raw.get("weight"), 1);
                if (!enchant.isBlank()) {
                    candidates.add(new EnchantCandidate(enchant, minLevel, maxLevel, weight));
                }
            }
            if (candidates.isEmpty()) {
                plugin.getLogger().warning("Skipping enchanting band without candidates: " + id);
                continue;
            }
            loaded.add(new EnchantBand(
                id,
                Math.max(0, bandSection.getInt("min-bookshelves", 0)),
                Math.max(0.0D, bandSection.getDouble("chance-percent", 0.0D)),
                EnchantOutput.parse(bandSection.getString("output", "APPLY_TO_ITEM")),
                List.copyOf(candidates)
            ));
        }
        loaded.sort(Comparator.comparingInt(EnchantBand::minBookshelves));
        return List.copyOf(loaded);
    }

    private Set<String> lowerSet(List<String> values) {
        Set<String> set = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                set.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(set);
    }

    private Set<Material> loadMaterials(List<String> values) {
        Set<Material> materials = new HashSet<>();
        for (String value : values) {
            Material material = Material.matchMaterial(value == null ? "" : value);
            if (material != null && material.isBlock()) {
                materials.add(material);
            } else {
                plugin.getLogger().warning("Ignoring invalid bookshelf material: " + value);
            }
        }
        if (materials.isEmpty()) {
            materials.add(Material.BOOKSHELF);
        }
        return Set.copyOf(materials);
    }

    private Map<String, String> loadLoreDescriptions() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("lore-descriptions.descriptions");
        if (section == null) {
            return Map.of();
        }
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            descriptions.put(key.toLowerCase(Locale.ROOT), section.getString(key, ""));
        }
        return Map.copyOf(descriptions);
    }

    private int positiveInt(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(raw)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
