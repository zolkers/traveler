package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.steering.SteeringPlan;
import java.util.Objects;

public record NavigationSteeringDebug(
        double lateralError,
        boolean outsideCorridor,
        boolean clearanceWarning) {
    private static final NavigationSteeringDebug NONE = new NavigationSteeringDebug(0.0, false, false);

    public NavigationSteeringDebug {
        if (!Double.isFinite(lateralError) || lateralError < 0.0) {
            throw new IllegalArgumentException("lateralError must be non-negative.");
        }
    }

    public static NavigationSteeringDebug none() {
        return NONE;
    }

    public static NavigationSteeringDebug from(SteeringPlan steeringPlan) {
        SteeringPlan steering = Objects.requireNonNull(steeringPlan, "steeringPlan");
        return new NavigationSteeringDebug(
                steering.lateralError(),
                steering.outsideCorridor(),
                steering.clearanceWarning());
    }
}
