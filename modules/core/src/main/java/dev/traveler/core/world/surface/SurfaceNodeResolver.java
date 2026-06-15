package dev.traveler.core.world.surface;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.settings.TravelerSettings;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SurfaceNodeResolver {
    private static final int CENTER_CELL = 1;
    private static final double PARTIAL_SUPPORT_RANGE = 0.5;
    private static final double STANDING_RANGE = 1.0;
    private static final double FLOOR_EPSILON = 0.001;
    private static final int[] CLIMB_LANDING_Y_OFFSETS = {0, -1, 1};

    private final SurfaceWorldLayer worldLayer;
    private final MovementCapabilities capabilities;

    public SurfaceNodeResolver(SurfaceWorldLayer worldLayer) {
        this(
                worldLayer,
                TravelerSettings.standard().movementCapabilities());
    }

    public SurfaceNodeResolver(SurfaceWorldLayer worldLayer, MovementCapabilities capabilities) {
        this.worldLayer = Objects.requireNonNull(worldLayer, "worldLayer");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
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

    public List<SurfaceNode> climbLandingSurfaces(BlockPosition climbPosition) {
        BlockPosition safePosition = Objects.requireNonNull(climbPosition, "climbPosition");
        Optional<ClimbableBlockBehavior> climbable = climbableBehaviorAt(safePosition);
        if (climbable.isEmpty()) {
            return List.of();
        }
        ClimbableBlockBehavior behavior = climbable.orElseThrow();
        List<SurfaceNode> candidates = new ArrayList<>();
        for (HorizontalFacing face : HorizontalFacing.values()) {
            if (behavior.climbSurface(face, capabilities).isEmpty()) {
                continue;
            }
            for (int yOffset : CLIMB_LANDING_Y_OFFSETS) {
                addClimbLandingAtY(candidates, safePosition, safePosition.y() + yOffset, face);
            }
        }
        List<SurfaceNode> preferredLandings = highestSurfaces(candidates);
        if (!preferredLandings.isEmpty()) {
            return preferredLandings;
        }
        for (HorizontalFacing face : HorizontalFacing.values()) {
            for (int yOffset : CLIMB_LANDING_Y_OFFSETS) {
                addClimbLandingAtY(candidates, safePosition, safePosition.y() + yOffset, face);
            }
        }
        return highestSurfaces(candidates);
    }

    public List<SurfaceNode> climbSurfaces(BlockPosition climbPosition) {
        BlockPosition safePosition = Objects.requireNonNull(climbPosition, "climbPosition");
        Optional<ClimbableBlockBehavior> climbable = climbableBehaviorAt(safePosition);
        if (climbable.isEmpty()) {
            return List.of();
        }
        ClimbableBlockBehavior behavior = climbable.orElseThrow();
        List<SurfaceNode> nodes = new ArrayList<>();
        for (HorizontalFacing face : HorizontalFacing.values()) {
            behavior.climbSurface(face, capabilities)
                    .map(surface -> surface.node(safePosition))
                    .ifPresent(nodes::add);
        }
        return List.copyOf(nodes);
    }

    public Optional<SurfaceNode> surfaceAt(BlockPosition supportPosition, int cellX, int cellZ) {
        BlockPosition safePosition = Objects.requireNonNull(supportPosition, "supportPosition");
        double floor = worldLayer.surfaceBlock(safePosition).shape().floorHeightForCellOrNaN(cellX, cellZ);
        if (!Double.isNaN(floor)) {
            return Optional.of(new SurfaceNode(safePosition, cellX, cellZ, safePosition.y() + floor));
        }
        if (isTopFluidSurface(safePosition)) {
            return Optional.of(new SurfaceNode(safePosition, cellX, cellZ, safePosition.y() + 1.0));
        }
        return Optional.empty();
    }

    private boolean isTopFluidSurface(BlockPosition position) {
        return capabilities.canSwim()
                && hasFluid(position)
                && !hasFluid(position.above());
    }

    private boolean hasFluid(BlockPosition position) {
        return worldLayer.surfaceBlock(position).classification().fluidHandling() == FluidHandling.ALLOW;
    }

    private void addClimbLandingAtY(
            List<SurfaceNode> candidates,
            BlockPosition climbPosition,
            int supportY,
            HorizontalFacing face) {
        BlockPosition support = new BlockPosition(
                climbPosition.x() + face.xOffset(),
                supportY,
                climbPosition.z() + face.zOffset());
        candidates.addAll(surfaces(support));
    }

    private Optional<ClimbableBlockBehavior> climbableBehaviorAt(BlockPosition position) {
        return climbableBehavior(worldLayer.surfaceBlock(position).behavior());
    }

    private static Optional<ClimbableBlockBehavior> climbableBehavior(BlockBehavior behavior) {
        if (behavior instanceof ClimbableBlockBehavior climbable) {
            return Optional.of(climbable);
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return climbableBehavior(waterlogged.delegate());
        }
        return Optional.empty();
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
        double minFloor = feetPosition.y() - PARTIAL_SUPPORT_RANGE - FLOOR_EPSILON;
        double maxFloor = feetPosition.y() + STANDING_RANGE + FLOOR_EPSILON;
        return node.floorY() >= minFloor && node.floorY() <= maxFloor;
    }

    private static boolean sameFloor(double first, double second) {
        return Math.abs(first - second) <= FLOOR_EPSILON;
    }
}
