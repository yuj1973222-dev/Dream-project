package me.leeseol.town.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class StructureRegistryTest {
    @Test
    public void loadsConfiguredSchemStructures() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("structures.enabled", true);
        config.set("structures.schematic-directory", "structures");
        config.set("structures.itemsadder.required", true);
        config.set("structures.itemsadder.allowed-core-blocks", List.of("leeseolwar:capital_core"));
        config.set("structures.types.nation_core.name", "&bNation Core");
        config.set("structures.types.nation_core.icon", "BEACON");
        config.set("structures.types.nation_core.itemsadder-block", "leeseolwar:capital_core");
        config.set("structures.types.nation_core.schematic", "nation_core.schem");
        config.set("structures.types.nation_core.category", "nation-core");
        config.set("structures.types.nation_core.permission", "leeseoltown.structure.nation_core");
        config.set("structures.types.nation_core.width", 15);
        config.set("structures.types.nation_core.height", 12);
        config.set("structures.types.nation_core.length", 15);
        config.set("structures.types.nation_core.offset-x", -7);
        config.set("structures.types.nation_core.offset-y", 0);
        config.set("structures.types.nation_core.offset-z", -7);
        config.set("structures.types.nation_core.marker-offset-x", 0);
        config.set("structures.types.nation_core.marker-offset-y", 1);
        config.set("structures.types.nation_core.marker-offset-z", 0);

        StructureRegistry registry = StructureRegistry.from(config);

        assertTrue(registry.enabled());
        assertEquals("structures", registry.schematicDirectory());
        assertEquals(List.of("leeseolwar:capital_core"), registry.allowedCoreBlocks());
        assertEquals(1, registry.all().size());
        assertEquals(StructureCategory.NATION_CORE, registry.require("nation_core").category());
        assertEquals("leeseolwar:capital_core", registry.require("nation_core").itemsAdderBlock());
        assertEquals("nation_core.schem", registry.require("nation_core").schematic());
        assertEquals(1, registry.require("nation_core").markerOffsetY());
    }

    @Test
    public void doesNotBundleDefaultNationCoreSchematic() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("structures/nation_core.schem")) {
            assertNull(input);
        }
    }

    @Test
    public void keepsStructureRegisteredWhenSchematicIsUnlinked() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("structures.enabled", true);
        config.set("structures.types.nation_core.name", "&bNation Core");
        config.set("structures.types.nation_core.schematic", "");
        config.set("structures.types.nation_core.category", "nation-core");
        config.set("structures.types.nation_core.width", 15);
        config.set("structures.types.nation_core.height", 12);
        config.set("structures.types.nation_core.length", 15);

        StructureRegistry registry = StructureRegistry.from(config);

        assertTrue(registry.enabled());
        assertEquals(1, registry.all().size());
        assertEquals("", registry.require("nation_core").schematic());
        assertTrue(registry.require("nation_core").nationCore());
    }

    @Test
    public void unlinkedSchematicDoesNotTouchPluginDataFolder() {
        StructureDefinition definition = new StructureDefinition(
                "nation_core",
                "Nation Core",
                "BEACON",
                "",
                StructureCategory.NATION_CORE,
                "",
                15,
                12,
                15,
                -7,
                0,
                -7
        );
        WorldEditStructurePaster paster = new WorldEditStructurePaster(null);

        assertFalse(paster.schematicExists(definition));
    }
}
