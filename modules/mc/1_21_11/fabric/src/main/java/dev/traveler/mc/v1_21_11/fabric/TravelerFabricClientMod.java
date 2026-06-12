package dev.traveler.mc.v1_21_11.fabric;

import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import dev.traveler.core.debug.PathfinderDebugState;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class TravelerFabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TravelerCommandModule module =
                new TravelerCommandModule(new PathfinderDebugState(), () -> Minecraft.getInstance().level);
        FabricCommandBootstrap.register(module);
        FabricEventBootstrap.registerClientEvents(module);
    }
}
