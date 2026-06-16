package dev.traveler.core.navigation.diagnostics;

import dev.traveler.core.world.block.BlockPosition;
import java.util.Objects;

public record BlockScanSample(
        BlockPosition position,
        String block,
        String passability,
        String fluid,
        String behavior) {
    public BlockScanSample {
        Objects.requireNonNull(position, "position");
        block = requireText(block, "block");
        passability = requireText(passability, "passability");
        fluid = requireText(fluid, "fluid");
        behavior = requireText(behavior, "behavior");
    }

    public static BlockScanSample unavailable(BlockPosition position, RuntimeException failure) {
        String reason = failure.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = failure.getClass().getSimpleName();
        }
        return new BlockScanSample(position, "unavailable:" + reason, "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name);
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return text;
    }
}
