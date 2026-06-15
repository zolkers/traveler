package dev.traveler.core.route.longdistance;

public record LongDistanceRouteSettings(
        double directHorizontalDistance,
        double segmentHorizontalDistance,
        int maxSegmentAxisDelta,
        double replanDistance) {
    public LongDistanceRouteSettings {
        requirePositive(directHorizontalDistance, "directHorizontalDistance");
        requirePositive(segmentHorizontalDistance, "segmentHorizontalDistance");
        if (maxSegmentAxisDelta <= 0) {
            throw new IllegalArgumentException("maxSegmentAxisDelta must be positive.");
        }
        requirePositive(replanDistance, "replanDistance");
    }

    private static void requirePositive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite.");
        }
    }
}
