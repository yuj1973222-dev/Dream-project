package me.leeseol.combat.diagnostic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import me.leeseol.combat.LeeSeolCombatPlugin;
import me.leeseol.combat.config.CombatConfig;
import org.bukkit.Bukkit;

public final class CombatDiagnosticService {
    private static final Set<String> KNOWN_CLONE_MODES = Set.of("lying-corpse", "lying-player", "husk");
    private static final Set<String> KNOWN_HITBOX_DIRECTIONS = Set.of("PLAYER", "NORTH", "SOUTH", "EAST", "WEST");

    private final LeeSeolCombatPlugin plugin;

    public CombatDiagnosticService(LeeSeolCombatPlugin plugin) {
        this.plugin = plugin;
    }

    public DiagnosticReport run(boolean fix) {
        List<FeatureDiagnostic> results = new ArrayList<>();
        if (fix) {
            applySafeFixes(results);
        }
        diagnoseCitizens(results);
        diagnoseCloneConfig(results);
        diagnoseCloneRuntime(results);
        diagnosePendingDeaths(results);
        return new DiagnosticReport(results);
    }

    private void applySafeFixes(List<FeatureDiagnostic> results) {
        try {
            plugin.reloadPluginConfig();
            plugin.combatCloneManager().initializeRegistry();
            int removedHitboxes = plugin.combatCloneManager().cleanupStaleHitboxes();
            results.add(new FeatureDiagnostic(
                    DiagnosticStatus.FIXED,
                    "logout-clone",
                    "reloaded config, initialized registry, removed stale hitboxes=" + removedHitboxes
            ));
        } catch (RuntimeException | LinkageError exception) {
            results.add(new FeatureDiagnostic(
                    DiagnosticStatus.FAIL,
                    "logout-clone",
                    "safe fix failed: " + safeMessage(exception)
            ));
        }
    }

    private void diagnoseCitizens(List<FeatureDiagnostic> results) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "logout-clone", "Citizens is not enabled"));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "Citizens is enabled"));
    }

    private void diagnoseCloneConfig(List<FeatureDiagnostic> results) {
        CombatConfig config = plugin.combatConfig();
        if (!config.isCloneEnabled()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "logout-clone", "disabled by config"));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "enabled by config"));

        String mode = config.cloneMode().toLowerCase(Locale.ROOT);
        if (KNOWN_CLONE_MODES.contains(mode)) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "mode=" + mode));
        } else {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "logout-clone", "unknown mode=" + mode + " (treated as husk)"));
        }

        diagnoseCloneWorlds(results);
        diagnoseHitboxConfig(results, config);
    }

    private void diagnoseCloneWorlds(List<FeatureDiagnostic> results) {
        List<String> worlds = plugin.getConfig().getStringList("logout-clone.enabled-worlds");
        if (worlds.isEmpty()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "all worlds are enabled"));
            return;
        }
        long missingWorlds = worlds.stream()
                .filter(world -> Bukkit.getWorld(world) == null)
                .count();
        if (missingWorlds > 0) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "logout-clone", "configured worlds=" + worlds.size() + " unloaded=" + missingWorlds));
            return;
        }
        results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "configured worlds loaded=" + worlds.size()));
    }

    private void diagnoseHitboxConfig(List<FeatureDiagnostic> results, CombatConfig config) {
        if (!config.hitboxEnabled()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "logout-clone", "interaction hitbox is disabled"));
            return;
        }
        results.add(new FeatureDiagnostic(
                DiagnosticStatus.OK,
                "logout-clone",
                "hitbox width=" + config.hitboxWidth()
                        + " height=" + config.hitboxHeight()
                        + " length=" + config.hitboxLengthBlocks()
        ));

        String direction = config.hitboxDirection();
        if (KNOWN_HITBOX_DIRECTIONS.contains(direction)) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "hitbox direction=" + direction));
        } else {
            results.add(new FeatureDiagnostic(DiagnosticStatus.WARN, "logout-clone", "unknown hitbox direction=" + direction + " (uses player yaw)"));
        }
    }

    private void diagnoseCloneRuntime(List<FeatureDiagnostic> results) {
        if (!plugin.combatCloneManager().registryInitialized()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "logout-clone", "NPC registry is not initialized"));
        } else {
            results.add(new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "NPC registry is initialized"));
        }
        results.add(new FeatureDiagnostic(
                DiagnosticStatus.OK,
                "logout-clone",
                "activeClones=" + plugin.combatCloneManager().activeCloneCount()
                        + " activeHitboxes=" + plugin.combatCloneManager().activeHitboxCount()
        ));
    }

    private void diagnosePendingDeaths(List<FeatureDiagnostic> results) {
        if (plugin.pendingDeathStore() == null) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "pending-death", "store is not initialized"));
            return;
        }
        File file = plugin.pendingDeathStore().file();
        if (file.exists() && !file.canRead()) {
            results.add(new FeatureDiagnostic(DiagnosticStatus.FAIL, "pending-death", "file is not readable: " + file.getPath()));
            return;
        }
        results.add(new FeatureDiagnostic(
                DiagnosticStatus.OK,
                "pending-death",
                "pendingPlayers=" + plugin.pendingDeathStore().count()
        ));
    }

    private String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
