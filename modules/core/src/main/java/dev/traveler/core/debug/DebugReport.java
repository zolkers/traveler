package dev.traveler.core.debug;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.locomotion.LocomotionAction;
import dev.traveler.core.navigation.plan.NavigationPhase;
import dev.traveler.core.path.PathfinderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DebugReport(
        Optional<NavigationDebugSnapshot> navigation,
        Optional<PathfinderDebugSnapshot> path,
        Instant generatedAt,
        List<DebugAnomaly> anomalies) {
    private static final Duration STALE_NAVIGATION_AFTER = Duration.ofSeconds(2);
    private static final double CAMERA_YAW_LAG_THRESHOLD = 90.0;
    private static final double BEHIND_TARGET_DOT_THRESHOLD = -0.1;
    private static final double LOW_SPEED_TARGET_DISTANCE = 3.0;
    private static final double LOW_SPEED_SCALE = 0.5;

    public DebugReport {
        navigation = Objects.requireNonNull(navigation, "navigation");
        path = Objects.requireNonNull(path, "path");
        Objects.requireNonNull(generatedAt, "generatedAt");
        anomalies = List.copyOf(Objects.requireNonNull(anomalies, "anomalies"));
    }

    public static DebugReport from(
            Optional<NavigationDebugSnapshot> navigation,
            Optional<PathfinderDebugSnapshot> path,
            Instant generatedAt) {
        Optional<NavigationDebugSnapshot> navigationSnapshot = Objects.requireNonNull(navigation, "navigation");
        Optional<PathfinderDebugSnapshot> pathSnapshot = Objects.requireNonNull(path, "path");
        Instant now = Objects.requireNonNull(generatedAt, "generatedAt");
        return new DebugReport(
                navigationSnapshot,
                pathSnapshot,
                now,
                anomaliesFor(navigationSnapshot, pathSnapshot, now));
    }

    public boolean hasAnomalies() {
        return !anomalies.isEmpty();
    }

    private static List<DebugAnomaly> anomaliesFor(
            Optional<NavigationDebugSnapshot> navigation,
            Optional<PathfinderDebugSnapshot> path,
            Instant generatedAt) {
        List<DebugAnomaly> anomalies = new ArrayList<>();
        addPathAnomalies(anomalies, path);
        addNavigationAnomalies(anomalies, navigation, generatedAt);
        return anomalies.stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
    }

    private static void addPathAnomalies(
            List<DebugAnomaly> anomalies,
            Optional<PathfinderDebugSnapshot> path) {
        if (path.isEmpty()) {
            anomalies.add(DebugAnomaly.NO_PATH);
            return;
        }
        if (path.orElseThrow().result().status() != PathfinderStatus.FOUND) {
            anomalies.add(DebugAnomaly.PATH_NOT_FOUND);
        }
    }

    private static void addNavigationAnomalies(
            List<DebugAnomaly> anomalies,
            Optional<NavigationDebugSnapshot> navigation,
            Instant generatedAt) {
        if (navigation.isEmpty()) {
            anomalies.add(DebugAnomaly.NO_NAVIGATION);
            return;
        }
        NavigationDebugSnapshot snapshot = navigation.orElseThrow();
        addStaleNavigationAnomaly(anomalies, snapshot, generatedAt);
        addActiveNavigationAnomalies(anomalies, snapshot);
    }

    private static void addStaleNavigationAnomaly(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot,
            Instant generatedAt) {
        Duration age = Duration.between(snapshot.updatedAt(), generatedAt);
        if (!age.isNegative() && age.compareTo(STALE_NAVIGATION_AFTER) > 0) {
            anomalies.add(DebugAnomaly.STALE_NAVIGATION_DEBUG);
        }
    }

    private static void addActiveNavigationAnomalies(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot) {
        if (snapshot.completed() || snapshot.phase() == NavigationPhase.ARRIVE) {
            return;
        }
        addMovementAnomalies(anomalies, snapshot);
        addCameraAnomalies(anomalies, snapshot);
        addActionAnomalies(anomalies, snapshot);
        addSpeedAnomalies(anomalies, snapshot);
    }

    private static void addMovementAnomalies(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot) {
        if (!snapshot.intent().moving()) {
            anomalies.add(DebugAnomaly.NO_MOVEMENT_KEYS_WHILE_ACTIVE);
        }
        if (snapshot.movementVector().isZero()) {
            anomalies.add(DebugAnomaly.ZERO_VECTOR_WHILE_NOT_COMPLETED);
            return;
        }
        if (targetDot(snapshot) < BEHIND_TARGET_DOT_THRESHOLD) {
            anomalies.add(DebugAnomaly.TARGET_BEHIND_PLAYER);
        }
    }

    private static void addCameraAnomalies(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot) {
        if (yawLag(snapshot) >= CAMERA_YAW_LAG_THRESHOLD) {
            anomalies.add(DebugAnomaly.CAMERA_YAW_LAG_HIGH);
        }
    }

    private static void addActionAnomalies(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot) {
        if (snapshot.actionIntent().jumpRequested() && !snapshot.intent().jump()) {
            anomalies.add(DebugAnomaly.JUMP_REQUESTED_WITHOUT_SPACE);
        }
        if (specialActionBlocked(snapshot)) {
            anomalies.add(DebugAnomaly.SPECIAL_ACTION_BLOCKED);
        }
    }

    private static void addSpeedAnomalies(
            List<DebugAnomaly> anomalies,
            NavigationDebugSnapshot snapshot) {
        double distance = snapshot.agentPosition().distanceTo(snapshot.movementTarget());
        if (distance > LOW_SPEED_TARGET_DISTANCE && snapshot.speedIntent().scale() < LOW_SPEED_SCALE) {
            anomalies.add(DebugAnomaly.LOW_SPEED_FAR_FROM_TARGET);
        }
    }

    private static boolean specialActionBlocked(NavigationDebugSnapshot snapshot) {
        return snapshot.actionIntent().action() != LocomotionAction.WALK
                && snapshot.phase() == NavigationPhase.ALIGN;
    }

    private static double targetDot(NavigationDebugSnapshot snapshot) {
        return snapshot.movementVector()
                .normalized()
                .dot(snapshot.agentPosition().horizontalVectorTo(snapshot.movementTarget()).normalized());
    }

    static double yawLag(NavigationDebugSnapshot snapshot) {
        return Math.abs(CameraAngles.shortestYawDelta(
                snapshot.currentCamera().yawDegrees(),
                snapshot.cameraTarget().yawDegrees()));
    }
}
