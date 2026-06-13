package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;
import java.util.Objects;

final class MovementVectorSelector {
    MovementDirective select(
            NavigationPoint current,
            SteeringPlan steering,
            MovementInputSettings settings) {
        NavigationPoint position = Objects.requireNonNull(current, "current");
        SteeringPlan plan = Objects.requireNonNull(steering, "steering");
        MovementInputSettings inputSettings = Objects.requireNonNull(settings, "settings");
        if (shouldRecenter(plan, inputSettings)) {
            return new MovementDirective(plan.lateralCorrection(), false);
        }
        if (!plan.tangent().isZero()) {
            return new MovementDirective(plan.desiredVectorFrom(position), true);
        }
        return new MovementDirective(position.horizontalVectorTo(plan.steeringTarget()), true);
    }

    private static boolean shouldRecenter(
            SteeringPlan steering,
            MovementInputSettings settings) {
        return steering.outsideCorridor()
                && steering.lateralCorrection().length() >= settings.centeringCorrectionThreshold();
    }
}
