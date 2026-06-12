package dev.traveler.core.smooth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PathSmoother<N> {
    private final LineOfWalk<N> lineOfWalk;
    private final PathNodePreservation<N> preservation;

    public PathSmoother(LineOfWalk<N> lineOfWalk) {
        this(lineOfWalk, PathNodePreservation.none());
    }

    public PathSmoother(LineOfWalk<N> lineOfWalk, PathNodePreservation<N> preservation) {
        this.lineOfWalk = Objects.requireNonNull(lineOfWalk, "lineOfWalk");
        this.preservation = Objects.requireNonNull(preservation, "preservation");
    }

    public List<N> smooth(List<N> path) {
        Objects.requireNonNull(path, "path");
        if (path.size() <= 1) {
            return List.copyOf(path);
        }
        return smoothMultiNodePath(path);
    }

    private List<N> smoothMultiNodePath(List<N> path) {
        List<N> smoothed = new ArrayList<>();
        int anchor = 0;
        smoothed.add(path.get(anchor));
        while (anchor < path.size() - 1) {
            int limit = nextRequiredNodeIndex(path, anchor);
            int next = farthestReachableIndex(path, anchor, limit);
            smoothed.add(path.get(next));
            anchor = next;
        }
        return List.copyOf(smoothed);
    }

    private int nextRequiredNodeIndex(List<N> path, int anchor) {
        for (int index = anchor + 1; index < path.size() - 1; index++) {
            if (preservation.mustPreserve(path.get(index - 1), path.get(index), path.get(index + 1))) {
                return index;
            }
        }
        return path.size() - 1;
    }

    private int farthestReachableIndex(List<N> path, int anchor, int limit) {
        int next = limit;
        while (next > anchor + 1 && !lineOfWalk.hasLineOfWalk(path.get(anchor), path.get(next))) {
            next--;
        }
        return next;
    }
}
