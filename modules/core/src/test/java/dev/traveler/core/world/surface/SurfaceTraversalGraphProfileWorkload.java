package dev.traveler.core.world.surface;

import dev.traveler.core.graph.Graph;
import dev.traveler.core.graph.MutableGraphPath;
import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.AStarPathfinder;
import dev.traveler.core.path.PathfinderRequest;
import dev.traveler.core.path.PathfinderResult;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.smooth.PathSmoother;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.navigation.SurfaceLineOfWalk;
import dev.traveler.core.world.navigation.SurfaceLineOfWalkSettings;
import dev.traveler.core.world.navigation.SurfaceSmoothingPolicy;
import dev.traveler.core.world.navigation.SurfaceTraversalGraph;
import java.util.List;

public final class SurfaceTraversalGraphProfileWorkload {
    private static final int WARMUP_SEARCHES = 100;
    private static final int MEASURED_SEARCHES = 250;
    private static final int SMOOTH_WARMUP_SEARCHES = 10;
    private static final int SMOOTH_MEASURED_SEARCHES = 25;
    private static final String SMOOTH_MODE = "smooth";
    private static final BlockPosition START_BLOCK = new BlockPosition(0, 63, 0);
    private static final BlockPosition GOAL_BLOCK = new BlockPosition(47, 63, 47);
    private static final SurfaceNode START = new SurfaceNode(START_BLOCK, 1, 1, 64.0);
    private static final SurfaceNode GOAL = new SurfaceNode(GOAL_BLOCK, 0, 0, 64.0);
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    private SurfaceTraversalGraphProfileWorkload() {}

    public static void main(String[] args) {
        boolean smoothing = args.length > 0 && SMOOTH_MODE.equals(args[0]);
        int warmupSearches = smoothing ? SMOOTH_WARMUP_SEARCHES : WARMUP_SEARCHES;
        int measuredSearches = smoothing ? SMOOTH_MEASURED_SEARCHES : MEASURED_SEARCHES;
        ProfileSurfaceWorldLayer world = new ProfileSurfaceWorldLayer();
        runSearches(world, warmupSearches, smoothing);
        long startNanos = System.nanoTime();
        int found = runSearches(world, measuredSearches, smoothing);
        long elapsedNanos = System.nanoTime() - startNanos;
        System.out.println("found=" + found
                + " searches=" + measuredSearches
                + " mode=" + modeName(smoothing)
                + " millis=" + elapsedNanos / 1_000_000L
                + " blockReads=" + world.blockReads());
    }

    private static int runSearches(ProfileSurfaceWorldLayer world, int searches, boolean smoothing) {
        int found = 0;
        for (int index = 0; index < searches; index++) {
            if (search(world, smoothing).status() == PathfinderStatus.FOUND) {
                found++;
            }
        }
        return found;
    }

    private static PathfinderResult<SurfaceNode> search(ProfileSurfaceWorldLayer world, boolean smoothing) {
        Graph<SurfaceNode> graph = new SurfaceTraversalGraph(world, START, GOAL, PLAYER, 56, 4);
        PathfinderRequest<SurfaceNode> request =
                new PathfinderRequest<>(graph, START, GOAL, SurfaceTraversalGraphProfileWorkload::distance);
        PathfinderResult<SurfaceNode> result = new AStarPathfinder<SurfaceNode>().search(request);
        if (!smoothing) {
            return result;
        }
        return smoothedResult(world, result);
    }

    private static PathfinderResult<SurfaceNode> smoothedResult(
            ProfileSurfaceWorldLayer world,
            PathfinderResult<SurfaceNode> result) {
        if (result.status() != PathfinderStatus.FOUND || result.path().nodeCount() < 3) {
            return result;
        }
        List<SurfaceNode> nodes = result.path().nodes();
        List<SurfaceNode> smoothed =
                new PathSmoother<SurfaceNode>(new SurfaceLineOfWalk(
                                world,
                                nodes.getFirst(),
                                nodes.getLast(),
                                PLAYER,
                                SurfaceLineOfWalkSettings.smoothing(56, 4)),
                                new SurfaceSmoothingPolicy(world))
                        .smooth(nodes);
        MutableGraphPath<SurfaceNode> path = new MutableGraphPath<>();
        smoothed.forEach(path::addNode);
        path.setCost(result.path().cost());
        return new PathfinderResult<>(result.status(), path);
    }

    private static double distance(SurfaceNode from, SurfaceNode to) {
        return Math.hypot(from.centerX() - to.centerX(), from.centerZ() - to.centerZ());
    }

    private static String modeName(boolean smoothing) {
        return smoothing ? SMOOTH_MODE : "graph";
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
