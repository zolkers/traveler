package dev.traveler.core.world.navigation;

final class HorizontalDirections {
    private static final HorizontalOffset EAST = new HorizontalOffset(1, 0);
    private static final HorizontalOffset WEST = new HorizontalOffset(-1, 0);
    private static final HorizontalOffset SOUTH = new HorizontalOffset(0, 1);
    private static final HorizontalOffset NORTH = new HorizontalOffset(0, -1);
    private static final HorizontalOffset SOUTH_EAST = new HorizontalOffset(1, 1);
    private static final HorizontalOffset NORTH_EAST = new HorizontalOffset(1, -1);
    private static final HorizontalOffset SOUTH_WEST = new HorizontalOffset(-1, 1);
    private static final HorizontalOffset NORTH_WEST = new HorizontalOffset(-1, -1);

    static final HorizontalOffset[] CARDINAL = {EAST, WEST, SOUTH, NORTH};

    static final HorizontalOffset[] EIGHT_WAY = {
            EAST,
            WEST,
            SOUTH,
            NORTH,
            SOUTH_EAST,
            NORTH_EAST,
            SOUTH_WEST,
            NORTH_WEST
    };

    private HorizontalDirections() {}
}
