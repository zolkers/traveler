package dev.traveler.mc.v1_21_11.common.adapter.world;

import java.util.List;

public final class MinecraftBlockBehaviorCatalog {
    private MinecraftBlockBehaviorCatalog() {}

    public static List<MinecraftBlockBehaviorResolver> defaultResolvers() {
        return List.of(
                new CarpetBlockBehaviorResolver(),
                new LadderBlockBehaviorResolver(),
                new VineBlockBehaviorResolver(),
                new FenceBlockBehaviorResolver(),
                new WallBlockBehaviorResolver(),
                new AirBlockBehaviorResolver(),
                new SlabBlockBehaviorResolver(),
                new StairBlockBehaviorResolver(),
                new FullBlockBehaviorResolver());
    }
}
