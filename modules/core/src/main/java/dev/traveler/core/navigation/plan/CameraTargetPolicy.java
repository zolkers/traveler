package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.camera.CameraTargetPlanner;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.common.geometry.WorldPoint;
import java.util.Objects;

public final class CameraTargetPolicy {
    private final CameraTargetPlanner targetPlanner;

    public CameraTargetPolicy(CameraTargetPlanner targetPlanner) {
        this.targetPlanner = Objects.requireNonNull(targetPlanner, "targetPlanner");
    }

    public static CameraTargetPolicy standard() {
        return new CameraTargetPolicy(CameraTargetPlanner.standard());
    }

    public CameraAngles target(
            NavigationPath path,
            WorldPoint position,
            PathProgress progress,
            CameraAngles currentAngles) {
        return targetPlanner.targetAngles(path, position, progress, currentAngles);
    }
}
