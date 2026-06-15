package dev.traveler.core.layer;

import dev.traveler.core.world.geometry.BlockShape;
import java.util.Objects;

public record SurfaceBlockSample(
        boolean air,
        boolean containsFluid,
        BlockShape collisionShape,
        BlockBehaviorSpec behaviorSpec) {
    public SurfaceBlockSample {
        Objects.requireNonNull(collisionShape, "collisionShape");
        Objects.requireNonNull(behaviorSpec, "behaviorSpec");
    }
}
