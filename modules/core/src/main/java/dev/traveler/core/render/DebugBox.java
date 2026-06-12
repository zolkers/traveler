package dev.traveler.core.render;

import java.util.Objects;

public record DebugBox(RenderVertex min, RenderVertex max, ColorRgba color) {
    public DebugBox {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        Objects.requireNonNull(color, "color");
        if (max.x() <= min.x() || max.y() <= min.y() || max.z() <= min.z()) {
            throw new IllegalArgumentException("Box max bounds must be greater than min bounds.");
        }
    }
}
