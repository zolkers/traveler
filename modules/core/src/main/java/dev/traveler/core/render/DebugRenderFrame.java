package dev.traveler.core.render;

import java.util.List;

public record DebugRenderFrame(List<DebugLine> lines) {
    public DebugRenderFrame {
        lines = List.copyOf(lines);
    }

    public static DebugRenderFrame empty() {
        return new DebugRenderFrame(List.of());
    }
}
