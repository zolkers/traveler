package dev.traveler.core.route.start;

import dev.traveler.core.world.surface.SurfaceNode;
import java.util.List;
import java.util.Objects;

public final class SubmergedSwimRouteStartProvider implements SurfaceRouteStartProvider {
    @Override
    public List<SurfaceNode> startNodes(SurfaceRouteStartContext context) {
        SurfaceRouteStartContext safeContext = Objects.requireNonNull(context, "context");
        return safeContext.surfaceNodes().swimStartSurfaces(safeContext.feetPosition());
    }
}
