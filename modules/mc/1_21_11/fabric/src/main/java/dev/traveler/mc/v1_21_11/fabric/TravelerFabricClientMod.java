package dev.traveler.mc.v1_21_11.fabric;

import dev.traveler.mc.v1_21_11.fabric.command.FabricCommandBootstrap;
import dev.traveler.mc.v1_21_11.fabric.event.FabricEventBootstrap;
import dev.traveler.mc.v1_21_11.fabric.hud.FabricHudBootstrap;
import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import dev.traveler.core.debug.PathfinderDebugState;
import dev.traveler.core.navigation.TravelerNavigationState;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class TravelerFabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PathfinderDebugState debugState = new PathfinderDebugState();
        TravelerNavigationState navigationState = new TravelerNavigationState();
        TravelerCommandModule module =
                new TravelerCommandModule(debugState, navigationState, () -> Minecraft.getInstance().level);
        FabricCommandBootstrap.register(module);
        FabricEventBootstrap.registerClientEvents(module);
        FabricHudBootstrap.registerDefaultHud();
    }
}
