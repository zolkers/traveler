package dev.traveler.core.world.navigation;

import dev.traveler.core.world.behavior.context.MovementDirection;
import java.util.List;

final class SurfaceConnectionDirections {
    static final List<MovementDirection> CARDINAL = List.of(
            MovementDirection.east(),
            MovementDirection.west(),
            MovementDirection.south(),
            MovementDirection.north());
    static final List<MovementDirection> EIGHT_WAY = List.of(
            MovementDirection.east(),
            MovementDirection.west(),
            MovementDirection.south(),
            MovementDirection.north(),
            MovementDirection.fromOffset(1, 1),
            MovementDirection.fromOffset(1, -1),
            MovementDirection.fromOffset(-1, 1),
            MovementDirection.fromOffset(-1, -1));

    private SurfaceConnectionDirections() {}
}
