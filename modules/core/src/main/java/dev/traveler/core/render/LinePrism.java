package dev.traveler.core.render;

import java.util.Objects;

public record LinePrism(
        RenderVertex startA,
        RenderVertex startB,
        RenderVertex startC,
        RenderVertex startD,
        RenderVertex endA,
        RenderVertex endB,
        RenderVertex endC,
        RenderVertex endD) {
    private static final double MIN_LENGTH = 1.0E-6;
    private static final Vector3 WORLD_UP = new Vector3(0.0, 1.0, 0.0);
    private static final Vector3 WORLD_X = new Vector3(1.0, 0.0, 0.0);

    public LinePrism {
        Objects.requireNonNull(startA, "startA");
        Objects.requireNonNull(startB, "startB");
        Objects.requireNonNull(startC, "startC");
        Objects.requireNonNull(startD, "startD");
        Objects.requireNonNull(endA, "endA");
        Objects.requireNonNull(endB, "endB");
        Objects.requireNonNull(endC, "endC");
        Objects.requireNonNull(endD, "endD");
    }

    public static LinePrism around(RenderVertex from, RenderVertex to, double thickness) {
        RenderVertex start = Objects.requireNonNull(from, "from");
        RenderVertex end = Objects.requireNonNull(to, "to");
        double radius = radius(thickness);
        Vector3 direction = Vector3.between(start, end).normalized();
        Vector3 side = referenceFor(direction).cross(direction).normalized();
        Vector3 vertical = direction.cross(side).normalized();
        return new LinePrism(
                corner(start, side, radius, vertical, radius),
                corner(start, side, -radius, vertical, radius),
                corner(start, side, -radius, vertical, -radius),
                corner(start, side, radius, vertical, -radius),
                corner(end, side, radius, vertical, radius),
                corner(end, side, -radius, vertical, radius),
                corner(end, side, -radius, vertical, -radius),
                corner(end, side, radius, vertical, -radius));
    }

    private static RenderVertex corner(
            RenderVertex origin,
            Vector3 side,
            double sideScale,
            Vector3 vertical,
            double verticalScale) {
        return new RenderVertex(
                origin.x() + side.x() * sideScale + vertical.x() * verticalScale,
                origin.y() + side.y() * sideScale + vertical.y() * verticalScale,
                origin.z() + side.z() * sideScale + vertical.z() * verticalScale);
    }

    private static Vector3 referenceFor(Vector3 direction) {
        if (Math.abs(direction.y()) > 0.95) {
            return WORLD_X;
        }
        return WORLD_UP;
    }

    private static double radius(double thickness) {
        if (!Double.isFinite(thickness) || thickness <= 0.0) {
            throw new IllegalArgumentException("thickness must be positive.");
        }
        return thickness * 0.5;
    }

    private record Vector3(double x, double y, double z) {
        private static Vector3 between(RenderVertex from, RenderVertex to) {
            return new Vector3(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
        }

        private double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        private Vector3 normalized() {
            double currentLength = length();
            if (currentLength <= MIN_LENGTH) {
                throw new IllegalArgumentException("line segment must have positive length.");
            }
            return new Vector3(x / currentLength, y / currentLength, z / currentLength);
        }

        private Vector3 cross(Vector3 other) {
            Vector3 vector = Objects.requireNonNull(other, "other");
            return new Vector3(
                    y * vector.z - z * vector.y,
                    z * vector.x - x * vector.z,
                    x * vector.y - y * vector.x);
        }
    }
}
