package dev.traveler.core.navigation.locomotion;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public final class JumpTraversalRules {
    public static final double LANDING_HEIGHT_TOLERANCE = 0.2;
    public static final double LANDING_DISTANCE_TOLERANCE = 1.0;
    public static final double TAKEOFF_EXIT_DISTANCE = 0.35;
    public static final double TAKEOFF_EXIT_HEIGHT = 0.35;

    private JumpTraversalRules() {}

    public static boolean hasReachedLandingHeight(NavigationPoint position, NavigationPoint target) {
        NavigationPoint current = Objects.requireNonNull(position, "position");
        NavigationPoint landing = Objects.requireNonNull(target, "target");
        return current.y() >= landing.y() - LANDING_HEIGHT_TOLERANCE;
    }

    public static boolean hasLeftTakeoffZone(NavigationPoint position, NavigationPoint segmentStart) {
        NavigationPoint current = Objects.requireNonNull(position, "position");
        NavigationPoint takeoff = Objects.requireNonNull(segmentStart, "segmentStart");
        return current.horizontalDistanceTo(takeoff) >= TAKEOFF_EXIT_DISTANCE
                || current.y() >= takeoff.y() + TAKEOFF_EXIT_HEIGHT;
    }

    public static boolean hasLeftTakeoffZone(
            NavigationPoint position,
            NavigationPoint segmentStart,
            NavigationPoint actionTarget) {
        NavigationPoint current = Objects.requireNonNull(position, "position");
        NavigationPoint takeoff = Objects.requireNonNull(segmentStart, "segmentStart");
        NavigationPoint target = Objects.requireNonNull(actionTarget, "actionTarget");
        double verticalExit = takeoff.y() + TAKEOFF_EXIT_HEIGHT;
        if (current.y() >= verticalExit) {
            return true;
        }
        dev.traveler.core.navigation.spatial.HorizontalVector axis =
                takeoff.horizontalVectorTo(target);
        if (axis.isZero()) {
            return current.horizontalDistanceTo(takeoff) >= TAKEOFF_EXIT_DISTANCE;
        }
        double along = takeoff.horizontalVectorTo(current).dot(axis.normalized());
        return along >= TAKEOFF_EXIT_DISTANCE;
    }
}
