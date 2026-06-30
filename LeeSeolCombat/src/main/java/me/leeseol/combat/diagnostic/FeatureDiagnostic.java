package me.leeseol.combat.diagnostic;

public record FeatureDiagnostic(DiagnosticStatus status, String feature, String message) {
    public String line() {
        return "[" + status.name() + "] " + feature + " - " + message;
    }
}
