package dev.traveler.core.navigation.input;

import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;

enum StrafingStrategy {
    DIRECT {
        @Override
        HorizontalVector desiredVector(NavigationPoint current, SteeringPlan steering) {
            return current.horizontalVectorTo(steering.steeringTarget());
        }
    },
    CORRIDOR {
        @Override
        HorizontalVector desiredVector(NavigationPoint current, SteeringPlan steering) {
            return steering.desiredVectorFrom(current);
        }
    },
    CENTERING {
        @Override
        HorizontalVector desiredVector(NavigationPoint current, SteeringPlan steering) {
            return steering.lateralCorrection();
        }

        @Override
        boolean allowsSpecialAction() {
            return false;
        }
    };

    abstract HorizontalVector desiredVector(NavigationPoint current, SteeringPlan steering);

    boolean allowsSpecialAction() {
        return true;
    }

    static StrafingStrategy select(SteeringPlan steering, double centeringCorrectionThreshold) {
        if (shouldCenter(steering, centeringCorrectionThreshold)) {
            return CENTERING;
        }
        if (!steering.tangent().isZero()) {
            return CORRIDOR;
        }
        return DIRECT;
    }

    private static boolean shouldCenter(SteeringPlan steering, double centeringCorrectionThreshold) {
        return steering.outsideCorridor()
                && steering.lateralCorrection().length() >= centeringCorrectionThreshold;
    }
}
