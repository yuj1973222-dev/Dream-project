package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.NeutralZone;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BlueMapNeutralZoneMarkers {
    private static final String DEFAULT_SET_ID = "leeseol-neutral-zones";
    private static final int MAX_REFRESH_ATTEMPTS = 12;

    private final LeeSeolTownPlugin plugin;
    private final NeutralZoneManager neutralZones;

    public BlueMapNeutralZoneMarkers(LeeSeolTownPlugin plugin, NeutralZoneManager neutralZones) {
        this.plugin = plugin;
        this.neutralZones = neutralZones;
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
        try {
            Optional<?> api = blueMapApi();
            if (api.isEmpty()) {
                if (attempt < MAX_REFRESH_ATTEMPTS) {
                    refreshLater(attempt + 1);
                } else {
                    plugin.getLogger().warning("BlueMap API is not ready. Neutral-zone markers were not refreshed.");
                }
                return;
            }
            int markers = updateMaps(api.get());
            plugin.getLogger().info("Refreshed BlueMap neutral-zone markers: " + markers);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not refresh BlueMap neutral-zone markers: " + exception.getMessage());
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
            for (NeutralZone zone : neutralZones.zones()) {
                if (!matchesMap(map, zone)) {
                    continue;
                }
                putMarker(markerSet, zone);
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

    private void putMarker(Object markerSet, NeutralZone zone) throws ReflectiveOperationException {
        Object shape = shape(zone);
        Object marker = extrudeMarker(zone, shape);
        markerSet.getClass().getMethod(
                "put",
                String.class,
                blueMapClass("de.bluecolored.bluemap.api.markers.Marker")
        ).invoke(markerSet, markerId(zone), marker);
    }

    private Object shape(NeutralZone zone) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        return shapeClass.getMethod("createRect", double.class, double.class, double.class, double.class)
                .invoke(null, (double) zone.minX(), (double) zone.minZ(), (double) zone.maxX() + 1.0D, (double) zone.maxZ() + 1.0D);
    }

    private Object extrudeMarker(NeutralZone zone, Object shape) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        Class<?> markerClass = blueMapClass("de.bluecolored.bluemap.api.markers.ExtrudeMarker");
        Constructor<?> constructor = markerClass.getConstructor(String.class, shapeClass, float.class, float.class);
        Object marker = constructor.newInstance("중립구역: " + zone.id(), shape, (float) zone.minY(), (float) zone.maxY() + 1.0F);
        markerClass.getMethod("setLineWidth", int.class).invoke(marker, lineWidth());
        markerClass.getMethod("setDepthTestEnabled", boolean.class).invoke(marker, depthTest());
        markerClass.getMethod(
                "setColors",
                blueMapClass("de.bluecolored.bluemap.api.math.Color"),
                blueMapClass("de.bluecolored.bluemap.api.math.Color")
        ).invoke(marker, color(lineColor(), lineAlpha()), color(fillColor(), fillAlpha()));
        markerClass.getMethod("setDetail", String.class).invoke(marker, detail(zone));
        markerClass.getMethod("setListed", boolean.class).invoke(marker, true);
        markerClass.getMethod("setSorting", int.class).invoke(marker, 0);
        return marker;
    }

    private Object color(String hex, float alpha) throws ReflectiveOperationException {
        Class<?> colorClass = blueMapClass("de.bluecolored.bluemap.api.math.Color");
        Constructor<?> constructor = colorClass.getConstructor(int.class, float.class);
        return constructor.newInstance(parseHex(hex), alpha);
    }

    private boolean matchesMap(Object map, NeutralZone zone) throws ReflectiveOperationException {
        String mapId = stringMethod(map, "getId");
        List<String> override = mapOverrides(zone.world());
        if (!override.isEmpty()) {
            return override.stream().anyMatch(id -> id.equalsIgnoreCase(mapId));
        }
        if (zone.world().equalsIgnoreCase("world")) {
            return mapId.equalsIgnoreCase("overworld");
        }

        Object blueMapWorld = map.getClass().getMethod("getWorld").invoke(map);
        String worldId = stringMethod(blueMapWorld, "getId");
        if (zone.world().equalsIgnoreCase(mapId) || zone.world().equalsIgnoreCase(worldId)) {
            return true;
        }
        Object saveFolder = blueMapWorld.getClass().getMethod("getSaveFolder").invoke(blueMapWorld);
        if (saveFolder instanceof Path path && path.getFileName() != null) {
            return zone.world().equalsIgnoreCase(path.getFileName().toString());
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
        String path = "neutral-zones.bluemap.map-overrides." + world;
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

    private String markerId(NeutralZone zone) {
        return "neutral_" + zone.id().toLowerCase(Locale.ROOT);
    }

    private String detail(NeutralZone zone) {
        return "<b>중립구역</b><br>"
                + "ID: " + zone.id() + "<br>"
                + "보호 범위: " + zone.blockRange() + "<br>"
                + "점령금지: " + zone.claimRange();
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("neutral-zones.bluemap.enabled", true);
    }

    private String setId() {
        return plugin.getConfig().getString("neutral-zones.bluemap.marker-set-id", DEFAULT_SET_ID);
    }

    private String label() {
        return plugin.getConfig().getString("neutral-zones.bluemap.marker-set-label", "중립구역");
    }

    private boolean defaultHidden() {
        return plugin.getConfig().getBoolean("neutral-zones.bluemap.default-hidden", false);
    }

    private int sorting() {
        return plugin.getConfig().getInt("neutral-zones.bluemap.sorting", 20);
    }

    private int lineWidth() {
        return Math.max(0, plugin.getConfig().getInt("neutral-zones.bluemap.line-width", 2));
    }

    private boolean depthTest() {
        return plugin.getConfig().getBoolean("neutral-zones.bluemap.depth-test", false);
    }

    private String lineColor() {
        return plugin.getConfig().getString("neutral-zones.bluemap.line-color", "#8FD9A8");
    }

    private String fillColor() {
        return plugin.getConfig().getString("neutral-zones.bluemap.fill-color", "#8FD9A8");
    }

    private float lineAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("neutral-zones.bluemap.line-alpha", 0.9D));
    }

    private float fillAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("neutral-zones.bluemap.fill-alpha", 0.18D));
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
            return 0x8FD9A8;
        }
    }
}
