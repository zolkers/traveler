package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import java.util.List;

public interface TraversalConnectionContributor {
    List<SurfaceConnectionProvider> surfaceConnectionProviders();
}
