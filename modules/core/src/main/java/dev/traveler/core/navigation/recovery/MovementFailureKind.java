package dev.traveler.core.navigation.recovery;

public enum MovementFailureKind {
    STUCK_NO_PROGRESS,
    PATH_DIVERGENCE,
    ACTION_SETUP_TIMEOUT,
    OFF_ROUTE_BUT_RECOVERABLE
}
