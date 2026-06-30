package me.leeseol.cleanup.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CleanupCountdownTest {
    @Test
    public void showsCountdownOnlyDuringFinalWarningWindow() {
        assertFalse(CleanupCountdown.shouldShow(11L, 10));
        assertTrue(CleanupCountdown.shouldShow(10L, 10));
        assertTrue(CleanupCountdown.shouldShow(1L, 10));
        assertFalse(CleanupCountdown.shouldShow(0L, 10));
    }

    @Test
    public void playsStartSoundOnceAtCountdownStart() {
        assertTrue(CleanupCountdown.shouldPlayStartSound(10L, 10, false));
        assertFalse(CleanupCountdown.shouldPlayStartSound(10L, 10, true));
        assertFalse(CleanupCountdown.shouldPlayStartSound(9L, 10, false));
    }

    @Test
    public void rendersSecondsIntoActionBarTemplate() {
        assertEquals("&c곧 아이템이 삭제됩니다: &e7초", CleanupCountdown.render("&c곧 아이템이 삭제됩니다: &e%seconds%초", 7L));
    }
}
