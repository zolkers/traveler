package dev.traveler.core.smooth;

import java.util.List;

@FunctionalInterface
public interface PathSmoothingSelector<N> {
    int selectNext(List<N> path, int anchor, int limit, LineOfWalk<N> lineOfWalk);

    static <N> PathSmoothingSelector<N> farthestReachable() {
        return new FarthestReachablePathSmoothingSelector<>();
    }
}
