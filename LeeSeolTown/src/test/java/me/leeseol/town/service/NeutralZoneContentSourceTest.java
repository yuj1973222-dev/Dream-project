package me.leeseol.town.service;

import static org.junit.Assert.assertEquals;

import java.util.List;
import me.leeseol.town.model.NeutralZone;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class NeutralZoneContentSourceTest {
    @Test
    public void loadsNeutralZonesFromCoreContentRegistry() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("contents.neutral.central.type", "neutral");
        data.set("contents.neutral.central.id", "central");
        data.set("contents.neutral.central.world", "world");
        data.set("contents.neutral.central.box.min.x", -10);
        data.set("contents.neutral.central.box.min.y", 60);
        data.set("contents.neutral.central.box.min.z", -20);
        data.set("contents.neutral.central.box.max.x", 30);
        data.set("contents.neutral.central.box.max.y", 90);
        data.set("contents.neutral.central.box.max.z", 40);

        List<NeutralZone> zones = NeutralZoneContentSource.loadCoreNeutralZones(data, 2);

        assertEquals(1, zones.size());
        NeutralZone zone = zones.get(0);
        assertEquals("central", zone.id());
        assertEquals("world", zone.world());
        assertEquals(-10, zone.minX());
        assertEquals(90, zone.maxY());
        assertEquals(2, zone.claimBufferChunks());
    }

    @Test
    public void ignoresNonNeutralContent() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("contents.dungeon.test.type", "dungeon");
        data.set("contents.dungeon.test.id", "test");
        data.set("contents.dungeon.test.world", "world");
        data.set("contents.dungeon.test.box.min.x", 0);
        data.set("contents.dungeon.test.box.min.y", 60);
        data.set("contents.dungeon.test.box.min.z", 0);
        data.set("contents.dungeon.test.box.max.x", 5);
        data.set("contents.dungeon.test.box.max.y", 70);
        data.set("contents.dungeon.test.box.max.z", 5);

        assertEquals(0, NeutralZoneContentSource.loadCoreNeutralZones(data, 2).size());
    }
}
