package dev.traveler.core.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudPlacementTest {
    @Test
    void resolvesTopLeftAnchorFromMargins() {
        HudPoint point = HudPlacement.topLeft(6, 8).position(new HudViewport(100, 80), new HudSize(20, 10));

        assertEquals(new HudPoint(6, 8), point);
    }

    @Test
    void resolvesOtherAnchorsAgainstViewportEdges() {
        HudViewport viewport = new HudViewport(100, 80);
        HudSize size = new HudSize(20, 10);

        assertEquals(new HudPoint(74, 8), HudPlacement.topRight(6, 8).position(viewport, size));
        assertEquals(new HudPoint(6, 62), HudPlacement.bottomLeft(6, 8).position(viewport, size));
        assertEquals(new HudPoint(74, 62), HudPlacement.bottomRight(6, 8).position(viewport, size));
    }
}
