package dev.traveler.core.navigation.steering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.spatial.HorizontalVector;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathCorridorTest {
    @Test
    void projectsPositionOntoNearestSegmentAndReportsDistanceAlongPath() {
        PathCorridor corridor = PathCorridor.from(path(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 6.0),
                new NavigationPoint(4.0, 64.0, 6.0)));

        PathProjection projection = corridor.project(new NavigationPoint(1.0, 64.0, 3.0));

        assertEquals(new NavigationPoint(0.0, 64.0, 3.0), projection.nearestPoint());
        assertEquals(3.0, projection.distanceOnPath());
        assertEquals(1.0, projection.lateralError());
        assertEquals(new HorizontalVector(0.0, 1.0), projection.tangent());
    }

    @Test
    void calculatesTargetAtOffsetDistanceAlongPath() {
        PathCorridor corridor = PathCorridor.from(path(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 6.0),
                new NavigationPoint(4.0, 64.0, 6.0)));

        NavigationPoint target = corridor.targetAt(8.0);

        assertEquals(new NavigationPoint(2.0, 64.0, 6.0), target);
    }

    @Test
    void projectsExactCornerOntoForwardSegment() {
        PathCorridor corridor = PathCorridor.from(path(
                new NavigationPoint(0.0, 64.0, 0.0),
                new NavigationPoint(0.0, 64.0, 4.0),
                new NavigationPoint(4.0, 64.0, 4.0)));

        PathProjection projection = corridor.project(new NavigationPoint(0.0, 64.0, 4.0));

        assertEquals(new NavigationPoint(0.0, 64.0, 4.0), projection.nearestPoint());
        assertEquals(new HorizontalVector(1.0, 0.0), projection.tangent());
    }

    private static NavigationPath path(NavigationPoint first, NavigationPoint second, NavigationPoint third) {
        return NavigationPath.of(List.of(first, second, third));
    }
}
