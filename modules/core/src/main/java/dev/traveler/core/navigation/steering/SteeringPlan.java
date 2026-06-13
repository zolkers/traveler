package dev.traveler.core.navigation.steering;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.Objects;

public record SteeringPlan(
        NavigationPoint steeringTarget,
        NavigationPoint pathTarget,
        NavigationPoint nearestPoint,
        HorizontalVector tangent,
        HorizontalVector lateralCorrection,
        double lateralError,
        double distanceOnPath,
        boolean outsideCorridor,
        boolean clearanceWarning) {
    public SteeringPlan {
        Objects.requireNonNull(steeringTarget, "steeringTarget");
        Objects.requireNonNull(pathTarget, "pathTarget");
        Objects.requireNonNull(nearestPoint, "nearestPoint");
        Objects.requireNonNull(tangent, "tangent");
        Objects.requireNonNull(lateralCorrection, "lateralCorrection");
        requireNonNegative(lateralError, "lateralError");
        requireNonNegative(distanceOnPath, "distanceOnPath");
    }

    public static SteeringPlan seek(NavigationPoint target) {
        NavigationPoint point = Objects.requireNonNull(target, "target");
        HorizontalVector zero = new HorizontalVector(0.0, 0.0);
        return new SteeringPlan(point, point, point, zero, zero, 0.0, 0.0, false, false);
    }

    public static SteeringPlan corridor(
            NavigationPoint steeringTarget,
            NavigationPoint pathTarget,
            NavigationPoint nearestPoint,
            HorizontalVector tangent,
            HorizontalVector lateralCorrection,
            double lateralError,
            double distanceOnPath,
            boolean outsideCorridor) {
        return corridor(
                steeringTarget,
                pathTarget,
                nearestPoint,
                tangent,
                lateralCorrection,
                lateralError,
                distanceOnPath,
                outsideCorridor,
                false);
    }

    public static SteeringPlan corridor(
            NavigationPoint steeringTarget,
            NavigationPoint pathTarget,
            NavigationPoint nearestPoint,
            HorizontalVector tangent,
            HorizontalVector lateralCorrection,
            double lateralError,
            double distanceOnPath,
            boolean outsideCorridor,
            boolean clearanceWarning) {
        return new SteeringPlan(
                steeringTarget,
                pathTarget,
                nearestPoint,
                tangent,
                lateralCorrection,
                lateralError,
                distanceOnPath,
                outsideCorridor,
                clearanceWarning);
    }

    public HorizontalVector desiredVectorFrom(NavigationPoint position) {
        NavigationPoint current = Objects.requireNonNull(position, "position");
        HorizontalVector targetVector = current.horizontalVectorTo(steeringTarget);
        if (tangent.isZero()) {
            return targetVector;
        }
        HorizontalVector desired = tangent.normalized().plus(lateralCorrection).plus(targetVector.normalized());
        if (desired.isZero()) {
            return targetVector;
        }
        return desired;
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}
