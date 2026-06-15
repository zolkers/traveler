package dev.traveler.core.layer;

public record WorldNavigationBudget(int visibleHorizontalRadiusBlocks) {
    private static final WorldNavigationBudget UNBOUNDED =
            new WorldNavigationBudget(Integer.MAX_VALUE);

    public WorldNavigationBudget {
        if (visibleHorizontalRadiusBlocks <= 0) {
            throw new IllegalArgumentException("visibleHorizontalRadiusBlocks must be positive.");
        }
    }

    public static WorldNavigationBudget unbounded() {
        return UNBOUNDED;
    }
}
