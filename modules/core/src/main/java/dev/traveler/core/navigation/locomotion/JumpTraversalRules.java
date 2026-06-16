package dev.traveler.core.navigation.locomotion;

import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public final class JumpTraversalRules {
    public static final double LANDING_HEIGHT_TOLERANCE = 0.2;
    public static final double LANDING_DISTANCE_TOLERANCE = 1.0;
    public static final double TAKEOFF_EXIT_DISTANCE = 0.35;
    public static final double TAKEOFF_EXIT_HEIGHT = 0.35;

    private JumpTraversalRules() {}

    public static boolean hasReachedLandingHeight(WorldPoint position, WorldPoint target) {
        WorldPoint current = Objects.requireNonNull(position, "position");
        WorldPoint landing = Objects.requireNonNull(target, "target");
        return current.y() >= landing.y() - LANDING_HEIGHT_TOLERANCE;
    }

    public static boolean hasLeftTakeoffZone(WorldPoint position, WorldPoint segmentStart) {
        WorldPoint current = Objects.requireNonNull(position, "position");
        WorldPoint takeoff = Objects.requireNonNull(segmentStart, "segmentStart");
        return current.horizontalDistanceTo(takeoff) >= TAKEOFF_EXIT_DISTANCE
                || current.y() >= takeoff.y() + TAKEOFF_EXIT_HEIGHT;
    }

    public static boolean hasLeftTakeoffZone(
            WorldPoint position,
            WorldPoint segmentStart,
            WorldPoint actionTarget) {
        WorldPoint current = Objects.requireNonNull(position, "position");
        WorldPoint takeoff = Objects.requireNonNull(segmentStart, "segmentStart");
        WorldPoint target = Objects.requireNonNull(actionTarget, "actionTarget");
        double verticalExit = takeoff.y() + TAKEOFF_EXIT_HEIGHT;
        if (current.y() >= verticalExit) {
            return true;
        }
        dev.traveler.core.common.geometry.HorizontalVector axis =
                takeoff.horizontalVectorTo(target);
        if (axis.isZero()) {
            return current.horizontalDistanceTo(takeoff) >= TAKEOFF_EXIT_DISTANCE;
        }
        double along = takeoff.horizontalVectorTo(current).dot(axis.normalized());
        return along >= TAKEOFF_EXIT_DISTANCE;
    }
}
