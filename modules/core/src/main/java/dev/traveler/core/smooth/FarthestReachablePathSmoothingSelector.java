package dev.traveler.core.smooth;

import java.util.List;

final class FarthestReachablePathSmoothingSelector<N> implements PathSmoothingSelector<N> {
    @Override
    public int selectNext(List<N> path, int anchor, int limit, LineOfWalk<N> lineOfWalk) {
        int next = limit;
        while (next > anchor + 1 && !lineOfWalk.hasLineOfWalk(path.get(anchor), path.get(next))) {
            next--;
        }
        return next;
    }
}
