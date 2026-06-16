package dev.traveler.core.route.longdistance;

public record LongDistanceRouteSettings(
        double directHorizontalDistance,
        double segmentHorizontalDistance,
        int maxSegmentAxisDelta,
        double replanDistance,
        int frontierVerticalSearchRadius,
        int frontierLateralStep,
        int frontierLateralSamples,
        int frontierCaptureHorizontalMargin,
        int frontierCaptureVerticalMargin,
        int visibilityEdgeSafetyBlocks,
        int targetSnapshotBlockBudget,
        int frontierFallbackSurfaceGoalLimit,
        double minimumLookaheadReplanDistance,
        double lookaheadReplanDistanceRatio,
        double maximumLookaheadReplanDistance,
        double lookaheadReplanSegmentCapRatio) {
    public LongDistanceRouteSettings(
            double directHorizontalDistance,
            double segmentHorizontalDistance,
            int maxSegmentAxisDelta,
            double replanDistance) {
        this(
                directHorizontalDistance,
                segmentHorizontalDistance,
                maxSegmentAxisDelta,
                replanDistance,
                16,
                1,
                8,
                8,
                16,
                24,
                160_000,
                16,
                48.0,
                0.5,
                96.0,
                0.75);
    }

    public LongDistanceRouteSettings(
            double directHorizontalDistance,
            double segmentHorizontalDistance,
            int maxSegmentAxisDelta,
            double replanDistance,
            int frontierVerticalSearchRadius,
            int frontierLateralStep,
            int frontierLateralSamples,
            int frontierCaptureHorizontalMargin,
            int frontierCaptureVerticalMargin,
            int visibilityEdgeSafetyBlocks,
            int targetSnapshotBlockBudget,
            int frontierFallbackSurfaceGoalLimit) {
        this(
                directHorizontalDistance,
                segmentHorizontalDistance,
                maxSegmentAxisDelta,
                replanDistance,
                frontierVerticalSearchRadius,
                frontierLateralStep,
                frontierLateralSamples,
                frontierCaptureHorizontalMargin,
                frontierCaptureVerticalMargin,
                visibilityEdgeSafetyBlocks,
                targetSnapshotBlockBudget,
                frontierFallbackSurfaceGoalLimit,
                48.0,
                0.5,
                96.0,
                0.75);
    }

    public LongDistanceRouteSettings {
        requirePositive(directHorizontalDistance, "directHorizontalDistance");
        requirePositive(segmentHorizontalDistance, "segmentHorizontalDistance");
        if (maxSegmentAxisDelta <= 0) {
            throw new IllegalArgumentException("maxSegmentAxisDelta must be positive.");
        }
        requirePositive(replanDistance, "replanDistance");
        requireNonNegative(frontierVerticalSearchRadius, "frontierVerticalSearchRadius");
        if (frontierLateralStep <= 0) {
            throw new IllegalArgumentException("frontierLateralStep must be positive.");
        }
        requireNonNegative(frontierLateralSamples, "frontierLateralSamples");
        requireNonNegative(frontierCaptureHorizontalMargin, "frontierCaptureHorizontalMargin");
        requireNonNegative(frontierCaptureVerticalMargin, "frontierCaptureVerticalMargin");
        requireNonNegative(visibilityEdgeSafetyBlocks, "visibilityEdgeSafetyBlocks");
        if (targetSnapshotBlockBudget <= 0) {
            throw new IllegalArgumentException("targetSnapshotBlockBudget must be positive.");
        }
        if (frontierFallbackSurfaceGoalLimit <= 0) {
            throw new IllegalArgumentException("frontierFallbackSurfaceGoalLimit must be positive.");
        }
        requirePositive(minimumLookaheadReplanDistance, "minimumLookaheadReplanDistance");
        requirePositive(lookaheadReplanDistanceRatio, "lookaheadReplanDistanceRatio");
        requirePositive(maximumLookaheadReplanDistance, "maximumLookaheadReplanDistance");
        requirePositive(lookaheadReplanSegmentCapRatio, "lookaheadReplanSegmentCapRatio");
        if (minimumLookaheadReplanDistance > maximumLookaheadReplanDistance) {
            throw new IllegalArgumentException(
                    "minimumLookaheadReplanDistance must be less than or equal to maximumLookaheadReplanDistance.");
        }
    }

    public LongDistanceRouteSettings withMaxSegmentAxisDelta(int axisDelta) {
        return new LongDistanceRouteSettings(
                directHorizontalDistance,
                segmentHorizontalDistance,
                axisDelta,
                replanDistance,
                frontierVerticalSearchRadius,
                frontierLateralStep,
                frontierLateralSamples,
                frontierCaptureHorizontalMargin,
                frontierCaptureVerticalMargin,
                visibilityEdgeSafetyBlocks,
                targetSnapshotBlockBudget,
                frontierFallbackSurfaceGoalLimit,
                minimumLookaheadReplanDistance,
                lookaheadReplanDistanceRatio,
                maximumLookaheadReplanDistance,
                lookaheadReplanSegmentCapRatio);
    }

    public double lookaheadReplanDistance(double activeSegmentHorizontalDistance) {
        requireNonNegative(activeSegmentHorizontalDistance, "activeSegmentHorizontalDistance");
        if (activeSegmentHorizontalDistance <= 0.0) {
            return replanDistance;
        }
        double desired = Math.max(
                replanDistance,
                Math.max(minimumLookaheadReplanDistance,
                        activeSegmentHorizontalDistance * lookaheadReplanDistanceRatio));
        double capped = Math.min(desired, maximumLookaheadReplanDistance);
        return Math.min(capped, activeSegmentHorizontalDistance * lookaheadReplanSegmentCapRatio);
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }
}
