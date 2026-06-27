package me.leeseol.core.content;

import me.leeseol.core.LeeSeolCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BlueMapContentMarkers {
    private static final String DEFAULT_SET_ID = "leeseol-content-areas";
    private static final int MAX_REFRESH_ATTEMPTS = 12;

    private final LeeSeolCorePlugin plugin;
    private final ContentService contentService;

    public BlueMapContentMarkers(LeeSeolCorePlugin plugin, ContentService contentService) {
        this.plugin = plugin;
        this.contentService = contentService;
    }

    public void refreshLater() {
        refreshLater(0);
    }

    private void refreshLater(int attempt) {
        if (!enabled()) {
            return;
        }
        long delay = attempt == 0 ? 20L : 100L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refresh(attempt), delay);
    }

    public void refresh() {
        refresh(0);
    }

    private void refresh(int attempt) {
        if (!enabled()) {
            clear();
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("BlueMap") == null) {
            return;
        }
        try {
            Optional<?> api = blueMapApi();
            if (api.isEmpty()) {
                if (attempt < MAX_REFRESH_ATTEMPTS) {
                    refreshLater(attempt + 1);
                } else {
                    plugin.getLogger().warning("BlueMap API is not ready. Content markers were not refreshed.");
                }
                return;
            }
            int markers = updateMaps(api.get());
            plugin.getLogger().info("Refreshed BlueMap content markers: " + markers);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not refresh BlueMap content markers: " + exception.getMessage());
        }
    }

    public void clear() {
        try {
            Optional<?> api = blueMapApi();
            if (api.isEmpty()) {
                return;
            }
            Method getMaps = api.get().getClass().getMethod("getMaps");
            Collection<?> maps = (Collection<?>) getMaps.invoke(api.get());
            for (Object map : maps) {
                markerSets(map).remove(setId());
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // BlueMap may already be disabled during shutdown.
        }
    }

    public boolean enabledByConfig() {
        return enabled();
    }

    public boolean blueMapAvailable() {
        try {
            return blueMapApi().isPresent();
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private int updateMaps(Object api) throws ReflectiveOperationException {
        Method getMaps = api.getClass().getMethod("getMaps");
        Collection<?> maps = (Collection<?>) getMaps.invoke(api);
        int total = 0;
        for (Object map : maps) {
            Object markerSet = newMarkerSet();
            int added = 0;
            for (ContentEntry entry : contentService.list()) {
                if (!ContentBlueMapMarkerPolicy.visibleOnBlueMap(entry.type()) || !matchesMap(map, entry)) {
                    continue;
                }
                putMarker(markerSet, entry);
                added++;
            }
            Map<?, ?> markerSets = markerSets(map);
            if (added == 0) {
                markerSets.remove(setId());
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> writable = (Map<String, Object>) markerSets;
                writable.put(setId(), markerSet);
            }
            total += added;
        }
        return total;
    }

    private Object newMarkerSet() throws ReflectiveOperationException {
        Class<?> markerSetClass = blueMapClass("de.bluecolored.bluemap.api.markers.MarkerSet");
        Constructor<?> constructor = markerSetClass.getConstructor(String.class, boolean.class, boolean.class);
        Object markerSet = constructor.newInstance(label(), true, defaultHidden());
        markerSetClass.getMethod("setSorting", int.class).invoke(markerSet, sorting());
        return markerSet;
    }

    private void putMarker(Object markerSet, ContentEntry entry) throws ReflectiveOperationException {
        Object shape = shape(entry);
        Object marker = extrudeMarker(entry, shape);
        markerSet.getClass().getMethod(
                "put",
                String.class,
                blueMapClass("de.bluecolored.bluemap.api.markers.Marker")
        ).invoke(markerSet, ContentBlueMapMarkerPolicy.markerId(entry.type(), entry.id()), marker);
    }

    private Object shape(ContentEntry entry) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        return shapeClass.getMethod("createRect", double.class, double.class, double.class, double.class)
                .invoke(null, (double) entry.minX(), (double) entry.minZ(), (double) entry.maxX() + 1.0D, (double) entry.maxZ() + 1.0D);
    }

    private Object extrudeMarker(ContentEntry entry, Object shape) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        Class<?> markerClass = blueMapClass("de.bluecolored.bluemap.api.markers.ExtrudeMarker");
        Constructor<?> constructor = markerClass.getConstructor(String.class, shapeClass, float.class, float.class);
        Object marker = constructor.newInstance(labelPrefix(entry.type()) + ": " + entry.displayName(), shape, (float) entry.minY(), (float) entry.maxY() + 1.0F);
        markerClass.getMethod("setLineWidth", int.class).invoke(marker, lineWidth());
        markerClass.getMethod("setDepthTestEnabled", boolean.class).invoke(marker, depthTest());
        markerClass.getMethod(
                "setColors",
                blueMapClass("de.bluecolored.bluemap.api.math.Color"),
                blueMapClass("de.bluecolored.bluemap.api.math.Color")
        ).invoke(marker, color(lineColor(entry.type()), lineAlpha()), color(fillColor(entry.type()), fillAlpha()));
        markerClass.getMethod("setDetail", String.class).invoke(marker, detail(entry));
        markerClass.getMethod("setListed", boolean.class).invoke(marker, true);
        markerClass.getMethod("setSorting", int.class).invoke(marker, 0);
        return marker;
    }

    private Object color(String hex, float alpha) throws ReflectiveOperationException {
        Class<?> colorClass = blueMapClass("de.bluecolored.bluemap.api.math.Color");
        Constructor<?> constructor = colorClass.getConstructor(int.class, float.class);
        return constructor.newInstance(parseHex(hex), alpha);
    }

    private boolean matchesMap(Object map, ContentEntry entry) throws ReflectiveOperationException {
        String mapId = stringMethod(map, "getId");
        List<String> override = mapOverrides(entry.world());
        if (!override.isEmpty()) {
            return override.stream().anyMatch(id -> id.equalsIgnoreCase(mapId));
        }
        if (entry.world().equalsIgnoreCase("world")) {
            return mapId.equalsIgnoreCase("overworld");
        }

        Object blueMapWorld = map.getClass().getMethod("getWorld").invoke(map);
        String worldId = stringMethod(blueMapWorld, "getId");
        if (entry.world().equalsIgnoreCase(mapId) || entry.world().equalsIgnoreCase(worldId)) {
            return true;
        }
        Object saveFolder = blueMapWorld.getClass().getMethod("getSaveFolder").invoke(blueMapWorld);
        if (saveFolder instanceof Path path && path.getFileName() != null) {
            return entry.world().equalsIgnoreCase(path.getFileName().toString());
        }
        return false;
    }

    private Optional<?> blueMapApi() throws ReflectiveOperationException {
        if (plugin.getServer().getPluginManager().getPlugin("BlueMap") == null) {
            return Optional.empty();
        }
        Class<?> apiClass = blueMapClass("de.bluecolored.bluemap.api.BlueMapAPI");
        return (Optional<?>) apiClass.getMethod("getInstance").invoke(null);
    }

    private Class<?> blueMapClass(String name) throws ClassNotFoundException {
        org.bukkit.plugin.Plugin blueMap = plugin.getServer().getPluginManager().getPlugin("BlueMap");
        if (blueMap == null) {
            return Class.forName(name);
        }
        return Class.forName(name, true, blueMap.getClass().getClassLoader());
    }

    private Map<?, ?> markerSets(Object map) throws ReflectiveOperationException {
        return (Map<?, ?>) map.getClass().getMethod("getMarkerSets").invoke(map);
    }

    private String stringMethod(Object target, String method) throws ReflectiveOperationException {
        return String.valueOf(target.getClass().getMethod(method).invoke(target));
    }

    private List<String> mapOverrides(String world) {
        String path = "content-registry.bluemap.map-overrides." + world;
        FileConfiguration config = plugin.getConfig();
        if (config.isList(path)) {
            return config.getStringList(path);
        }
        String value = config.getString(path, "");
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value);
    }

    private String detail(ContentEntry entry) {
        return "<b>" + labelPrefix(entry.type()) + "</b><br>"
                + "ID: " + entry.id() + "<br>"
                + "Name: " + entry.displayName() + "<br>"
                + "World: " + entry.world() + "<br>"
                + "Box: " + entry.minX() + "," + entry.minY() + "," + entry.minZ()
                + " -> " + entry.maxX() + "," + entry.maxY() + "," + entry.maxZ() + "<br>"
                + "Radius: " + entry.radius() + "<br>"
                + "Spawn: " + round(entry.spawn().x()) + ", " + round(entry.spawn().y()) + ", " + round(entry.spawn().z());
    }

    private String labelPrefix(ContentType type) {
        String fallback = type == ContentType.CASINO ? "Casino" : "Dungeon";
        return plugin.getConfig().getString("content-registry.bluemap.types." + type.key() + ".label-prefix", fallback);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("content-registry.bluemap.enabled", true);
    }

    private String setId() {
        return plugin.getConfig().getString("content-registry.bluemap.marker-set-id", DEFAULT_SET_ID);
    }

    private String label() {
        return plugin.getConfig().getString("content-registry.bluemap.marker-set-label", "Content Areas");
    }

    private boolean defaultHidden() {
        return plugin.getConfig().getBoolean("content-registry.bluemap.default-hidden", false);
    }

    private int sorting() {
        return plugin.getConfig().getInt("content-registry.bluemap.sorting", 25);
    }

    private int lineWidth() {
        return Math.max(0, plugin.getConfig().getInt("content-registry.bluemap.line-width", 2));
    }

    private boolean depthTest() {
        return plugin.getConfig().getBoolean("content-registry.bluemap.depth-test", false);
    }

    private String lineColor(ContentType type) {
        return plugin.getConfig().getString("content-registry.bluemap.types." + type.key() + ".line-color", fallbackColor(type));
    }

    private String fillColor(ContentType type) {
        return plugin.getConfig().getString("content-registry.bluemap.types." + type.key() + ".fill-color", fallbackColor(type));
    }

    private String fallbackColor(ContentType type) {
        return type == ContentType.CASINO ? "#FFD166" : "#B976FF";
    }

    private float lineAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("content-registry.bluemap.line-alpha", 0.9D));
    }

    private float fillAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("content-registry.bluemap.fill-alpha", 0.16D));
    }

    private float clampAlpha(float alpha) {
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private int parseHex(String input) {
        String value = input == null ? "" : input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException exception) {
            return 0xB976FF;
        }
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
