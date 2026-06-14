package dev.traveler.core.world.geometry;

public record CollisionBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public CollisionBox {
        requireHorizontalRange(minX, "minX");
        requireVerticalRange(minY, "minY");
        requireHorizontalRange(minZ, "minZ");
        requireHorizontalRange(maxX, "maxX");
        requireVerticalRange(maxY, "maxY");
        requireHorizontalRange(maxZ, "maxZ");
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            throw new IllegalArgumentException("Collision box max bounds must be greater than min bounds.");
        }
    }

    boolean overlapsHorizontal(double minX, double minZ, double maxX, double maxZ) {
        return this.maxX > minX && this.minX < maxX && this.maxZ > minZ && this.minZ < maxZ;
    }

    boolean overlapsVertical(double minY, double maxY) {
        return maxY > this.minY && minY < this.maxY;
    }

    private static void requireHorizontalRange(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be finite and inside 0..1.");
        }
    }

    private static void requireVerticalRange(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }
}
