package dev.traveler.core.world.navigation;

record HorizontalOffset(int x, int z) {
    boolean isDiagonal() {
        return x != 0 && z != 0;
    }
}
