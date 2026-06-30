package me.leeseol.town.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class StructureRegistry {
    private final boolean enabled;
    private final String schematicDirectory;
    private final boolean itemsAdderRequired;
    private final List<String> allowedCoreBlocks;
    private final List<String> softOverwriteBlocks;
    private final Map<String, StructureDefinition> definitions;

    private StructureRegistry(
            boolean enabled,
            String schematicDirectory,
            boolean itemsAdderRequired,
            List<String> allowedCoreBlocks,
            List<String> softOverwriteBlocks,
            Map<String, StructureDefinition> definitions
    ) {
        this.enabled = enabled;
        this.schematicDirectory = schematicDirectory;
        this.itemsAdderRequired = itemsAdderRequired;
        this.allowedCoreBlocks = List.copyOf(allowedCoreBlocks);
        this.softOverwriteBlocks = List.copyOf(softOverwriteBlocks);
        this.definitions = Map.copyOf(definitions);
    }

    public static StructureRegistry from(FileConfiguration config) {
        boolean enabled = config.getBoolean("structures.enabled", false);
        String directory = config.getString("structures.schematic-directory", "structures");
        boolean itemsAdderRequired = config.getBoolean("structures.itemsadder.required", true);
        List<String> allowedCoreBlocks = config.getStringList("structures.itemsadder.allowed-core-blocks");
        List<String> softBlocks = config.getStringList("structures.soft-overwrite-blocks");
        Map<String, StructureDefinition> definitions = new LinkedHashMap<>();

        ConfigurationSection root = config.getConfigurationSection("structures.types");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                StructureDefinition definition = new StructureDefinition(
                        id,
                        section.getString("name", id),
                        section.getString("icon", "STONE"),
                        section.getString("schematic", id + ".schem"),
                        StructureCategory.parse(section.getString("category", "normal")),
                        section.getString("permission", ""),
                        section.getString("itemsadder-block", ""),
                        section.getInt("width", 1),
                        section.getInt("height", 1),
                        section.getInt("length", 1),
                        section.getInt("offset-x", 0),
                        section.getInt("offset-y", 0),
                        section.getInt("offset-z", 0),
                        section.getInt("marker-offset-x", 0),
                        section.getInt("marker-offset-y", 0),
                        section.getInt("marker-offset-z", 0)
                );
                definitions.put(id, definition);
            }
        }
        return new StructureRegistry(enabled, directory, itemsAdderRequired, allowedCoreBlocks, softBlocks, definitions);
    }

    public boolean enabled() {
        return enabled;
    }

    public String schematicDirectory() {
        return schematicDirectory;
    }

    public boolean itemsAdderRequired() {
        return itemsAdderRequired;
    }

    public List<String> allowedCoreBlocks() {
        return allowedCoreBlocks;
    }

    public List<String> softOverwriteBlocks() {
        return softOverwriteBlocks;
    }

    public Collection<StructureDefinition> all() {
        return new ArrayList<>(definitions.values());
    }

    public StructureDefinition get(String id) {
        return definitions.get(id);
    }

    public StructureDefinition getByItemsAdderBlock(String namespacedId) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return null;
        }
        for (StructureDefinition definition : definitions.values()) {
            if (namespacedId.equalsIgnoreCase(definition.itemsAdderBlock())) {
                return definition;
            }
        }
        return null;
    }

    public StructureDefinition require(String id) {
        StructureDefinition definition = get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown structure id: " + id);
        }
        return definition;
    }
}
