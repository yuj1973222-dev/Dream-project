package me.leeseol.town.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class WarModeTest {
    @Test
    public void parsesOperationalWarModesAndKoreanAliases() {
        assertEquals(WarMode.INVASION, WarMode.parse("invasion"));
        assertEquals(WarMode.INVASION, WarMode.parse("INVASION"));
        assertEquals(WarMode.INVASION, WarMode.parse("\uCE68\uACF5"));
        assertEquals("\uCE68\uACF5", WarMode.INVASION.displayName());

        assertEquals(WarMode.TOTAL, WarMode.parse("total"));
        assertEquals(WarMode.TOTAL, WarMode.parse("TOTAL"));
        assertEquals(WarMode.TOTAL, WarMode.parse("\uCD1D\uB825\uC804"));
        assertEquals("\uCD1D\uB825\uC804", WarMode.TOTAL.displayName());
    }

    @Test
    public void defaultsUnknownLegacyModeToInvasion() {
        assertEquals(WarMode.INVASION, WarMode.parseOrDefault("skirmish"));
        assertEquals(WarMode.INVASION, WarMode.parseOrDefault(null));
    }
}
