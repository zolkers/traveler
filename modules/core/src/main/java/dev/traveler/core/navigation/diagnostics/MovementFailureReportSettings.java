package dev.traveler.core.navigation.diagnostics;

public record MovementFailureReportSettings(int scanSize) {
    public MovementFailureReportSettings {
        if (scanSize <= 0) {
            throw new IllegalArgumentException("scanSize must be positive.");
        }
    }

    public static MovementFailureReportSettings standard() {
        return new MovementFailureReportSettings(16);
    }
}
