package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.MovementProfiles;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceTraversalGraphJumpTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);
    private static final MovementProfile PROFILE = MovementProfiles.defaultPlayerWith(PLAYER);

    @Test
    void findsRaisedPlatformPathFromSetbackJumpCell() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(verticalStepSurface());
        SurfaceNode start = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0);
        SurfaceNode goal = new SurfaceNode(new BlockPosition(3, 64, 0), 1, 1, 65.0);
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(
                world,
                start,
                goal,
                PROFILE,
                SurfaceTraversalGraphSettings.standard(24, 8));

        PathfinderResult<SurfaceNode> result = new AStarPathfinder<SurfaceNode>().search(
                new PathfinderRequest<>(graph, start, goal, SurfaceTraversalGraphJumpTest::distance));

        assertEquals(PathfinderStatus.FOUND, result.status());
    }

    private static Map<BlockPosition, SurfaceBlock> verticalStepSurface() {
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(0, 1, -1, 1);
        for (int x = 2; x <= 3; x++) {
            addFlatRowAtY(blocks, x, 64, -1, 1);
        }
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX, int minZ, int maxZ) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addFlatRowAtY(blocks, x, 63, minZ, maxZ);
        }
        return blocks;
    }

    private static void addFlatRowAtY(
            Map<BlockPosition, SurfaceBlock> blocks, int x, int y, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            blocks.put(new BlockPosition(x, y, z), SurfaceBlock.solid(BlockShape.fullCube()));
        }
    }

    private static double distance(SurfaceNode from, SurfaceNode to) {
        return Math.hypot(from.centerX() - to.centerX(), from.centerZ() - to.centerZ())
                + Math.abs(from.floorY() - to.floorY()) * 0.5;
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
