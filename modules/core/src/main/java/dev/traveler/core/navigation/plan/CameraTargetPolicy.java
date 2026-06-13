package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.camera.CameraAngles;
import dev.traveler.core.navigation.camera.CameraTargetPlanner;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.follow.PathProgress;
import dev.traveler.core.navigation.spatial.NavigationPoint;
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
            NavigationPoint position,
            PathProgress progress,
            CameraAngles currentAngles) {
        return targetPlanner.targetAngles(path, position, progress, currentAngles);
    }
}
