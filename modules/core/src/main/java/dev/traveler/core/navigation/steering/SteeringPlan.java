package dev.traveler.core.navigation.steering;

import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public record SteeringPlan(
        WorldPoint steeringTarget,
        WorldPoint pathTarget,
        WorldPoint nearestPoint,
        HorizontalVector tangent,
        HorizontalVector lateralCorrection,
        double lateralError,
        double signedLateralError,
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
        requireFinite(signedLateralError, "signedLateralError");
        requireNonNegative(distanceOnPath, "distanceOnPath");
    }

    public static SteeringPlan seek(WorldPoint target) {
        return seek(target, 0.0);
    }

    public static SteeringPlan seek(WorldPoint target, double lateralError) {
        WorldPoint point = Objects.requireNonNull(target, "target");
        HorizontalVector zero = new HorizontalVector(0.0, 0.0);
        return new SteeringPlan(point, point, point, zero, zero, lateralError, 0.0, 0.0, false, false);
    }

    public static SteeringPlan corridor(
            WorldPoint steeringTarget,
            WorldPoint pathTarget,
            WorldPoint nearestPoint,
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
                0.0,
                distanceOnPath,
                outsideCorridor,
                false);
    }

    public static SteeringPlan corridor(
            WorldPoint steeringTarget,
            WorldPoint pathTarget,
            WorldPoint nearestPoint,
            HorizontalVector tangent,
            HorizontalVector lateralCorrection,
            double lateralError,
            double signedLateralError,
            double distanceOnPath,
            boolean outsideCorridor) {
        return corridor(
                steeringTarget,
                pathTarget,
                nearestPoint,
                tangent,
                lateralCorrection,
                lateralError,
                signedLateralError,
                distanceOnPath,
                outsideCorridor,
                false);
    }

    public static SteeringPlan corridor(
            WorldPoint steeringTarget,
            WorldPoint pathTarget,
            WorldPoint nearestPoint,
            HorizontalVector tangent,
            HorizontalVector lateralCorrection,
            double lateralError,
            double distanceOnPath,
            boolean outsideCorridor,
            boolean clearanceWarning) {
        return corridor(
                steeringTarget,
                pathTarget,
                nearestPoint,
                tangent,
                lateralCorrection,
                lateralError,
                0.0,
                distanceOnPath,
                outsideCorridor,
                clearanceWarning);
    }

    public static SteeringPlan corridor(
            WorldPoint steeringTarget,
            WorldPoint pathTarget,
            WorldPoint nearestPoint,
            HorizontalVector tangent,
            HorizontalVector lateralCorrection,
            double lateralError,
            double signedLateralError,
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
                signedLateralError,
                distanceOnPath,
                outsideCorridor,
                clearanceWarning);
    }

    public HorizontalVector desiredVectorFrom(WorldPoint position) {
        WorldPoint current = Objects.requireNonNull(position, "position");
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

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
