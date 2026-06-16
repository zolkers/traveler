package dev.traveler.core.route.longdistance;

import dev.traveler.core.layer.WorldNavigationBudget;
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
        return plan(start, goal, settingsForConfiguredCap());
    }

    public LongDistanceRoutePlan plan(BlockPosition start, RouteGoal goal, WorldNavigationBudget budget) {
        return plan(start, goal, settingsFor(Objects.requireNonNull(budget, "budget")));
    }

    private LongDistanceRoutePlan plan(
            BlockPosition start,
            RouteGoal goal,
            LongDistanceRouteSettings effectiveSettings) {
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        RouteGoal safeGoal = Objects.requireNonNull(goal, "goal");
        BlockPosition finalPosition = safeGoal.preferredPosition(safeStart);
        int deltaX = finalPosition.x() - safeStart.x();
        int deltaZ = finalPosition.z() - safeStart.z();
        double horizontalDistance = Math.hypot(deltaX, deltaZ);
        int maxAxisDistance = Math.max(Math.abs(deltaX), Math.abs(deltaZ));
        if (horizontalDistance <= effectiveSettings.directHorizontalDistance()
                && maxAxisDistance <= effectiveSettings.maxSegmentAxisDelta()) {
            return new LongDistanceRoutePlan(
                    safeGoal,
                    safeGoal,
                    true,
                    horizontalDistance,
                    horizontalDistance,
                    effectiveSettings);
        }
        RouteGoal activeGoal = new FrontierRouteGoal(finalPosition, effectiveSettings);
        return new LongDistanceRoutePlan(
                safeGoal,
                activeGoal,
                false,
                horizontalDistance,
                horizontalDistance(safeStart, activeGoal.preferredPosition(safeStart)),
                effectiveSettings);
    }

    private LongDistanceRouteSettings settingsFor(WorldNavigationBudget budget) {
        int budgetedAxisDelta = budget.visibleHorizontalRadiusBlocks()
                - settings.visibilityEdgeSafetyBlocks()
                - settings.frontierCaptureHorizontalMargin();
        int axisDelta = constrainedAxisDelta(budgetedAxisDelta);
        return settings.withMaxSegmentAxisDelta(axisDelta);
    }

    private LongDistanceRouteSettings settingsForConfiguredCap() {
        int axisDelta = constrainedAxisDelta(settings.maxSegmentAxisDelta());
        return settings.withMaxSegmentAxisDelta(axisDelta);
    }

    private int constrainedAxisDelta(int candidateAxisDelta) {
        int upperBound = Math.min(settings.maxSegmentAxisDelta(), snapshotAxisDeltaLimit());
        return Math.clamp(candidateAxisDelta, 1, upperBound);
    }

    private int snapshotAxisDeltaLimit() {
        int height = settings.frontierCaptureVerticalMargin() * 2 + 1;
        int horizontalSpan = (int) Math.floor(Math.sqrt((double) settings.targetSnapshotBlockBudget() / height));
        return Math.max(1, horizontalSpan - settings.frontierCaptureHorizontalMargin() * 2 - 1);
    }

    private static double horizontalDistance(BlockPosition start, BlockPosition end) {
        return Math.hypot(end.x() - start.x(), end.z() - start.z());
    }
}
