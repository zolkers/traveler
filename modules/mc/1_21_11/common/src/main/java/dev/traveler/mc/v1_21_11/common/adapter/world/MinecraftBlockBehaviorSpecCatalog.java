package dev.traveler.mc.v1_21_11.common.adapter.world;

import java.util.List;

public final class MinecraftBlockBehaviorSpecCatalog {
    private MinecraftBlockBehaviorSpecCatalog() {}

    public static List<MinecraftBlockBehaviorSpecResolver> defaultResolvers() {
        return List.of(
                new CarpetBlockSpecResolver(),
                new LadderBlockSpecResolver(),
                new VineBlockSpecResolver(),
                new FenceBlockSpecResolver(),
                new WallBlockSpecResolver(),
                new SlabBlockSpecResolver(),
                new StairBlockSpecResolver());
    }
}
