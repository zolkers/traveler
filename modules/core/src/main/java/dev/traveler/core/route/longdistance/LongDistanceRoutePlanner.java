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
        RouteGoal activeGoal = new FrontierRouteGoal(finalPosition, settings);
        return new LongDistanceRoutePlan(safeGoal, activeGoal, false, horizontalDistance);
    }
}
