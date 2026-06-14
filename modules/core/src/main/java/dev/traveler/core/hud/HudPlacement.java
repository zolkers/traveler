package dev.traveler.core.hud;

import java.util.Objects;

public record HudPlacement(HudAnchor anchor, int marginX, int marginY) {
    public HudPlacement {
        Objects.requireNonNull(anchor, "anchor");
        if (marginX < 0 || marginY < 0) {
            throw new IllegalArgumentException("Margins must be non-negative.");
        }
    }

    public static HudPlacement topLeft(int marginX, int marginY) {
        return new HudPlacement(HudAnchor.TOP_LEFT, marginX, marginY);
    }

    public static HudPlacement topRight(int marginX, int marginY) {
        return new HudPlacement(HudAnchor.TOP_RIGHT, marginX, marginY);
    }

    public static HudPlacement bottomLeft(int marginX, int marginY) {
        return new HudPlacement(HudAnchor.BOTTOM_LEFT, marginX, marginY);
    }

    public static HudPlacement bottomRight(int marginX, int marginY) {
        return new HudPlacement(HudAnchor.BOTTOM_RIGHT, marginX, marginY);
    }

    public HudPoint position(HudViewport viewport, HudSize size) {
        HudViewport safeViewport = Objects.requireNonNull(viewport, "viewport");
        HudSize safeSize = Objects.requireNonNull(size, "size");
        return switch (anchor) {
            case TOP_LEFT -> new HudPoint(marginX, marginY);
            case TOP_RIGHT -> new HudPoint(safeViewport.width() - safeSize.width() - marginX, marginY);
            case BOTTOM_LEFT -> new HudPoint(marginX, safeViewport.height() - safeSize.height() - marginY);
            case BOTTOM_RIGHT -> new HudPoint(
                    safeViewport.width() - safeSize.width() - marginX,
                    safeViewport.height() - safeSize.height() - marginY);
        };
    }
}
