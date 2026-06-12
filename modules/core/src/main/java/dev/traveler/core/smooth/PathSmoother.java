package dev.traveler.core.smooth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PathSmoother<N> {
    private final LineOfWalk<N> lineOfWalk;

    public PathSmoother(LineOfWalk<N> lineOfWalk) {
        this.lineOfWalk = Objects.requireNonNull(lineOfWalk, "lineOfWalk");
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
            int next = farthestReachableIndex(path, anchor);
            smoothed.add(path.get(next));
            anchor = next;
        }
        return List.copyOf(smoothed);
    }

    private int farthestReachableIndex(List<N> path, int anchor) {
        int next = path.size() - 1;
        while (next > anchor + 1 && !lineOfWalk.hasLineOfWalk(path.get(anchor), path.get(next))) {
            next--;
        }
        return next;
    }
}
