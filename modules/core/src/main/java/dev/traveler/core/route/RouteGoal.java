package dev.traveler.core.route;

import dev.traveler.core.layer.WorldLayer;
import dev.traveler.core.route.goal.ColumnRouteGoal;
import dev.traveler.core.route.goal.ExactBlockRouteGoal;
import dev.traveler.core.route.goal.HeightRouteGoal;
import dev.traveler.core.route.goal.UnspecifiedRouteGoal;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import dev.traveler.core.world.surface.SurfaceNodeResolver;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public interface RouteGoal {
    static RouteGoal blockTarget(BlockPosition target) {
        return new ExactBlockRouteGoal(Objects.requireNonNull(target, "target"));
    }

    static RouteGoal xyz(int x, int y, int z) {
        return blockTarget(new BlockPosition(x, y, z));
    }

    static RouteGoal xz(int x, int z) {
        return new ColumnRouteGoal(x, z);
    }

    static RouteGoal yLevel(int y) {
        return new HeightRouteGoal(y);
    }

    static RouteGoal unspecified() {
        return UnspecifiedRouteGoal.INSTANCE;
    }

    default Optional<BlockPosition> requestedTarget() {
        return Optional.empty();
    }

    default OptionalInt surfaceFallbackGoalLimit() {
        return OptionalInt.empty();
    }

    default List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver) {
        return surfaceGoals(resolver, new BlockPosition(0, 0, 0));
    }

    List<SurfaceNode> surfaceGoals(SurfaceNodeResolver resolver, BlockPosition start);

    default BlockPosition blockGoal(WorldLayer worldLayer) {
        return blockGoal(worldLayer, preferredPosition(new BlockPosition(0, 0, 0)));
    }

    BlockPosition blockGoal(WorldLayer worldLayer, BlockPosition start);

    BlockPosition preferredPosition(BlockPosition fallback);

    boolean isSatisfiedBy(BlockPosition position);

    String displayName();
}
