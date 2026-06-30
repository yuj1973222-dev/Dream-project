package me.leeseol.core.status;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import org.junit.Test;

public final class ServerStatusServiceTest {
    @Test
    public void formatsUptimeWithHoursMinutesAndSeconds() {
        assertEquals("1h 2m 3s", ServerStatusService.formatDuration(Duration.ofSeconds(3723)));
    }

    @Test
    public void formatsUptimeWithoutZeroHourPrefix() {
        assertEquals("2m 5s", ServerStatusService.formatDuration(Duration.ofSeconds(125)));
        assertEquals("9s", ServerStatusService.formatDuration(Duration.ofSeconds(9)));
    }
}
