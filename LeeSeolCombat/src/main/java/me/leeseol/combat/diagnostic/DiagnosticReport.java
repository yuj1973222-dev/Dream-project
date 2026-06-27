package me.leeseol.combat.diagnostic;

import java.util.List;

public record DiagnosticReport(List<FeatureDiagnostic> results) {
    public DiagnosticStatus overallStatus() {
        DiagnosticStatus overall = DiagnosticStatus.OK;
        for (FeatureDiagnostic result : results) {
            if (result.status().severity() > overall.severity()) {
                overall = result.status();
            }
        }
        return overall;
    }

    public String summaryLine() {
        return new FeatureDiagnostic(
                overallStatus(),
                "summary",
                "ok=" + count(DiagnosticStatus.OK)
                        + " warn=" + count(DiagnosticStatus.WARN)
                        + " fail=" + count(DiagnosticStatus.FAIL)
                        + " fixed=" + count(DiagnosticStatus.FIXED)
        ).line();
    }

    private long count(DiagnosticStatus status) {
        return results.stream().filter(result -> result.status() == status).count();
    }
}
