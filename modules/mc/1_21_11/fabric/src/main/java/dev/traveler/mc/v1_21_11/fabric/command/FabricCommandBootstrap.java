package dev.traveler.mc.v1_21_11.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.minecraft.common.MinecraftSourceMapper;
import dev.riege.buildmycommand.adapters.minecraft.fabric.FabricMinecraftIntegration;
import dev.traveler.command.buildmycommand.TravelerCommandModule;
import java.util.Objects;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class FabricCommandBootstrap {
    private FabricCommandBootstrap() {
    }

    public static void register(TravelerCommandModule module) {
        Objects.requireNonNull(module, "module");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                registerInto(dispatcher, module));
    }

    static void registerInto(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            TravelerCommandModule module) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(module, "module");
        MinecraftSourceMapper<FabricClientCommandSource> sourceMapper = FabricCommandSourceAdapter::new;
        FabricMinecraftIntegration.registration(module.framework(), sourceMapper).registerInto(dispatcher);
    }
}
