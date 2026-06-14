package dev.traveler.core.world.navigation;

import java.util.List;
import java.util.Objects;

public record SurfaceTraversalGraphSettings(
        int horizontalMargin,
        int verticalMargin,
        SurfaceClearanceScorer clearanceScorer,
        SurfaceBodyClearanceMode bodyClearanceMode,
        List<SurfaceConnectionProvider> connectionProviders) {
    public SurfaceTraversalGraphSettings {
        requirePositive(horizontalMargin, "horizontalMargin");
        requirePositive(verticalMargin, "verticalMargin");
        Objects.requireNonNull(clearanceScorer, "clearanceScorer");
        Objects.requireNonNull(bodyClearanceMode, "bodyClearanceMode");
        connectionProviders = List.copyOf(Objects.requireNonNull(connectionProviders, "connectionProviders"));
        if (connectionProviders.isEmpty()) {
            throw new IllegalArgumentException("connectionProviders must not be empty");
        }
    }

    public SurfaceTraversalGraphSettings(
            int horizontalMargin,
            int verticalMargin,
            SurfaceClearanceScorer clearanceScorer,
            SurfaceBodyClearanceMode bodyClearanceMode) {
        this(
                horizontalMargin,
                verticalMargin,
                clearanceScorer,
                bodyClearanceMode,
                SurfaceConnectionProvider.standard());
    }

    public static SurfaceTraversalGraphSettings basic(int horizontalMargin, int verticalMargin) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                SurfaceClearanceScorer.disabled(),
                SurfaceBodyClearanceMode.ADJUSTED);
    }

    public static SurfaceTraversalGraphSettings standard(int horizontalMargin, int verticalMargin) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                SurfaceClearanceScorer.standard(),
                SurfaceBodyClearanceMode.ADJUSTED);
    }

    public static SurfaceTraversalGraphSettings exact(int horizontalMargin, int verticalMargin) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                SurfaceClearanceScorer.disabled(),
                SurfaceBodyClearanceMode.EXACT);
    }

    public SurfaceTraversalGraphSettings withConnectionProviders(List<SurfaceConnectionProvider> providers) {
        return new SurfaceTraversalGraphSettings(
                horizontalMargin,
                verticalMargin,
                clearanceScorer,
                bodyClearanceMode,
                providers);
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
