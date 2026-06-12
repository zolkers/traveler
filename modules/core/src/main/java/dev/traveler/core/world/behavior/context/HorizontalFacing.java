package dev.traveler.core.world.behavior.context;

public enum HorizontalFacing {
    NORTH(0, -1),
    SOUTH(0, 1),
    WEST(-1, 0),
    EAST(1, 0);

    private final int xOffset;
    private final int zOffset;

    HorizontalFacing(int xOffset, int zOffset) {
        this.xOffset = xOffset;
        this.zOffset = zOffset;
    }

    public int xOffset() {
        return xOffset;
    }

    public int zOffset() {
        return zOffset;
    }

    public HorizontalFacing opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }
}
