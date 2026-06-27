package me.leeseol.town.service;

import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.ClaimKey;
import me.leeseol.town.model.Nation;
import me.leeseol.town.model.Town;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BlueMapNationClaimMarkers {
    private static final String DEFAULT_SET_ID = "leeseol-nation-claims";
    private static final int MAX_REFRESH_ATTEMPTS = 12;
    private static final List<String> FALLBACK_COLORS = List.of(
            "#8EC5FF", "#FF9AA2", "#FFD166", "#8FD9A8", "#C7A8FF", "#FFB86B", "#7DD3FC", "#F472B6"
    );

    private final LeeSeolTownPlugin plugin;

    public BlueMapNationClaimMarkers(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
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
                    plugin.getLogger().warning("BlueMap API is not ready. Nation claim markers were not refreshed.");
                }
                return;
            }
            int markers = updateMaps(api.get());
            plugin.getLogger().info("Refreshed BlueMap nation-claim markers: " + markers);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not refresh BlueMap nation-claim markers: " + exception.getMessage());
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

    public int claimCount() {
        int count = 0;
        for (Town town : plugin.townService().towns()) {
            if (town.nationId() != null) {
                count += town.claims().size();
            }
        }
        return count;
    }

    private int updateMaps(Object api) throws ReflectiveOperationException {
        Method getMaps = api.getClass().getMethod("getMaps");
        Collection<?> maps = (Collection<?>) getMaps.invoke(api);
        List<NationClaim> claims = nationClaims();
        int total = 0;
        for (Object map : maps) {
            Object markerSet = newMarkerSet();
            int added = 0;
            for (NationClaim claim : claims) {
                if (!matchesMap(map, claim.claim())) {
                    continue;
                }
                putMarker(markerSet, claim);
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

    private List<NationClaim> nationClaims() {
        Map<String, Nation> nations = new LinkedHashMap<>();
        for (Nation nation : plugin.townService().nations()) {
            nations.put(nation.id(), nation);
        }

        List<NationClaim> claims = new ArrayList<>();
        for (Town town : plugin.townService().towns()) {
            if (town.nationId() == null) {
                continue;
            }
            Nation nation = nations.get(town.nationId());
            if (nation == null) {
                continue;
            }
            for (ClaimKey claim : town.claims()) {
                claims.add(new NationClaim(nation, town, claim));
            }
        }
        return claims;
    }

    private Object newMarkerSet() throws ReflectiveOperationException {
        Class<?> markerSetClass = blueMapClass("de.bluecolored.bluemap.api.markers.MarkerSet");
        Constructor<?> constructor = markerSetClass.getConstructor(String.class, boolean.class, boolean.class);
        Object markerSet = constructor.newInstance(label(), true, defaultHidden());
        markerSetClass.getMethod("setSorting", int.class).invoke(markerSet, sorting());
        return markerSet;
    }

    private void putMarker(Object markerSet, NationClaim claim) throws ReflectiveOperationException {
        Object shape = shape(claim.claim());
        Object marker = extrudeMarker(claim, shape);
        markerSet.getClass().getMethod(
                "put",
                String.class,
                blueMapClass("de.bluecolored.bluemap.api.markers.Marker")
        ).invoke(markerSet, markerId(claim), marker);
    }

    private Object shape(ClaimKey claim) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        double minX = claim.x() * 16.0D;
        double minZ = claim.z() * 16.0D;
        return shapeClass.getMethod("createRect", double.class, double.class, double.class, double.class)
                .invoke(null, minX, minZ, minX + 16.0D, minZ + 16.0D);
    }

    private Object extrudeMarker(NationClaim claim, Object shape) throws ReflectiveOperationException {
        Class<?> shapeClass = blueMapClass("de.bluecolored.bluemap.api.math.Shape");
        Class<?> markerClass = blueMapClass("de.bluecolored.bluemap.api.markers.ExtrudeMarker");
        Constructor<?> constructor = markerClass.getConstructor(String.class, shapeClass, float.class, float.class);
        Object marker = constructor.newInstance(
                claim.nation().name() + " / " + claim.claim().x() + ", " + claim.claim().z(),
                shape,
                (float) minY(),
                (float) maxY()
        );
        String color = colorFor(claim.nation());
        markerClass.getMethod("setLineWidth", int.class).invoke(marker, lineWidth());
        markerClass.getMethod("setDepthTestEnabled", boolean.class).invoke(marker, depthTest());
        markerClass.getMethod(
                "setColors",
                blueMapClass("de.bluecolored.bluemap.api.math.Color"),
                blueMapClass("de.bluecolored.bluemap.api.math.Color")
        ).invoke(marker, color(color, lineAlpha()), color(color, fillAlpha()));
        markerClass.getMethod("setDetail", String.class).invoke(marker, detail(claim));
        markerClass.getMethod("setListed", boolean.class).invoke(marker, true);
        markerClass.getMethod("setSorting", int.class).invoke(marker, 0);
        return marker;
    }

    private Object color(String hex, float alpha) throws ReflectiveOperationException {
        Class<?> colorClass = blueMapClass("de.bluecolored.bluemap.api.math.Color");
        Constructor<?> constructor = colorClass.getConstructor(int.class, float.class);
        return constructor.newInstance(parseHex(hex), alpha);
    }

    private boolean matchesMap(Object map, ClaimKey claim) throws ReflectiveOperationException {
        String mapId = stringMethod(map, "getId");
        List<String> override = mapOverrides(claim.world());
        if (!override.isEmpty()) {
            return override.stream().anyMatch(id -> id.equalsIgnoreCase(mapId));
        }
        if (claim.world().equalsIgnoreCase("world")) {
            return mapId.equalsIgnoreCase("overworld");
        }

        Object blueMapWorld = map.getClass().getMethod("getWorld").invoke(map);
        String worldId = stringMethod(blueMapWorld, "getId");
        if (claim.world().equalsIgnoreCase(mapId) || claim.world().equalsIgnoreCase(worldId)) {
            return true;
        }
        Object saveFolder = blueMapWorld.getClass().getMethod("getSaveFolder").invoke(blueMapWorld);
        if (saveFolder instanceof Path path && path.getFileName() != null) {
            return claim.world().equalsIgnoreCase(path.getFileName().toString());
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
        String path = "nation-claims.bluemap.map-overrides." + world;
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

    private String markerId(NationClaim claim) {
        return "nation_" + safeId(claim.nation().id()) + "_" + safeId(claim.claim().world())
                + "_" + claim.claim().x() + "_" + claim.claim().z();
    }

    private String detail(NationClaim claim) {
        return "<b>Nation Territory</b><br>"
                + "Nation: " + claim.nation().name() + "<br>"
                + "Town: " + claim.town().name() + "<br>"
                + "Chunk: " + claim.claim().display();
    }

    private String safeId(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private String colorFor(Nation nation) {
        return nation.color() == null ? FALLBACK_COLORS.get(0) : nation.color().hex();
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("nation-claims.bluemap.enabled", true);
    }

    private String setId() {
        return plugin.getConfig().getString("nation-claims.bluemap.marker-set-id", DEFAULT_SET_ID);
    }

    private String label() {
        return plugin.getConfig().getString("nation-claims.bluemap.marker-set-label", "Nation Territory");
    }

    private boolean defaultHidden() {
        return plugin.getConfig().getBoolean("nation-claims.bluemap.default-hidden", false);
    }

    private int sorting() {
        return plugin.getConfig().getInt("nation-claims.bluemap.sorting", 30);
    }

    private int lineWidth() {
        return Math.max(0, plugin.getConfig().getInt("nation-claims.bluemap.line-width", 1));
    }

    private boolean depthTest() {
        return plugin.getConfig().getBoolean("nation-claims.bluemap.depth-test", false);
    }

    private int minY() {
        return plugin.getConfig().getInt("nation-claims.bluemap.min-y", 60);
    }

    private int maxY() {
        return Math.max(minY() + 1, plugin.getConfig().getInt("nation-claims.bluemap.max-y", 320));
    }

    private float lineAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("nation-claims.bluemap.line-alpha", 0.85D));
    }

    private float fillAlpha() {
        return clampAlpha((float) plugin.getConfig().getDouble("nation-claims.bluemap.fill-alpha", 0.12D));
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
            return 0x8EC5FF;
        }
    }

    private record NationClaim(Nation nation, Town town, ClaimKey claim) {
    }
}
