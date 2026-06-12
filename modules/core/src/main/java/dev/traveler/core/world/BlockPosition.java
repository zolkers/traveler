package dev.traveler.core.world;

import java.util.List;

public record BlockPosition(int x, int y, int z) {
    public BlockPosition offset(int xOffset, int yOffset, int zOffset) {
        return new BlockPosition(x + xOffset, y + yOffset, z + zOffset);
    }

    public BlockPosition east() {
        return offset(1, 0, 0);
    }

    public BlockPosition west() {
        return offset(-1, 0, 0);
    }

    public BlockPosition above() {
        return offset(0, 1, 0);
    }

    public BlockPosition below() {
        return offset(0, -1, 0);
    }

    public BlockPosition south() {
        return offset(0, 0, 1);
    }

    public BlockPosition north() {
        return offset(0, 0, -1);
    }

    public List<BlockPosition> neighbors() {
        return List.of(east(), west(), above(), below(), south(), north());
    }
}
