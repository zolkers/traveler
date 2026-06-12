package dev.traveler.core.render;

import java.util.Objects;

public record DebugLine(RenderVertex from, RenderVertex to, ColorRgba color) {
    public DebugLine {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(color, "color");
    }
}
