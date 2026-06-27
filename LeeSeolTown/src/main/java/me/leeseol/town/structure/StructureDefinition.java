package me.leeseol.town.structure;

public record StructureDefinition(
        String id,
        String name,
        String icon,
        String schematic,
        StructureCategory category,
        String permission,
        String itemsAdderBlock,
        int width,
        int height,
        int length,
        int offsetX,
        int offsetY,
        int offsetZ,
        int markerOffsetX,
        int markerOffsetY,
        int markerOffsetZ
) {
    public StructureDefinition(
            String id,
            String name,
            String icon,
            String schematic,
            StructureCategory category,
            String permission,
            int width,
            int height,
            int length,
            int offsetX,
            int offsetY,
            int offsetZ
    ) {
        this(id, name, icon, schematic, category, permission, "", width, height, length, offsetX, offsetY, offsetZ, 0, 0, 0);
    }

    public StructureDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("structure id is required");
        }
        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IllegalArgumentException("structure dimensions must be positive for " + id);
        }
        name = name == null || name.isBlank() ? id : name;
        icon = icon == null || icon.isBlank() ? "STONE" : icon;
        schematic = schematic == null ? "" : schematic.trim();
        category = category == null ? StructureCategory.NORMAL : category;
        permission = permission == null ? "" : permission;
        itemsAdderBlock = itemsAdderBlock == null ? "" : itemsAdderBlock;
    }

    public boolean nationCore() {
        return category == StructureCategory.NATION_CORE;
    }

    public boolean hasSchematic() {
        return !schematic.isBlank();
    }
}
