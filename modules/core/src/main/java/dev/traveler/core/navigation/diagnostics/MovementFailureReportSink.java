package dev.traveler.core.navigation.diagnostics;

@FunctionalInterface
public interface MovementFailureReportSink {
    void write(MovementFailureReport report);
}
