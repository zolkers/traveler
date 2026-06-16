package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.navigation.debug.DebugLayer;
import java.util.List;

public interface TraversalDebugContributor {
    List<DebugLayer> debugLayers();
}
