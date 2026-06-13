package dev.traveler.core.render;

import java.util.Objects;

public record DebugLine(RenderVertex from, RenderVertex to, ColorRgba color, double thickness) {
    public DebugLine(RenderVertex from, RenderVertex to, ColorRgba color) {
        this(from, to, color, 0.08);
    }

    public DebugLine {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(color, "color");
        if (!Double.isFinite(thickness) || thickness <= 0.0) {
            throw new IllegalArgumentException("thickness must be positive.");
        }
    }
}
