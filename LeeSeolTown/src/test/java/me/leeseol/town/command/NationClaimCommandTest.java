package me.leeseol.town.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class NationClaimCommandTest {
    @Test
    public void parsesNationClaimAliases() {
        assertEquals(NationClaimCommand.CLAIM, NationClaimCommand.parse("claim"));
        assertEquals(NationClaimCommand.CLAIM, NationClaimCommand.parse("buy"));
        assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("claimprice"));
        assertEquals(NationClaimCommand.PRICE, NationClaimCommand.parse("price"));
        assertEquals(NationClaimCommand.UNCLAIM, NationClaimCommand.parse("unclaim"));
    }

    @Test
    public void ignoresOtherNationSubcommands() {
        assertNull(NationClaimCommand.parse("create"));
        assertNull(NationClaimCommand.parse("treasury"));
        assertNull(NationClaimCommand.parse(null));
    }
}
