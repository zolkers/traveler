package dev.traveler.core.world.surface;

import dev.traveler.core.world.block.BlockPosition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.bottomSlab;
import static dev.traveler.core.world.surface.FakeSurfaceWorldLayer.fullBlock;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceNodeResolverTest {
    @Test
    void standingSurfaceUsesSolidBlockBelowWholeBlockFeet() {
        SurfaceNodeResolver resolver =
                new SurfaceNodeResolver(new FakeSurfaceWorldLayer(Map.of(new BlockPosition(0, 63, 0), fullBlock())));

        SurfaceNode node = resolver.standingSurface(new BlockPosition(0, 64, 0)).orElseThrow();

        assertEquals(new BlockPosition(0, 63, 0), node.blockPosition());
        assertEquals(64.0, node.floorY());
    }

    @Test
    void standingSurfaceCanUseBottomSlabInSameFeetBlock() {
        SurfaceNodeResolver resolver =
                new SurfaceNodeResolver(new FakeSurfaceWorldLayer(Map.of(new BlockPosition(0, 63, 0), bottomSlab())));

        SurfaceNode node = resolver.standingSurface(new BlockPosition(0, 63, 0)).orElseThrow();

        assertEquals(new BlockPosition(0, 63, 0), node.blockPosition());
        assertEquals(63.5, node.floorY());
    }

    @Test
    void standingSurfacesExposeEverySupportedSubcellAtFeetHeight() {
        BlockPosition support = new BlockPosition(0, 63, 0);
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(new FakeSurfaceWorldLayer(Map.of(support, fullBlock())));

        assertEquals(4, resolver.standingSurfaces(new BlockPosition(0, 64, 0)).size());
    }

    @Test
    void centeredSurfaceIsEmptyWhenBlockHasNoSupport() {
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(new FakeSurfaceWorldLayer(Map.of()));

        assertTrue(resolver.centeredSurface(new BlockPosition(0, 63, 0)).isEmpty());
    }

    @Test
    void surfacesExposeEverySupportedSubcell() {
        BlockPosition support = new BlockPosition(0, 63, 0);
        SurfaceNodeResolver resolver = new SurfaceNodeResolver(new FakeSurfaceWorldLayer(Map.of(support, fullBlock())));

        assertEquals(4, resolver.surfaces(support).size());
    }
}
