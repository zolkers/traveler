package dev.traveler.core.route.longdistance;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FrontierRouteGoal implements RouteGoal {
    private static final double[] DISTANCE_RATIOS = {1.0, 0.875, 0.75, 0.625, 0.5};

    private final BlockPosition finalPosition;
    private final LongDistanceRouteSettings settings;

    public FrontierRouteGoal(BlockPosition finalPosition, LongDistanceRouteSettings settings) {
        this.finalPosition = Objects.requireNonNull(finalPosition, "finalPosition");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start) {
        SurfaceNodeResolver safeResolver = Objects.requireNonNull(resolver, "resolver");
        BlockPosition safeStart = Objects.requireNonNull(start, "start");
        Direction direction = Direction.between(safeStart, finalPosition);
        Set<SurfaceNode> nodes = new LinkedHashSet<>();
        for (double distanceRatio : DISTANCE_RATIOS) {
            addSurfaceGoalsAtDistance(safeResolver, safeStart, direction, distanceRatio, nodes);
        }
        return List.copyOf(nodes);
    }

    @Override
    public BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start) {
        return preferredPosition(start);
    }

    @Override
    public BlockPosition preferredPosition(BlockPosition fallback) {
        return Direction.between(fallback, finalPosition).frontierCenter(fallback, settings, 1.0);
    }

    @Override
    public boolean isSatisfiedBy(BlockPosition position) {
        BlockPosition safePosition = Objects.requireNonNull(position, "position");
        BlockPosition preferred = preferredPosition(safePosition);
        return safePosition.x() == preferred.x() && safePosition.z() == preferred.z();
    }

    @Override
    public String displayName() {
        return "frontier final="
                + finalPosition.x()
                + ","
                + finalPosition.y()
                + ","
                + finalPosition.z();
    }

    private void addSurfaceGoalsAtDistance(
            SurfaceNodeResolver resolver,
            BlockPosition start,
            Direction direction,
            double distanceRatio,
            Set<SurfaceNode> nodes) {
        BlockPosition center = direction.frontierCenter(start, settings, distanceRatio);
        for (int lateralOffset : lateralOffsets()) {
            int x = center.x() + (int) Math.round(direction.perpendicularX() * lateralOffset);
            int z = center.z() + (int) Math.round(direction.perpendicularZ() * lateralOffset);
            addColumnSurfaces(resolver, start.y(), x, z, nodes);
        }
    }

    private List<Integer> lateralOffsets() {
        List<Integer> offsets = new ArrayList<>(settings.frontierLateralSamples() * 2 + 1);
        offsets.add(0);
        for (int index = 1; index <= settings.frontierLateralSamples(); index++) {
            int offset = index * settings.frontierLateralStep();
            offsets.add(offset);
            offsets.add(-offset);
        }
        return offsets;
    }

    private void addColumnSurfaces(
            SurfaceNodeResolver resolver,
            int startY,
            int x,
            int z,
            Set<SurfaceNode> nodes) {
        for (int yOffset : verticalOffsets()) {
            BlockPosition feet = new BlockPosition(x, startY + yOffset, z);
            nodes.addAll(resolver.standingSurfaces(feet));
        }
    }

    private List<Integer> verticalOffsets() {
        int radius = settings.frontierVerticalSearchRadius();
        List<Integer> offsets = new ArrayList<>(radius * 2 + 1);
        offsets.add(0);
        for (int offset = 1; offset <= radius; offset++) {
            offsets.add(-offset);
            offsets.add(offset);
        }
        return offsets;
    }

    private record Direction(double x, double z) {
        private static Direction between(BlockPosition start, BlockPosition end) {
            int deltaX = end.x() - start.x();
            int deltaZ = end.z() - start.z();
            double length = Math.hypot(deltaX, deltaZ);
            if (length <= 0.0) {
                return new Direction(1.0, 0.0);
            }
            return new Direction(deltaX / length, deltaZ / length);
        }

        private BlockPosition frontierCenter(
                BlockPosition start,
                LongDistanceRouteSettings settings,
                double distanceRatio) {
            int maxAxisDelta = maxAxisDelta(settings, distanceRatio);
            double scale = axisScale(maxAxisDelta, x, z);
            int xDelta = signedAxisDelta(x, scale);
            int zDelta = signedAxisDelta(z, scale);
            return new BlockPosition(start.x() + xDelta, start.y(), start.z() + zDelta);
        }

        private double perpendicularX() {
            return -z;
        }

        private double perpendicularZ() {
            return x;
        }

        private static int maxAxisDelta(LongDistanceRouteSettings settings, double distanceRatio) {
            return Math.max(1, (int) Math.round(settings.maxSegmentAxisDelta() * distanceRatio));
        }

        private static double axisScale(int maxAxisDelta, double x, double z) {
            double maxComponent = Math.max(Math.abs(x), Math.abs(z));
            if (maxComponent <= 1.0E-9) {
                return maxAxisDelta;
            }
            return maxAxisDelta / maxComponent;
        }

        private static int signedAxisDelta(double component, double scale) {
            if (Math.abs(component) < 1.0E-9) {
                return 0;
            }
            return (int) Math.round(component * scale);
        }
    }
}
