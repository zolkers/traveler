package dev.traveler.core.common.geometry;

public record HorizontalVector(double x, double z) {
    public HorizontalVector {
        requireFinite(x, "x");
        requireFinite(z, "z");
    }

    public double length() {
        return Math.hypot(x, z);
    }

    public boolean isZero() {
        return length() <= 1.0E-6;
    }

    public HorizontalVector normalized() {
        double currentLength = length();
        if (currentLength <= 1.0E-6) {
            return new HorizontalVector(0.0, 0.0);
        }
        return new HorizontalVector(x / currentLength, z / currentLength);
    }

    public double dot(HorizontalVector other) {
        HorizontalVector vector = java.util.Objects.requireNonNull(other, "other");
        return x * vector.x + z * vector.z;
    }

    public HorizontalVector plus(HorizontalVector other) {
        HorizontalVector vector = java.util.Objects.requireNonNull(other, "other");
        return new HorizontalVector(x + vector.x, z + vector.z);
    }

    public HorizontalVector scaled(double scale) {
        requireFinite(scale, "scale");
        return new HorizontalVector(x * scale, z * scale);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite.");
        }
    }
}
