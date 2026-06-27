package me.leeseol.town.diagnostic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import me.leeseol.town.LeeSeolTownPlugin;
import me.leeseol.town.model.NeutralZone;
import me.leeseol.town.service.TownScoreboardService;
import me.leeseol.town.service.WorldGuardNeutralZoneRegions;
import org.bukkit.Bukkit;

public final class TownDiagnosticService {
    private final LeeSeolTownPlugin plugin;

    public TownDiagnosticService(LeeSeolTownPlugin plugin) {
        this.plugin = plugin;
    }

    public DiagnosticReport run(boolean fix) {
        List<FeatureDiagnostic> results = new ArrayList<>();
        if (fix) {
            applySafeFixes(results);
        }
        diagnoseNeutralZones(results);
        diagnoseWorldGuard(results);
        diagnoseBlueMap(results);
        diagnoseScoreboard(results);
        return new DiagnosticReport(results);
    }

    private void applySafeFixes(List<FeatureDiagnostic> results) {
        try {
            plugin.reloadConfig();
            plugin.neutralZones().reload();
            if (!plugin.neutralZones().usingCoreContent()) {
                plugin.worldGuardNeutralZoneRegions().syncAll(plugin.neutralZones().zones());
            }
            plugin.blueMapNeutralZoneMarkers().refreshLater();
            plugin.blueMapNationClaimMarkers().refreshLater();
            plugin.scoreboardService().start();
            int refreshed = plugin.scoreboardService().refreshOnlinePlayers();
            String worldGuardMode = plugin.neutralZones().usingCoreContent()
                    ? "WorldGuard owned by LeeSeolCore content"
                    : "synced WorldGuard";
            results.add(new FeatureDiagnostic(
                    DiagnosticStatus.FIXED,
                    "town",
                    "reloaded neutral zones, " + worldGuardMode + ", refreshed BlueMap markers, restarted scoreboard, refreshed players=" + refreshed
            ));
        } catch (RuntimeException | LinkageError exception) {
            results.add(new FeatureDiagnostic(
                    DiagnosticStatus.FAIL,
                    "town",
                    "safe fix failed: " + safeMessage(exception)
            ));
        }
    }

    private void diagnoseNeutralZones(List<FeatureDiagnostic> results) {
        File file = plugin.neutralZones().dataFile();
        if (!file.exists()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "neutral-zone", "data file is missing: " + file.getPath()));
        } else if (!file.canRead()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "neutral-zone", "data file is not readable: " + file.getPath()));
        } else {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "neutral-zone", "data file readable: " + file.getPath()));
        }

        List<NeutralZone> zones = List.copyOf(plugin.neutralZones().zones());
        if (zones.isEmpty()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "neutral-zone", "no neutral zones are loaded"));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "neutral-zone", "loaded zones=" + zones.size()));

        long missingWorlds = zones.stream()
                .filter(zone -> Bukkit.getWorld(zone.world()) == null)
                .count();
        if (missingWorlds > 0) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "neutral-zone", "zones with unloaded worlds=" + missingWorlds));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "neutral-zone", "all zone worlds are loaded"));
    }

    private void diagnoseWorldGuard(List<FeatureDiagnostic> results) {
        if (plugin.neutralZones().usingCoreContent()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "worldguard", "neutral-zone regions are owned by LeeSeolCore content"));
            return;
        }

        WorldGuardNeutralZoneRegions regions = plugin.worldGuardNeutralZoneRegions();
        if (regions == null) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "worldguard", "neutral-zone region service is not initialized"));
            return;
        }
        if (!regions.enabledByConfig()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "worldguard", "neutral-zone sync is disabled by config"));
            return;
        }
        if (!regions.worldGuardAvailable()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "worldguard", "WorldGuard is not enabled"));
            return;
        }

        List<NeutralZone> zones = List.copyOf(plugin.neutralZones().zones());
        if (zones.isEmpty()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "worldguard", "no neutral zones to sync"));
            return;
        }

        long missingRegions = zones.stream()
                .filter(zone -> Bukkit.getWorld(zone.world()) != null)
                .filter(zone -> !regions.regionExists(zone))
                .count();
        if (missingRegions > 0) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "worldguard", "missing neutral-zone regions=" + missingRegions));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "worldguard", "all neutral-zone regions exist"));
    }

    private void diagnoseBlueMap(List<FeatureDiagnostic> results) {
        if (!plugin.blueMapNeutralZoneMarkers().enabledByConfig() && !plugin.blueMapNationClaimMarkers().enabledByConfig()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "bluemap", "marker sync is disabled by config"));
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "bluemap", "BlueMap is not enabled"));
            return;
        }
        if (!plugin.blueMapNeutralZoneMarkers().blueMapAvailable() && !plugin.blueMapNationClaimMarkers().blueMapAvailable()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "bluemap", "BlueMap API is not ready yet"));
            return;
        }
        results.add(new FeatureDiagnostic(
                DiagnosticStatus.OK,
                "bluemap",
                "marker API ready, neutralZones=" + plugin.neutralZones().zones().size()
                        + " nationClaims=" + plugin.blueMapNationClaimMarkers().claimCount()
        ));
    }

    private void diagnoseScoreboard(List<FeatureDiagnostic> results) {
        TownScoreboardService scoreboard = plugin.scoreboardService();
        if (scoreboard == null) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "scoreboard", "service is not initialized"));
            return;
        }
        if (!scoreboard.isEnabledByConfig()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "scoreboard", "disabled by config"));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "scoreboard", "enabled by config"));

        if (!scoreboard.isRunning()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "scoreboard", "update task is not running"));
        } else {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "scoreboard", "update task is running"));
        }

        int online = Bukkit.getOnlinePlayers().size();
        if (online == 0) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "scoreboard", "no online players to inspect"));
            return;
        }
        if (scoreboard.boardCount() < online) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "scoreboard", "boards=" + scoreboard.boardCount() + " online=" + online));
            return;
        }
        results.add(new FeatureDiagnostic(
                DiagnosticStatus.OK,
                "scoreboard",
                "boards=" + scoreboard.boardCount() + " zoneLabels=" + scoreboard.zoneLabelCount() + " online=" + online
        ));
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
