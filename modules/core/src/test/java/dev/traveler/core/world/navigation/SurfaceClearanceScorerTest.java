package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceClearanceScorerTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void diagonalBlockedCellsAddRiskToClearanceScore() {
        SurfaceNode open = nodeAt(-1, 0);
        SurfaceNode diagonalRisk = nodeAt(0, 0);
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(-1, 1, -1, 1);
        blocks.put(new BlockPosition(1, 64, 1), SurfaceBlock.solid(BlockShape.fullCube()));
        SurfaceTraversalGraph graph = new SurfaceTraversalGraph(
                new TestSurfaceWorldLayer(blocks),
                open,
                diagonalRisk,
                PLAYER,
                SurfaceTraversalGraphSettings.standard(8, 4));

        double openScore = SurfaceClearanceScorer.standard().score(graph, open);
        double riskScore = SurfaceClearanceScorer.standard().score(graph, diagonalRisk);

        assertTrue(riskScore > openScore);
    }

    private static SurfaceNode nodeAt(int x, int z) {
        return new SurfaceNode(new BlockPosition(x, 63, z), 1, 1, 64.0);
    }

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX, int minZ, int maxZ) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addFlatColumn(blocks, x, minZ, maxZ);
        }
        return blocks;
    }

    private static void addFlatColumn(Map<BlockPosition, SurfaceBlock> blocks, int x, int minZ, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            blocks.put(new BlockPosition(x, 63, z), SurfaceBlock.solid(BlockShape.fullCube()));
        }
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
