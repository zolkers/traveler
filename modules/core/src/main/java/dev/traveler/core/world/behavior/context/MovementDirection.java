package dev.traveler.core.world.behavior.context;

import java.util.Objects;

public record MovementDirection(int x, int z) {
    private static final MovementDirection NORTH = new MovementDirection(0, -1);
    private static final MovementDirection SOUTH = new MovementDirection(0, 1);
    private static final MovementDirection WEST = new MovementDirection(-1, 0);
    private static final MovementDirection EAST = new MovementDirection(1, 0);
    private static final MovementDirection NORTH_WEST = new MovementDirection(-1, -1);
    private static final MovementDirection NORTH_EAST = new MovementDirection(1, -1);
    private static final MovementDirection SOUTH_WEST = new MovementDirection(-1, 1);
    private static final MovementDirection SOUTH_EAST = new MovementDirection(1, 1);

    public MovementDirection {
        x = Integer.compare(x, 0);
        z = Integer.compare(z, 0);
        if (x == 0 && z == 0) {
            throw new IllegalArgumentException("Movement direction cannot be zero.");
        }
    }

    public static MovementDirection fromOffset(int x, int z) {
        return cached(Integer.compare(x, 0), Integer.compare(z, 0));
    }

    public static MovementDirection north() {
        return NORTH;
    }

    public static MovementDirection south() {
        return SOUTH;
    }

    public static MovementDirection west() {
        return WEST;
    }

    public static MovementDirection east() {
        return EAST;
    }

    public boolean isDiagonal() {
        return x != 0 && z != 0;
    }

    public boolean matches(HorizontalFacing facing) {
        HorizontalFacing safeFacing = Objects.requireNonNull(facing, "facing");
        return x == safeFacing.xOffset() && z == safeFacing.zOffset();
    }

    public boolean isPerpendicularTo(HorizontalFacing facing) {
        HorizontalFacing safeFacing = Objects.requireNonNull(facing, "facing");
        return !isDiagonal() && x * safeFacing.xOffset() + z * safeFacing.zOffset() == 0;
    }

    private static MovementDirection cached(int x, int z) {
        if (x == 0) {
            return vertical(z);
        }
        if (z == 0) {
            return horizontal(x);
        }
        return diagonal(x, z);
    }

    private static MovementDirection vertical(int z) {
        if (z < 0) {
            return NORTH;
        }
        if (z > 0) {
            return SOUTH;
        }
        throw new IllegalArgumentException("Movement direction cannot be zero.");
    }

    private static MovementDirection horizontal(int x) {
        return x < 0 ? WEST : EAST;
    }

    private static MovementDirection diagonal(int x, int z) {
        if (x < 0) {
            return z < 0 ? NORTH_WEST : SOUTH_WEST;
        }
        return z < 0 ? NORTH_EAST : SOUTH_EAST;
    }
}
