package me.leeseol.lobby.limbo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;

public final class LimboCommandPolicyTest {
    @Test
    public void allowsConfiguredLobbyExitCommands() {
        LimboCommandPolicy policy = new LimboCommandPolicy(Set.of("lobby", "로비"));

        assertTrue(policy.isAllowed("/lobby"));
        assertTrue(policy.isAllowed("/LOBBY now"));
        assertTrue(policy.isAllowed("/로비"));
    }

    @Test
    public void blocksOtherCommandsWhileWaiting() {
        LimboCommandPolicy policy = new LimboCommandPolicy(Set.of("lobby", "로비"));

        assertFalse(policy.isAllowed("/spawn"));
        assertFalse(policy.isAllowed("/msg player hello"));
        assertFalse(policy.isAllowed("lobby"));
    }
}
