package me.leeseol.town.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

public final class NationColorPaletteTest {
    @Test
    public void resolvesConfiguredNamedColor() {
        NationColorPalette palette = NationColorPalette.from(new YamlConfiguration());

        NationColor color = palette.resolve("red", Set.of());

        assertEquals("red", color.key());
        assertEquals("#FF5C7A", color.hex());
    }

    @Test
    public void migratesLegacyTypeToPreferredColorWithoutDuplicates() {
        NationColorPalette palette = NationColorPalette.from(new YamlConfiguration());
        Set<String> usedKeys = new LinkedHashSet<>();

        NationColor first = palette.resolveLegacy("republic", "alpha", usedKeys);
        usedKeys.add(first.key());
        NationColor second = palette.resolveLegacy("republic", "beta", usedKeys);

        assertEquals("blue", first.key());
        assertNotEquals(first.key(), second.key());
        assertFalse(second.hex().isBlank());
    }

    @Test
    public void fallsBackToDeterministicCustomColorWhenPaletteIsFull() {
        NationColorPalette palette = NationColorPalette.from(new YamlConfiguration());
        Set<String> usedKeys = new LinkedHashSet<>(palette.keys());

        NationColor color = palette.resolveLegacy(null, "seventh", usedKeys);

        assertEquals("custom:seventh", color.key());
        assertFalse(color.hex().isBlank());
    }
}
