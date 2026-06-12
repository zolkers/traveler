package dev.traveler.core.world.surface;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;

public final class SurfaceTraversalGraphProfileWorkload {
    private static final int WARMUP_SEARCHES = 100;
    private static final int MEASURED_SEARCHES = 250;
    private static final BlockPosition START_BLOCK = new BlockPosition(0, 63, 0);
    private static final BlockPosition GOAL_BLOCK = new BlockPosition(47, 63, 47);
    private static final SurfaceNode START = new SurfaceNode(START_BLOCK, 1, 1, 64.0);
    private static final SurfaceNode GOAL = new SurfaceNode(GOAL_BLOCK, 0, 0, 64.0);
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    private SurfaceTraversalGraphProfileWorkload() {}

    public static void main(String[] args) {
        ProfileSurfaceWorldLayer world = new ProfileSurfaceWorldLayer();
        runSearches(world, WARMUP_SEARCHES);
        long startNanos = System.nanoTime();
        int found = runSearches(world, MEASURED_SEARCHES);
        long elapsedNanos = System.nanoTime() - startNanos;
        System.out.println("found=" + found
                + " searches=" + MEASURED_SEARCHES
                + " millis=" + elapsedNanos / 1_000_000L
                + " blockReads=" + world.blockReads());
    }

    private static int runSearches(ProfileSurfaceWorldLayer world, int searches) {
        int found = 0;
        for (int index = 0; index < searches; index++) {
            if (search(world).status() == PathfinderStatus.FOUND) {
                found++;
            }
        }
        return found;
    }

    private static PathfinderResult<SurfaceNode> search(ProfileSurfaceWorldLayer world) {
        Graph<SurfaceNode> graph = new SurfaceTraversalGraph(world, START, GOAL, PLAYER, 56, 4);
        PathfinderRequest<SurfaceNode> request =
                new PathfinderRequest<>(graph, START, GOAL, SurfaceTraversalGraphProfileWorkload::distance);
        return new AStarPathfinder<SurfaceNode>().search(request);
    }

    private static double distance(SurfaceNode from, SurfaceNode to) {
        return Math.hypot(from.centerX() - to.centerX(), from.centerZ() - to.centerZ());
    }

    private static final class ProfileSurfaceWorldLayer implements SurfaceWorldLayer {
        private static final SurfaceBlock GROUND = SurfaceBlock.solid(BlockShape.fullCube());
        private static final SurfaceBlock AIR = SurfaceBlock.empty();

        private int blockReads;

        @Override
        public BlockClassification classify(BlockPosition position) {
            return surfaceBlock(position).classification();
        }

        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            blockReads++;
            if (position.y() != 63 || isBlocked(position)) {
                return AIR;
            }
            return GROUND;
        }

        private int blockReads() {
            return blockReads;
        }

        private static boolean isBlocked(BlockPosition position) {
            if (position.equals(START_BLOCK) || position.equals(GOAL_BLOCK)) {
                return false;
            }
            return verticalBarrier(position) || horizontalBarrier(position);
        }

        private static boolean verticalBarrier(BlockPosition position) {
            return Math.floorMod(position.x(), 9) == 4 && Math.floorMod(position.z(), 11) != 5;
        }

        private static boolean horizontalBarrier(BlockPosition position) {
            return Math.floorMod(position.z(), 13) == 7 && Math.floorMod(position.x(), 10) != 3;
        }
    }
}
