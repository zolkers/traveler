package dev.traveler.core.route.longdistance;

import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.settings.TravelerSettings;
import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public final class LongDistanceRoutePlanner {
    private final LongDistanceRouteSettings settings;

    public LongDistanceRoutePlanner(LongDistanceRouteSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public static LongDistanceRoutePlanner standard() {
        return new LongDistanceRoutePlanner(TravelerSettings.standard().longDistanceRouteSettings());
    }

    public LongDistanceRoutePlan plan(BlockPosition start, RouteGoal goal) {
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        RouteGoal safeGoal = Objects.requireNonNull(goal, "goal");
        BlockPosition finalPosition = safeGoal.preferredPosition(safeStart);
        int deltaX = finalPosition.x() - safeStart.x();
        int deltaZ = finalPosition.z() - safeStart.z();
        double horizontalDistance = Math.hypot(deltaX, deltaZ);
        int maxAxisDistance = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (horizontalDistance <= settings.directHorizontalDistance()
                && maxAxisDistance <= settings.maxSegmentAxisDelta()) {
            return new LongDistanceRoutePlan(safeGoal, safeGoal, true, horizontalDistance);
        }
        RouteGoal activeGoal = intermediateGoal(safeStart, deltaX, deltaZ, horizontalDistance, maxAxisDistance);
        return new LongDistanceRoutePlan(safeGoal, activeGoal, false, horizontalDistance);
    }

    private RouteGoal intermediateGoal(
            BlockPosition start,
            int deltaX,
            int deltaZ,
            double horizontalDistance,
            int maxAxisDistance) {
        if (horizontalDistance <= 0.0 || maxAxisDistance == 0) {
            return RouteGoal.xz(start.x(), start.z());
        }
        double ratio = Math.min(
                settings.segmentHorizontalDistance() / horizontalDistance,
                (double) settings.maxSegmentAxisDelta() / maxAxisDistance);
        ratio = Math.clamp(ratio, 0.0, 1.0);
        int x = start.x() + roundedStep(deltaX, ratio);
        int z = start.z() + roundedStep(deltaZ, ratio);
        if (x == start.x() && z == start.z()) {
            if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
                x += Integer.signum(deltaX);
            } else {
                z += Integer.signum(deltaZ);
            }
        }
        return RouteGoal.xz(x, z);
    }

    private static int roundedStep(int delta, double ratio) {
        if (delta == 0) {
            return 0;
        }
        int step = (int) Math.round(delta * ratio);
        if (step == 0) {
            return Integer.signum(delta);
        }
        return step;
    }
}
