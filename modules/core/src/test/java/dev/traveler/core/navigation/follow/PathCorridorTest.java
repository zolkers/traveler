package dev.traveler.core.navigation.follow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathCorridorTest {
    @Test
    void projectsPositionOntoNearestSegmentAndReportsDistanceAlongPath() {
        PathCorridor corridor = PathCorridor.from(path(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 6.0),
                new WorldPoint(4.0, 64.0, 6.0)));

        PathProjection projection = corridor.project(new WorldPoint(1.0, 64.0, 3.0));

        assertEquals(new WorldPoint(0.0, 64.0, 3.0), projection.nearestPoint());
        assertEquals(3.0, projection.distanceOnPath());
        assertEquals(1.0, projection.lateralError());
        assertEquals(new HorizontalVector(0.0, 1.0), projection.tangent());
    }

    @Test
    void reportsSignedLateralErrorRelativeToPathTangent() {
        PathCorridor corridor = PathCorridor.from(path(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 6.0),
                new WorldPoint(4.0, 64.0, 6.0)));

        PathProjection eastOfPath = corridor.project(new WorldPoint(1.0, 64.0, 3.0));
        PathProjection westOfPath = corridor.project(new WorldPoint(-1.0, 64.0, 3.0));

        assertEquals(-1.0, eastOfPath.signedLateralError());
        assertEquals(1.0, westOfPath.signedLateralError());
    }

    @Test
    void calculatesTargetAtOffsetDistanceAlongPath() {
        PathCorridor corridor = PathCorridor.from(path(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 6.0),
                new WorldPoint(4.0, 64.0, 6.0)));

        WorldPoint target = corridor.targetAt(8.0);

        assertEquals(new WorldPoint(2.0, 64.0, 6.0), target);
    }

    @Test
    void projectsExactCornerOntoForwardSegment() {
        PathCorridor corridor = PathCorridor.from(path(
                new WorldPoint(0.0, 64.0, 0.0),
                new WorldPoint(0.0, 64.0, 4.0),
                new WorldPoint(4.0, 64.0, 4.0)));

        PathProjection projection = corridor.project(new WorldPoint(0.0, 64.0, 4.0));

        assertEquals(new WorldPoint(0.0, 64.0, 4.0), projection.nearestPoint());
        assertEquals(new HorizontalVector(1.0, 0.0), projection.tangent());
    }

    private static NavigationPath path(WorldPoint first, WorldPoint second, WorldPoint third) {
        return NavigationPath.of(List.of(first, second, third));
    }
}
