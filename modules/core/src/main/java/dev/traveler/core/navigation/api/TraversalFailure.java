package dev.traveler.core.navigation.api;

import java.util.Objects;

public record TraversalFailure(Kind kind, String message) {
    public TraversalFailure {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(message, "message");
    }

    public enum Kind {
        NO_PROGRESS,
        PATH_DIVERGENCE,
        ACTION_SETUP_TIMEOUT,
        TRAVERSAL_ABORTED,
        SEGMENT_STITCH_FAILURE,
        WORLD_STATE_INVALIDATED,
        UNLOADED_FRONTIER
    }
}
