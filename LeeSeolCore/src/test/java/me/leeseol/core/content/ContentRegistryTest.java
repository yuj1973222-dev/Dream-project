package me.leeseol.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class ContentRegistryTest {
    @Test
    public void savesAndLoadsRegisteredContent() {
        ContentRegistry registry = new ContentRegistry();
        ContentEntry entry = registry.add(
                ContentType.NEUTRAL,
                "spawn_village",
                null,
                new ContentArea("world", 10, 60, -8, 18, 72, 4),
                new ContentSpawn("world", 12.5D, 65.0D, -2.5D, 90.0F, 12.0F)
        );

        assertEquals("spawn_village", entry.displayName());
        assertEquals(300, entry.radius());
        assertEquals("leeseol_content_neutral_spawn_village", entry.regionId());
        assertEquals("common", entry.preset());
        assertEquals(14.0D, entry.centerX(), 0.001D);
        assertEquals(66.0D, entry.centerY(), 0.001D);
        assertEquals(-2.0D, entry.centerZ(), 0.001D);

        registry.rename(ContentType.NEUTRAL, "spawn_village", "시작 마을");
        registry.setRadius(ContentType.NEUTRAL, "spawn_village", 450);
        registry.setSpawn(
                ContentType.NEUTRAL,
                "spawn_village",
                new ContentSpawn("world", 14.5D, 66.0D, 1.5D, 180.0F, 0.0F)
        );

        YamlConfiguration yaml = new YamlConfiguration();
        registry.saveTo(yaml);

        ContentRegistry loaded = new ContentRegistry();
        loaded.loadFrom(yaml);

        ContentEntry loadedEntry = loaded.require(ContentType.NEUTRAL, "spawn_village");
        assertEquals("시작 마을", loadedEntry.displayName());
        assertEquals(450, loadedEntry.radius());
        assertEquals("world", loadedEntry.world());
        assertEquals(10, loadedEntry.minX());
        assertEquals(72, loadedEntry.maxY());
        assertEquals(14.5D, loadedEntry.spawn().x(), 0.001D);
        assertEquals(180.0F, loadedEntry.spawn().yaw(), 0.001F);
        assertEquals(List.of(loadedEntry), loaded.list(ContentType.NEUTRAL));
    }

    @Test
    public void rejectsUnknownTypesAndUnsafeIds() {
        assertTrue(ContentType.fromKey("casino").isPresent());
        assertFalse(ContentType.fromKey("town").isPresent());

        ContentRegistry registry = new ContentRegistry();
        ContentArea area = new ContentArea("world", 0, 60, 0, 2, 64, 2);
        ContentSpawn spawn = new ContentSpawn("world", 1.5D, 61.0D, 1.5D, 0.0F, 0.0F);

        try {
            registry.add(ContentType.CASINO, "Bad-Id", "Casino", area, spawn);
        } catch (IllegalArgumentException exception) {
            assertEquals("content id must use lowercase letters, numbers, and underscores only", exception.getMessage());
            return;
        }

        throw new AssertionError("unsafe id was accepted");
    }
}
