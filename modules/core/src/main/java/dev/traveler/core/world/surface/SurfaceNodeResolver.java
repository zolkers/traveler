package dev.traveler.core.world.surface;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.layer.SurfaceWorldLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SurfaceNodeResolver {
    private static final int CENTER_CELL = 1;
    private static final double STANDING_RANGE = 1.0;
    private static final double FLOOR_EPSILON = 0.001;

    private final SurfaceWorldLayer worldLayer;

    public SurfaceNodeResolver(SurfaceWorldLayer worldLayer) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
    }

    public Optional<SurfaceNode> centeredSurface(BlockPosition supportPosition) {
        return surfaceAt(supportPosition, CENTER_CELL, CENTER_CELL);
    }

    public List<SurfaceNode> surfaces(BlockPosition supportPosition) {
        List<SurfaceNode> nodes = new ArrayList<>(4);
        for (int cellX = 0; cellX <= 1; cellX++) {
            addSurfaceRow(nodes, supportPosition, cellX);
        }
        return List.copyOf(nodes);
    }

    public Optional<SurfaceNode> standingSurface(BlockPosition feetPosition) {
        return standingSurfaces(feetPosition).stream().findFirst();
    }

    public List<SurfaceNode> standingSurfaces(BlockPosition feetPosition) {
        List<SurfaceNode> candidates = new ArrayList<>(8);
        addStandingCandidates(candidates, feetPosition, feetPosition);
        addStandingCandidates(candidates, feetPosition, feetPosition.below());
        return highestSurfaces(candidates);
    }

    public Optional<SurfaceNode> surfaceAt(BlockPosition supportPosition, int cellX, int cellZ) {
        BlockPosition safePosition = Objects.requireNonNull(supportPosition, "supportPosition");
        double floor = worldLayer.surfaceBlock(safePosition).shape().floorHeightForCellOrNaN(cellX, cellZ);
        if (Double.isNaN(floor)) {
            return Optional.empty();
        }
        return Optional.of(new SurfaceNode(safePosition, cellX, cellZ, safePosition.y() + floor));
    }

    private void addSurfaceRow(List<SurfaceNode> nodes, BlockPosition supportPosition, int cellX) {
        for (int cellZ = 0; cellZ <= 1; cellZ++) {
            surfaceAt(supportPosition, cellX, cellZ).ifPresent(nodes::add);
        }
    }

    private void addStandingCandidates(
            List<SurfaceNode> candidates, BlockPosition feetPosition, BlockPosition supportPosition) {
        for (SurfaceNode node : surfaces(supportPosition)) {
            addStandingCandidate(candidates, feetPosition, node);
        }
    }

    private static void addStandingCandidate(
            List<SurfaceNode> candidates, BlockPosition feetPosition, SurfaceNode node) {
        if (isStandingSurface(feetPosition, node)) {
            candidates.add(node);
        }
    }

    private static List<SurfaceNode> highestSurfaces(List<SurfaceNode> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        double highest = highestFloor(candidates);
        return candidates.stream()
                .filter(node -> sameFloor(node.floorY(), highest))
                .toList();
    }

    private static double highestFloor(List<SurfaceNode> candidates) {
        double highest = Double.NEGATIVE_INFINITY;
        for (SurfaceNode candidate : candidates) {
            highest = Math.max(highest, candidate.floorY());
        }
        return highest;
    }

    private static boolean isStandingSurface(BlockPosition feetPosition, SurfaceNode node) {
        double minFloor = feetPosition.y() - FLOOR_EPSILON;
        double maxFloor = feetPosition.y() + STANDING_RANGE + FLOOR_EPSILON;
        return node.floorY() >= minFloor && node.floorY() <= maxFloor;
    }

    private static boolean sameFloor(double first, double second) {
        return Math.abs(first - second) <= FLOOR_EPSILON;
    }
}
