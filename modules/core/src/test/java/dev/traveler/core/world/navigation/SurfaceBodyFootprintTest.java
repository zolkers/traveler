package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class SurfaceBodyFootprintTest {
    @Test
    void clampsBodyCenterInsideBlockForEdgeSubcells() {
        SurfaceNode eastCell = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 0, 64.0);

        SurfaceBodyFootprint footprint =
                SurfaceBodyFootprint.adjustedAround(eastCell, new EntityDimensions(0.6, 1.8));

        assertEquals(0, footprint.minGlobalX());
        assertEquals(1, footprint.maxGlobalX());
    }
}
