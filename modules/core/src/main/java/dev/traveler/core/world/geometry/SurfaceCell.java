package dev.traveler.core.world.geometry;

public final class SurfaceCell {
    private static final double SIZE = 0.5;

    private SurfaceCell() {}

    public static void requireIndex(int value, String name) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be 0 or 1.");
        }
    }

    public static double min(int index) {
        requireIndex(index, "index");
        return index * SIZE;
    }

    public static double max(int index) {
        return min(index) + SIZE;
    }

    public static double centerOffset(int index) {
        return min(index) + SIZE * 0.5;
    }
}
