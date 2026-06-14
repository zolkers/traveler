package dev.traveler.core.smooth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PathSmootherTest {
    @Test
    void removesMiddleNodesWhenLineOfWalkPermitsDirectMovement() {
        PathSmoother<GridPoint> smoother = new PathSmoother<>((from, to) -> true);
        List<GridPoint> path = List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0));

        assertEquals(List.of(new GridPoint(0, 0), new GridPoint(2, 0)), smoother.smooth(path));
    }

    @Test
    void preservesNodesWhenLineOfWalkBlocksDirectMovement() {
        FakeGrid grid = new FakeGrid();
        PathSmoother<GridPoint> smoother = new PathSmoother<>(grid::hasLineOfWalk);
        List<GridPoint> path = List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0));

        assertEquals(path, smoother.smooth(path));
    }

    @Test
    void preservesRequiredMiddleNodesEvenWhenDirectMovementIsClear() {
        PathSmoother<GridPoint> smoother =
                new PathSmoother<>((from, to) -> true, (previous, current, next) -> current.x() == 1);
        List<GridPoint> path = List.of(
                new GridPoint(0, 0),
                new GridPoint(1, 0),
                new GridPoint(2, 0),
                new GridPoint(3, 0));

        assertEquals(List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(3, 0)), smoother.smooth(path));
    }

    @Test
    void delegatesReachableNodeSelectionToStrategy() {
        PathSmoother<GridPoint> smoother = new PathSmoother<>(
                (from, to) -> true,
                PathNodePreservation.none(),
                (path, anchor, limit, lineOfWalk) -> anchor + 1);
        List<GridPoint> path = List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0));

        assertEquals(path, smoother.smooth(path));
    }

    @Test
    void rejectsSelectionOutsideCurrentSmoothingWindow() {
        PathSmoother<GridPoint> smoother = new PathSmoother<>(
                (from, to) -> true,
                PathNodePreservation.none(),
                (path, anchor, limit, lineOfWalk) -> limit + 1);
        List<GridPoint> path = List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0));

        assertThrows(IllegalStateException.class, () -> smoother.smooth(path));
    }

    @Test
    void rejectsSelectionThatSkipsUnreachableNodes() {
        PathSmoother<GridPoint> smoother = new PathSmoother<>(
                (from, to) -> false,
                PathNodePreservation.none(),
                (path, anchor, limit, lineOfWalk) -> limit);
        List<GridPoint> path = List.of(new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0));

        assertThrows(IllegalStateException.class, () -> smoother.smooth(path));
    }

    @Test
    void handlesEmptyAndSingleNodePaths() {
        PathSmoother<GridPoint> smoother = new PathSmoother<>((from, to) -> true);
        GridPoint spawn = new GridPoint(4, 7);

        assertEquals(List.of(), smoother.smooth(List.of()));
        assertEquals(List.of(spawn), smoother.smooth(List.of(spawn)));
    }

    private record GridPoint(int x, int z) {
    }

    private static final class FakeGrid {
        boolean hasLineOfWalk(GridPoint from, GridPoint to) {
            int distance = Math.abs(from.x() - to.x()) + Math.abs(from.z() - to.z());
            return distance <= 1;
        }
    }
}
