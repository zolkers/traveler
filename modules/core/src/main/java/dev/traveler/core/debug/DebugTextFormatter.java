package dev.traveler.core.debug;

import dev.traveler.core.navigation.control.MovementIntent;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DebugTextFormatter {
    private DebugTextFormatter() {}

    public static String navigationSummary(NavigationDebugSnapshot snapshot) {
        NavigationDebugSnapshot debug = Objects.requireNonNull(snapshot, "snapshot");
        return String.format(
                Locale.ROOT,
                "nav phase=%s action=%s mode=%s progress=%d target=%s vector=%s keys=%s yaw=%.1f->%.1f outYaw=%.1f speed=%.2f completed=%s",
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
