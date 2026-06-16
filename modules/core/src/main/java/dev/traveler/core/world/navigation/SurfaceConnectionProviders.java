package dev.traveler.core.world.navigation;

import java.util.List;

public final class SurfaceConnectionProviders {
    private SurfaceConnectionProviders() {}

    public static SurfaceConnectionProvider adjacent() {
        return new AdjacentSurfaceConnectionProvider();
    }

    public static SurfaceConnectionProvider drop() {
        return new DropSurfaceConnectionProvider();
    }

    public static SurfaceConnectionProvider jump() {
        return new JumpSurfaceConnectionProvider();
    }

    public static SurfaceConnectionProvider climb() {
        return new ClimbSurfaceConnectionProvider();
    }

    public static SurfaceConnectionProvider swim() {
        return new SwimSurfaceConnectionProvider();
    }

    public static List<SurfaceConnectionProvider> standard() {
        return List.of(adjacent(), swim(), drop(), jump(), climb());
    }
}
