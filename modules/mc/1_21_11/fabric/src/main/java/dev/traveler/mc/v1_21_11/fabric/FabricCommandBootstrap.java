package dev.traveler.mc.v1_21_11.fabric;

import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.adapters.minecraft.fabric.FabricMinecraftIntegration;
import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import java.util.Objects;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class FabricCommandBootstrap {
    private FabricCommandBootstrap() {
    }

    public static void register(TravelerCommandModule module) {
        Objects.requireNonNull(module, "module");
        MinecraftSourceMapper<FabricClientCommandSource> sourceMapper = FabricCommandSourceAdapter::new;
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                FabricMinecraftIntegration.registration(module.framework(), sourceMapper).registerInto(dispatcher));
    }
}
