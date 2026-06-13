package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.surface.SurfaceNode;
import org.junit.jupiter.api.Test;

class SurfaceNodeIndexTest {
    @Test
    void givesStableDistinctIndexesForSurfaceSubcellsInsideBounds() {
        SearchBounds bounds = new SearchBounds(10, 11, 60, 61, -2, -1);
        SurfaceNodeIndex index = new SurfaceNodeIndex(bounds);
        SurfaceNode first = new SurfaceNode(new BlockPosition(10, 60, -2), 0, 0, 61.0);
        SurfaceNode second = new SurfaceNode(new BlockPosition(10, 60, -2), 1, 0, 61.0);

        assertEquals(index.indexOf(first), index.indexOf(first));
        assertNotEquals(index.indexOf(first), index.indexOf(second));
    }

    @Test
    void rejectsNodesOutsideBounds() {
        SearchBounds bounds = new SearchBounds(10, 10, 60, 60, -2, -2);
        SurfaceNodeIndex index = new SurfaceNodeIndex(bounds);
        SurfaceNode outside = new SurfaceNode(new BlockPosition(11, 60, -2), 0, 0, 61.0);

        assertThrows(IllegalArgumentException.class, () -> index.indexOf(outside));
    }
}
