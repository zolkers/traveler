package dev.traveler.mc.v1_21_11.common.render;

import java.util.List;

public record DebugRenderFrame(List<DebugLine> lines) {
    public DebugRenderFrame {
        lines = List.copyOf(lines);
    }

    public static DebugRenderFrame empty() {
        return new DebugRenderFrame(List.of());
    }
}
