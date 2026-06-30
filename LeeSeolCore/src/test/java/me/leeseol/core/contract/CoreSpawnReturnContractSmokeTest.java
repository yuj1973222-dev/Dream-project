package me.leeseol.core.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class CoreSpawnReturnContractSmokeTest {
    @Test
    public void descriptorAndBootstrapKeepSurvivalSpawnCommandContract() throws IOException {
        YamlConfiguration plugin = loadYaml("src/main/resources/plugin.yml");
        String pluginSource = readText("src/main/java/me/leeseol/core/LeeSeolCorePlugin.java");

        assertTrue(plugin.contains("commands.survivalspawn"));
        assertEquals(List.of("returnspawn", "lsspawn"), plugin.getStringList("commands.survivalspawn.aliases"));
        assertTrue(plugin.contains("permissions.leeseolcore.survivalspawn"));
        assertTrue(pluginSource.contains("registerCommand(\"survivalspawn\", services.survivalSpawnManager())"));
        assertTrue(pluginSource.contains("registerEvents(services.survivalSpawnManager(), this)"));
    }

    @Test
    public void configAndManagerKeepSurvivalRespawnAndReturnPathsStable() throws IOException {
        YamlConfiguration config = loadYaml("src/main/resources/config.yml");
        String managerSource = readText("src/main/java/me/leeseol/core/spawn/SurvivalSpawnManager.java");

        assertTrue(config.getBoolean("survival-respawn.enabled"));
        assertEquals("world", config.getString("survival-respawn.target-world"));
        assertTrue(config.getBoolean("survival-spawn-return.enabled"));
        assertEquals("world", config.getString("survival-spawn-return.target-world"));
        assertEquals(8L, config.getLong("survival-spawn-return.warmup-seconds"));
        assertTrue(config.getStringList("survival-spawn-return.worlds").contains("world"));
        assertTrue(managerSource.contains("private static final String RESPAWN_PATH = \"survival-respawn\";"));
        assertTrue(managerSource.contains("private static final String RETURN_PATH = \"survival-spawn-return\";"));
        assertTrue(managerSource.contains("player.hasPermission(\"leeseolcore.survivalspawn\")"));
    }

    private static YamlConfiguration loadYaml(String path) {
        return YamlConfiguration.loadConfiguration(projectPath(path).toFile());
    }

    private static String readText(String path) throws IOException {
        return Files.readString(projectPath(path), StandardCharsets.UTF_8);
    }

    private static Path projectPath(String path) {
        Path modulePath = Path.of(path);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("LeeSeolCore").resolve(path);
    }
}
