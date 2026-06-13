package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceLineOfWalkTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void acceptsClearFlatSurfaceLines() {
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(flatWorld(0, 4));

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertTrue(clear);
    }

    @Test
    void acceptsClearFlatSurfaceLinesWithAdjacentClearance() {
        SurfaceLineOfWalk lineOfWalk = smoothLineOfWalk(flatWorld(0, 4, -1, 1));

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertTrue(clear);
    }

    @Test
    void rejectsSmoothSurfaceLinesTooCloseToAdjacentBlockedBodySpace() {
        Map<BlockPosition, SurfaceBlock> blocks = flatBlocks(0, 4, 0, 1);
        for (int x = 0; x <= 4; x++) {
            blocks.put(new BlockPosition(x, 64, 1), SurfaceBlock.solid(BlockShape.fullCube()));
        }
        SurfaceLineOfWalk lineOfWalk = smoothLineOfWalk(new TestSurfaceWorldLayer(blocks));

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    @Test
    void rejectsSmoothSurfaceLinesWhenEntityWidthTouchesOuterBodySpace() {
        Map<BlockPosition, SurfaceBlock> blocks = flatBlocks(0, 4, -1, 1);
        blocks.put(
                new BlockPosition(2, 64, 1),
                SurfaceBlock.solid(BlockShape.of(List.of(new CollisionBox(0.0, 0.0, 0.5, 1.0, 1.0, 1.0)))));
        SurfaceLineOfWalk lineOfWalk = smoothLineOfWalk(
                new TestSurfaceWorldLayer(blocks),
                widePlayerProfile());

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    @Test
    void rejectsSurfaceLinesThroughBlockedBodySpace() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(1), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(2), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(3), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(4), SurfaceBlock.solid(BlockShape.fullCube()),
                new BlockPosition(2, 64, 0), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(world);

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    @Test
    void rejectsSurfaceLinesWithoutIntermediateSupport() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(4), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(world);

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    private static SurfaceWorldLayer flatWorld(int minX, int maxX) {
        return new TestSurfaceWorldLayer(flatBlocks(minX, maxX, 0, 0));
    }

    private static SurfaceWorldLayer flatWorld(int minX, int maxX, int minZ, int maxZ) {
        return new TestSurfaceWorldLayer(flatBlocks(minX, maxX, minZ, maxZ));
    }

    private static Map<BlockPosition, SurfaceBlock> flatBlocks(int minX, int maxX, int minZ, int maxZ) {
        Map<BlockPosition, SurfaceBlock> blocks = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addFlatRow(blocks, x, minZ, maxZ);
        }
        return blocks;
    }

    private static void addFlatRow(Map<BlockPosition, SurfaceBlock> blocks, int x, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            blocks.put(supportAt(x, z), SurfaceBlock.solid(BlockShape.fullCube()));
        }
    }

    private static SurfaceNode nodeAt(int x) {
        return new SurfaceNode(supportAt(x), 1, 1, 64.0);
    }

    private static SurfaceLineOfWalk lineOfWalk(SurfaceWorldLayer world) {
        return new SurfaceLineOfWalk(world, nodeAt(0), nodeAt(4), PLAYER, 8, 4);
    }

    private static SurfaceLineOfWalk smoothLineOfWalk(SurfaceWorldLayer world) {
        return new SurfaceLineOfWalk(
                world,
                nodeAt(0),
                nodeAt(4),
                PLAYER,
                SurfaceLineOfWalkSettings.smoothing(8, 4));
    }

    private static SurfaceLineOfWalk smoothLineOfWalk(SurfaceWorldLayer world, MovementProfile profile) {
        return new SurfaceLineOfWalk(
                world,
                nodeAt(0),
                nodeAt(4),
                profile,
                SurfaceLineOfWalkSettings.smoothing(8, 4));
    }

    private static MovementProfile widePlayerProfile() {
        return new MovementProfile(
                new EntityDimensions(1.6, 1.8),
                PLAYER,
                MovementProfiles.defaultPlayerRules());
    }

    private static BlockPosition supportAt(int x) {
        return supportAt(x, 0);
    }

    private static BlockPosition supportAt(int x, int z) {
        return new BlockPosition(x, 63, z);
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
