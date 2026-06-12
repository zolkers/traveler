package dev.traveler.core.render;

import java.util.List;

public record DebugRenderFrame(List<DebugLine> lines, List<DebugBox> boxes) {
    public DebugRenderFrame {
        lines = List.copyOf(lines);
        boxes = List.copyOf(boxes);
    }

    public DebugRenderFrame(List<DebugLine> lines) {
        this(lines, List.of());
    }

    public static DebugRenderFrame empty() {
        return new DebugRenderFrame(List.of(), List.of());
    }
}
