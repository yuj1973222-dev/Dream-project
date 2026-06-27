package me.leeseol.core.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;

public final class ContentRegistry {
    private static final Pattern CONTENT_ID_PATTERN = Pattern.compile("[a-z0-9_]+");
    private final Map<ContentType, Map<String, ContentEntry>> contents = new EnumMap<>(ContentType.class);

    public ContentRegistry() {
        for (ContentType type : ContentType.values()) {
            contents.put(type, new LinkedHashMap<>());
        }
    }

    public ContentEntry add(
            ContentType type,
            String id,
            String displayName,
            ContentArea area,
            ContentSpawn spawn
    ) {
        validateId(id);
        String normalizedId = normalizeId(id);
        Map<String, ContentEntry> typed = contents.get(type);
        if (typed.containsKey(normalizedId)) {
            throw new IllegalArgumentException("content already exists");
        }

        ContentEntry entry = ContentEntry.create(type, normalizedId, displayName, area, spawn);
        typed.put(normalizedId, entry);
        return entry;
    }

    public Optional<ContentEntry> remove(ContentType type, String id) {
        validateId(id);
        return Optional.ofNullable(contents.get(type).remove(normalizeId(id)));
    }

    public ContentEntry rename(ContentType type, String id, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("content display name is required");
        }
        return replace(type, id, require(type, id).withDisplayName(displayName));
    }

    public ContentEntry setRadius(ContentType type, String id, int radius) {
        return replace(type, id, require(type, id).withRadius(radius));
    }

    public ContentEntry setSpawn(ContentType type, String id, ContentSpawn spawn) {
        return replace(type, id, require(type, id).withSpawn(spawn));
    }

    public Optional<ContentEntry> find(ContentType type, String id) {
        validateId(id);
        return Optional.ofNullable(contents.get(type).get(normalizeId(id)));
    }

    public ContentEntry require(ContentType type, String id) {
        return find(type, id).orElseThrow(() -> new IllegalArgumentException("content not found"));
    }

    public List<ContentEntry> list() {
        List<ContentEntry> entries = new ArrayList<>();
        for (ContentType type : ContentType.values()) {
            entries.addAll(contents.get(type).values());
        }
        return List.copyOf(entries);
    }

    public List<ContentEntry> list(ContentType type) {
        return List.copyOf(contents.get(type).values());
    }

    public void clear() {
        contents.values().forEach(Map::clear);
    }

    public void loadFrom(ConfigurationSection data) {
        clear();
        ConfigurationSection root = data.getConfigurationSection("contents");
        if (root == null) {
            return;
        }

        for (String typeKey : root.getKeys(false)) {
            Optional<ContentType> maybeType = ContentType.fromKey(typeKey);
            if (maybeType.isEmpty()) {
                continue;
            }
            ContentType type = maybeType.get();
            ConfigurationSection typed = root.getConfigurationSection(typeKey);
            if (typed == null) {
                continue;
            }
            for (String id : typed.getKeys(false)) {
                ConfigurationSection section = typed.getConfigurationSection(id);
                if (section != null) {
                    put(loadEntry(type, id, section));
                }
            }
        }
    }

    public void saveTo(ConfigurationSection data) {
        data.set("contents", null);
        for (ContentEntry entry : list()) {
            String base = "contents." + entry.type().key() + "." + entry.id() + ".";
            data.set(base + "type", entry.type().key());
            data.set(base + "id", entry.id());
            data.set(base + "displayName", entry.displayName());
            data.set(base + "world", entry.world());
            data.set(base + "box.min.x", entry.minX());
            data.set(base + "box.min.y", entry.minY());
            data.set(base + "box.min.z", entry.minZ());
            data.set(base + "box.max.x", entry.maxX());
            data.set(base + "box.max.y", entry.maxY());
            data.set(base + "box.max.z", entry.maxZ());
            data.set(base + "center.x", entry.centerX());
            data.set(base + "center.y", entry.centerY());
            data.set(base + "center.z", entry.centerZ());
            data.set(base + "radius", entry.radius());
            data.set(base + "spawn.world", entry.spawn().world());
            data.set(base + "spawn.x", entry.spawn().x());
            data.set(base + "spawn.y", entry.spawn().y());
            data.set(base + "spawn.z", entry.spawn().z());
            data.set(base + "spawn.yaw", entry.spawn().yaw());
            data.set(base + "spawn.pitch", entry.spawn().pitch());
            data.set(base + "region-id", entry.regionId());
            data.set(base + "preset", entry.preset());
        }
    }

    public Collection<ContentEntry> entries() {
        return list();
    }

    public static void validateId(String id) {
        if (id == null || !CONTENT_ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("content id must use lowercase letters, numbers, and underscores only");
        }
    }

    private ContentEntry replace(ContentType type, String id, ContentEntry entry) {
        validateId(id);
        contents.get(type).put(normalizeId(id), entry);
        return entry;
    }

    private void put(ContentEntry entry) {
        contents.get(entry.type()).put(entry.id(), entry);
    }

    private ContentEntry loadEntry(ContentType type, String id, ConfigurationSection section) {
        validateId(id);
        String world = section.getString("world", "");
        int minX = section.getInt("box.min.x");
        int minY = section.getInt("box.min.y");
        int minZ = section.getInt("box.min.z");
        int maxX = section.getInt("box.max.x");
        int maxY = section.getInt("box.max.y");
        int maxZ = section.getInt("box.max.z");
        ContentArea area = new ContentArea(world, minX, minY, minZ, maxX, maxY, maxZ);
        ContentSpawn spawn = new ContentSpawn(
                section.getString("spawn.world", world),
                section.getDouble("spawn.x"),
                section.getDouble("spawn.y"),
                section.getDouble("spawn.z"),
                (float) section.getDouble("spawn.yaw"),
                (float) section.getDouble("spawn.pitch")
        );

        return new ContentEntry(
                type,
                normalizeId(id),
                section.getString("displayName", id),
                world,
                area.minX(),
                area.minY(),
                area.minZ(),
                area.maxX(),
                area.maxY(),
                area.maxZ(),
                section.getDouble("center.x", area.centerX()),
                section.getDouble("center.y", area.centerY()),
                section.getDouble("center.z", area.centerZ()),
                Math.max(1, section.getInt("radius", ContentEntry.DEFAULT_RADIUS)),
                spawn,
                section.getString("region-id", ContentEntry.defaultRegionId(type, normalizeId(id))),
                section.getString("preset", ContentEntry.DEFAULT_PRESET)
        );
    }

    private static String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
