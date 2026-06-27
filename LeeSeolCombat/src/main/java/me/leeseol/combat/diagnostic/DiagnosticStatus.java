package me.leeseol.combat.diagnostic;

public enum DiagnosticStatus {
    OK(0),
    WARN(1),
    FIXED(2),
    FAIL(3);

    private final int severity;

    DiagnosticStatus(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }
}
