package dev.traveler.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LinePrismTest {
    @Test
    void createsCenteredSquarePrismAroundHorizontalSegment() {
        LinePrism prism = LinePrism.around(
                new RenderVertex(1.0, 64.5, 1.0),
                new RenderVertex(3.0, 64.5, 1.0),
                0.08);

        assertEquals(new RenderVertex(1.0, 64.54, 0.96), prism.startA());
        assertEquals(new RenderVertex(1.0, 64.54, 1.04), prism.startB());
        assertEquals(new RenderVertex(1.0, 64.46, 1.04), prism.startC());
        assertEquals(new RenderVertex(1.0, 64.46, 0.96), prism.startD());
        assertEquals(new RenderVertex(3.0, 64.54, 0.96), prism.endA());
    }

    @Test
    void rejectsZeroLengthSegments() {
        RenderVertex vertex = new RenderVertex(1.0, 64.5, 1.0);

        assertThrows(IllegalArgumentException.class, () -> LinePrism.around(vertex, vertex, 0.08));
    }
}
