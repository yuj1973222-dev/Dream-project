package me.leeseol.combat.diagnostic;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public final class FeatureDiagnosticTest {
    @Test
    public void formatsStableMachineReadableLines() {
        FeatureDiagnostic result = new FeatureDiagnostic(
                DiagnosticStatus.FAIL,
                "logout-clone",
                "Citizens is not enabled"
        );

        assertEquals("[FAIL] logout-clone - Citizens is not enabled", result.line());
    }

    @Test
    public void summarizesHighestSeverity() {
        DiagnosticReport report = new DiagnosticReport(List.of(
                new FeatureDiagnostic(DiagnosticStatus.OK, "logout-clone", "enabled"),
                new FeatureDiagnostic(DiagnosticStatus.FIXED, "config", "reloaded")
        ));

        assertEquals(DiagnosticStatus.FIXED, report.overallStatus());
        assertEquals("[FIXED] summary - ok=1 warn=0 fail=0 fixed=1", report.summaryLine());
    }
}
