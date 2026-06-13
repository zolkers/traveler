package dev.traveler.core.route;

import java.util.Objects;

public record RouteSearchDiagnostics(
        RouteSearchFailureReason reason,
        int startSurfaceCount,
        int goalSurfaceCount) {
    public RouteSearchDiagnostics {
        Objects.requireNonNull(reason, "reason");
        requireNonNegative(startSurfaceCount, "startSurfaceCount");
        requireNonNegative(goalSurfaceCount, "goalSurfaceCount");
    }

    public static RouteSearchDiagnostics none(int startSurfaceCount, int goalSurfaceCount) {
        return new RouteSearchDiagnostics(
                RouteSearchFailureReason.NONE,
                startSurfaceCount,
                goalSurfaceCount);
    }

    public static RouteSearchDiagnostics worldUnavailable() {
        return new RouteSearchDiagnostics(RouteSearchFailureReason.WORLD_UNAVAILABLE, 0, 0);
    }

    public static RouteSearchDiagnostics noStartSurface(int goalSurfaceCount) {
        return new RouteSearchDiagnostics(
                RouteSearchFailureReason.NO_START_SURFACE,
                0,
                goalSurfaceCount);
    }

    public static RouteSearchDiagnostics noGoalSurface(int startSurfaceCount) {
        return new RouteSearchDiagnostics(
                RouteSearchFailureReason.NO_GOAL_SURFACE,
                startSurfaceCount,
                0);
    }

    public static RouteSearchDiagnostics surfaceNotFound(int startSurfaceCount, int goalSurfaceCount) {
        return new RouteSearchDiagnostics(
                RouteSearchFailureReason.SURFACE_NOT_FOUND,
                startSurfaceCount,
                goalSurfaceCount);
    }

    public static RouteSearchDiagnostics blockNotFound() {
        return new RouteSearchDiagnostics(RouteSearchFailureReason.BLOCK_NOT_FOUND, 0, 0);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}
