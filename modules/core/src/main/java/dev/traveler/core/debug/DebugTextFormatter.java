package dev.traveler.core.debug;

import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.path.PathfinderStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DebugTextFormatter {
    private DebugTextFormatter() {}

    public static String detailedStatus(
            Optional<NavigationDebugSnapshot> navigation,
            Optional<PathfinderDebugSnapshot> path,
            Instant generatedAt) {
        DebugReport report = DebugReport.from(navigation, path, generatedAt);
        List<String> lines = new ArrayList<>();
        lines.add("traveler debug");
        lines.add(pathLine(report.path()));
        addNavigationLines(lines, report.navigation());
        lines.add(anomaliesLine(report));
        return String.join("\n", lines);
    }

    public static String navigationSummary(NavigationDebugSnapshot snapshot) {
        NavigationDebugSnapshot debug = Objects.requireNonNull(snapshot, "snapshot");
        return String.format(
                Locale.ROOT,
                "nav phase=%s action=%s mode=%s progress=%d target=%s vector=%s "
                        + "keys=%s yaw=%.1f->%.1f outYaw=%.1f speed=%.2f completed=%s",
                debug.phase(),
                debug.actionIntent().action(),
                debug.movementMode(),
                debug.routeProgress().nextNodeIndex(),
                point(debug.movementTarget()),
                vector(debug.movementVector()),
                keys(debug.intent()),
                debug.currentCamera().yawDegrees(),
                debug.cameraTarget().yawDegrees(),
                debug.outputCamera().yawDegrees(),
                debug.speedIntent().scale(),
                debug.completed());
    }

    public static String pathSummary(PathfinderDebugSnapshot snapshot) {
        PathfinderDebugSnapshot debug = Objects.requireNonNull(snapshot, "snapshot");
        PathfinderStatus status = debug.result().status();
        int nodes = debug.result().path().nodeCount();
        double cost = debug.result().path().cost();
        String surface = debug.hasSurfaceNodes() ? " surfaceNodes=" + debug.surfaceNodes().size() : "";
        return String.format(Locale.ROOT, "path status=%s nodes=%d cost=%.2f%s", status, nodes, cost, surface);
    }

    private static String pathLine(Optional<PathfinderDebugSnapshot> snapshot) {
        Optional<PathfinderDebugSnapshot> current = Objects.requireNonNull(snapshot, "snapshot");
        return current.map(DebugTextFormatter::pathSummary).orElse("path=none");
    }

    private static void addNavigationLines(
            List<String> lines,
            Optional<NavigationDebugSnapshot> snapshot) {
        Optional<NavigationDebugSnapshot> current = Objects.requireNonNull(snapshot, "snapshot");
        if (current.isEmpty()) {
            lines.add("nav=none");
            return;
        }
        NavigationDebugSnapshot debug = current.orElseThrow();
        lines.add(navigationStateLine(debug));
        lines.add(navigationPositionLine(debug));
        lines.add(navigationVectorLine(debug));
        lines.add(navigationInputLine(debug));
        lines.add(navigationCameraLine(debug));
    }

    private static String navigationStateLine(NavigationDebugSnapshot debug) {
        return String.format(
                Locale.ROOT,
                "nav phase=%s action=%s mode=%s progress=%d",
                debug.phase(),
                debug.actionIntent().action(),
                debug.movementMode(),
                debug.routeProgress().nextNodeIndex());
    }

    private static String navigationPositionLine(NavigationDebugSnapshot debug) {
        return String.format(
                Locale.ROOT,
                "pos=%s target=%s targetDistance=%.2f",
                point(debug.agentPosition()),
                point(debug.movementTarget()),
                debug.agentPosition().distanceTo(debug.movementTarget()));
    }

    private static String navigationVectorLine(NavigationDebugSnapshot debug) {
        return String.format(
                Locale.ROOT,
                "vector=%s vectorLength=%.2f",
                vector(debug.movementVector()),
                debug.movementVector().length());
    }

    private static String navigationInputLine(NavigationDebugSnapshot debug) {
        return String.format(
                Locale.ROOT,
                "keys=%s speed=%.2f sprint=%s completed=%s",
                keys(debug.intent()),
                debug.speedIntent().scale(),
                debug.speedIntent().sprintRequested(),
                debug.completed());
    }

    private static String navigationCameraLine(NavigationDebugSnapshot debug) {
        return String.format(
                Locale.ROOT,
                "camera currentYaw=%.1f targetYaw=%.1f outputYaw=%.1f yawLag=%.1f",
                debug.currentCamera().yawDegrees(),
                debug.cameraTarget().yawDegrees(),
                debug.outputCamera().yawDegrees(),
                DebugReport.yawLag(debug));
    }

    private static String anomaliesLine(DebugReport report) {
        if (!report.hasAnomalies()) {
            return "anomalies=none";
        }
        String names = report.anomalies().stream().map(Enum::name).collect(Collectors.joining(","));
        return "anomalies=" + names;
    }

    public static String keys(MovementIntent intent) {
        MovementIntent movementIntent = Objects.requireNonNull(intent, "intent");
        List<String> keys = new ArrayList<>();
        addIf(keys, movementIntent.forward(), "Z");
        addIf(keys, movementIntent.back(), "S");
        addIf(keys, movementIntent.left(), "Q");
        addIf(keys, movementIntent.right(), "D");
        addIf(keys, movementIntent.jump(), "SPACE");
        addIf(keys, movementIntent.sprint(), "SPRINT");
        if (keys.isEmpty()) {
            return "-";
        }
        return String.join("+", keys);
    }

    private static void addIf(List<String> keys, boolean condition, String value) {
        if (condition) {
            keys.add(value);
        }
    }

    private static String point(NavigationPoint point) {
        return String.format(Locale.ROOT, "(%.2f,%.2f,%.2f)", point.x(), point.y(), point.z());
    }

    private static String vector(HorizontalVector vector) {
        return String.format(Locale.ROOT, "(%.2f,%.2f)", vector.x(), vector.z());
    }
}
