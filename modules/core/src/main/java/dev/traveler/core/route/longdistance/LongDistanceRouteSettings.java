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
        int visibilityEdgeSafetyBlocks) {
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
                24);
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
                visibilityEdgeSafetyBlocks);
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
}
