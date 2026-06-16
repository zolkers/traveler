package dev.traveler.core.capability.behavior.spi;

import dev.traveler.core.layer.SurfaceBlockSample;
import dev.traveler.core.world.behavior.BlockBehavior;
import java.util.Optional;

public interface BlockBehaviorResolver {
    Optional<BlockBehavior> resolve(SurfaceBlockSample sample);
}
