package me.leeseol.town.diagnostic;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public final class FeatureDiagnosticTest {
    @Test
    public void formatsStableMachineReadableLines() {
        FeatureDiagnostic result = new FeatureDiagnostic(
                DiagnosticStatus.FAIL,
                "scoreboard",
                "task is not running"
        );

        assertEquals("[FAIL] scoreboard - task is not running", result.line());
    }

    @Test
    public void summarizesHighestSeverity() {
        DiagnosticReport report = new DiagnosticReport(List.of(
                new FeatureDiagnostic(DiagnosticStatus.OK, "neutral-zone", "loaded zones=2"),
                new FeatureDiagnostic(DiagnosticStatus.WARN, "scoreboard", "no online players")
        ));

        assertEquals(DiagnosticStatus.WARN, report.overallStatus());
        assertEquals("[WARN] summary - ok=1 warn=1 fail=0 fixed=0", report.summaryLine());
    }
}
