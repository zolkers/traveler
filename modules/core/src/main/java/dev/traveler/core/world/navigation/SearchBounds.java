package dev.traveler.core.world.navigation;

import dev.traveler.core.world.block.BlockPosition;
record SearchBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    static SearchBounds around(
            BlockPosition start,
            BlockPosition goal,
            int horizontalMargin,
            int verticalMargin) {
        int safeHorizontalMargin = requirePositive(horizontalMargin, "horizontalMargin");
        int safeVerticalMargin = requirePositive(verticalMargin, "verticalMargin");
        return new SearchBounds(
                min(start.x(), goal.x()) - safeHorizontalMargin,
                max(start.x(), goal.x()) + safeHorizontalMargin,
                min(start.y(), goal.y()) - safeVerticalMargin,
                max(start.y(), goal.y()) + safeVerticalMargin,
                min(start.z(), goal.z()) - safeHorizontalMargin,
                max(start.z(), goal.z()) + safeHorizontalMargin);
    }

    boolean contains(BlockPosition position) {
        return contains(position.x(), position.y(), position.z());
    }

    boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    int width() {
        return maxX - minX + 1;
    }

    int height() {
        return maxY - minY + 1;
    }

    int depth() {
        return maxZ - minZ + 1;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static int min(int first, int second) {
        return Math.min(first, second);
    }

    private static int max(int first, int second) {
        return Math.max(first, second);
    }
}
