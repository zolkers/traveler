package dev.traveler.core.navigation.follow;

public record PathProgress(int nextNodeIndex) {
    public PathProgress {
        if (nextNodeIndex < 1) {
            throw new IllegalArgumentException("nextNodeIndex must point past the starting node.");
        }
    }

    public static PathProgress start() {
        return new PathProgress(1);
    }
}
