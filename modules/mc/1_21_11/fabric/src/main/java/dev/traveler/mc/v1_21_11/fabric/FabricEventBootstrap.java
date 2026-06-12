package dev.traveler.mc.v1_21_11.fabric;

import dev.traveler.mc.v1_21_11.common.command.TravelerCommandModule;
import java.util.Objects;

public final class FabricEventBootstrap {
    private FabricEventBootstrap() {
    }

    public static void registerClientEvents(TravelerCommandModule module) {
        Objects.requireNonNull(module, "module");
    }
}
