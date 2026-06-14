package dev.traveler.core.smooth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PathSmoother<N> {
    private final LineOfWalk<N> lineOfWalk;
    private final PathNodePreservation<N> preservation;
    private final PathSmoothingSelector<N> selector;

    public PathSmoother(LineOfWalk<N> lineOfWalk) {
        this(lineOfWalk, PathNodePreservation.none());
    }

    public PathSmoother(LineOfWalk<N> lineOfWalk, PathNodePreservation<N> preservation) {
        this(lineOfWalk, preservation, PathSmoothingSelector.farthestReachable());
    }

    public PathSmoother(
            LineOfWalk<N> lineOfWalk,
            PathNodePreservation<N> preservation,
            PathSmoothingSelector<N> selector) {
        this.lineOfWalk = Objects.requireNonNull(lineOfWalk, "lineOfWalk");
        this.preservation = Objects.requireNonNull(preservation, "preservation");
        this.selector = Objects.requireNonNull(selector, "selector");
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
            int next = selectedNextIndex(path, anchor, limit);
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

    private int selectedNextIndex(List<N> path, int anchor, int limit) {
        int next = selector.selectNext(path, anchor, limit, lineOfWalk);
        requireWithinSmoothingWindow(anchor, limit, next);
        requireReachableSelection(path, anchor, next);
        return next;
    }

    private static void requireWithinSmoothingWindow(int anchor, int limit, int next) {
        if (next <= anchor || next > limit) {
            throw new IllegalStateException("Smoothing selector returned an index outside the active window.");
        }
    }

    private void requireReachableSelection(List<N> path, int anchor, int next) {
        if (next == anchor + 1) {
            return;
        }
        if (lineOfWalk.hasLineOfWalk(path.get(anchor), path.get(next))) {
            return;
        }
        throw new IllegalStateException("Smoothing selector returned an unreachable index.");
    }
}
